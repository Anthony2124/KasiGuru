package com.kasiguru.util.update

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.kasiguru.data.remote.model.AppReleaseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Where an update download stands, for the release the learner is looking at. */
sealed interface UpdateDownload {
    data object Idle : UpdateDownload
    /** [percent] is null until the server has said how big the file is. */
    data class Downloading(val percent: Int?) : UpdateDownload
    data object Ready : UpdateDownload
    data object Failed : UpdateDownload
}

/**
 * Downloads an update inside the app and hands it to the system installer.
 *
 * The update used to open its link in the browser. Chrome kept each of those links as an ordinary
 * tab and reloaded the old ones the next time it came forward, so every update also offered the
 * previous versions' APKs again. DownloadManager needs no tab, and the file goes to the app's own
 * storage, where older copies are deleted rather than piling up in Downloads.
 */
@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val ids = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    private val _versionCode = MutableStateFlow(0)
    private val _state = MutableStateFlow<UpdateDownload>(UpdateDownload.Idle)
    /** The state of the download for [versionCode]; Idle for any other version. */
    val state: StateFlow<UpdateDownload> = _state.asStateFlow()

    private val _signedByAnotherKey = MutableStateFlow(false)
    /**
     * Whether the finished download is signed with a different key from the app installed now.
     *
     * Android refuses such an update with "App not installed as package conflicts with an existing
     * package", which says nothing about why. Every official release is signed with the same key, so
     * it only happens on a phone running a build from Android Studio or another machine; the banner
     * then says what to do instead of opening an installer that is bound to fail. Checked once, off
     * the main thread, before the download is reported Ready: verifying a signature reads the file.
     */
    val signedByAnotherKey: StateFlow<Boolean> = _signedByAnotherKey.asStateFlow()

    private val folder: File? get() = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    private fun fileFor(release: AppReleaseDto) = folder?.let { File(it, "kasiguru-v${release.versionName}.apk") }

    /** Picks up a download for [release] started earlier, so reopening Home shows its progress. */
    fun watch(release: AppReleaseDto) {
        if (_versionCode.value == release.versionCode) return
        _versionCode.value = release.versionCode
        _state.value = UpdateDownload.Idle
        _signedByAnotherKey.value = false
        val id = ids.getLong(key(release), -1L)
        if (id >= 0) poll(release, id)
    }

    /** Starts downloading [release]. Returns false when the system downloader cannot be used. */
    fun start(release: AppReleaseDto): Boolean {
        val dm = downloads ?: return false
        val target = fileFor(release) ?: return false
        _versionCode.value = release.versionCode
        _signedByAnotherKey.value = false
        return try {
            // Older update files are this app's own, so they can go; DownloadManager would also
            // rename a new file "-1" rather than replace one of the same name.
            folder?.listFiles { f -> f.name.startsWith("kasiguru-") && f.name.endsWith(".apk") }?.forEach { it.delete() }
            ids.getLong(key(release), -1L).takeIf { it >= 0 }?.let { dm.remove(it) }
            val request = DownloadManager.Request(Uri.parse(release.apkUrl))
                .setTitle("KasiGuru ${release.versionName}")
                .setDescription("Downloading update")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(target))
            val id = dm.enqueue(request)
            ids.edit().putLong(key(release), id).apply()
            _state.value = UpdateDownload.Downloading(null)
            poll(release, id)
            true
        } catch (e: Exception) {
            Log.w("UpdateDownloader", "Could not start the update download", e)
            false
        }
    }

    private fun poll(release: AppReleaseDto, id: Long) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                val next = query(id, release)
                if (_versionCode.value != release.versionCode) return@launch
                if (next == UpdateDownload.Ready) _signedByAnotherKey.value = isSignedByAnotherKey(release)
                _state.value = next
                if (next !is UpdateDownload.Downloading) return@launch
                delay(500)
            }
        }
    }

    private fun query(id: Long, release: AppReleaseDto): UpdateDownload {
        val dm = downloads ?: return UpdateDownload.Failed
        dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) return UpdateDownload.Idle
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL ->
                    if (fileFor(release)?.exists() == true) UpdateDownload.Ready else UpdateDownload.Idle
                DownloadManager.STATUS_FAILED -> UpdateDownload.Failed
                else -> UpdateDownload.Downloading(if (total > 0) (done * 100 / total).toInt() else null)
            }
        }
        return UpdateDownload.Idle
    }

    /** Android asks once per installing app; until then the installer cannot open. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** The system installer for the downloaded file, or null if it is gone. */
    fun installIntent(release: AppReleaseDto): Intent? {
        val file = fileFor(release)?.takeIf { it.exists() } ?: return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun key(release: AppReleaseDto) = "id_${release.versionCode}"

    private fun isSignedByAnotherKey(release: AppReleaseDto): Boolean {
        val file = fileFor(release)?.takeIf { it.exists() } ?: return false
        val pm = context.packageManager
        val installed = runCatching { signerDigests(installedInfo(pm), withHistory = false) }.getOrNull()
        val update = runCatching { signerDigests(archiveInfo(pm, file), withHistory = true) }.getOrNull()
        return signersDiffer(installed, update)
    }

    // GET_SIGNATURES is the only option below API 28, and lint's warning about it concerns apps that
    // trust whatever it returns; here it is only compared against the installed app's own signers.
    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, file: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNATURES)
        }

    /**
     * SHA-256 of each signing certificate, or null if none could be read. [withHistory] adds the
     * certificates an update's key was rotated from, which Android also accepts.
     */
    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo?, withHistory: Boolean): Set<String>? {
        info ?: return null
        val signers: List<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return null
            val history = if (withHistory && !signing.hasMultipleSigners()) {
                signing.signingCertificateHistory.orEmpty().toList()
            } else {
                emptyList()
            }
            signing.apkContentsSigners.orEmpty().toList() + history
        } else {
            info.signatures.orEmpty().toList()
        }
        val sha256 = MessageDigest.getInstance("SHA-256")
        return signers.map { s -> sha256.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }
            .toSet()
            .takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}

/**
 * Whether an update cannot install over the app because no certificate signs both. Only when both
 * sides are known: a signature that could not be read never blocks an update, it is left to Android.
 */
internal fun signersDiffer(installed: Set<String>?, update: Set<String>?): Boolean =
    !installed.isNullOrEmpty() && !update.isNullOrEmpty() && installed.intersect(update).isEmpty()
