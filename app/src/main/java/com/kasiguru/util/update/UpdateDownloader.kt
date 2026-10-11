package com.kasiguru.util.update

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
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
import kotlinx.coroutines.withContext
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
 * Why an update cannot install, in words the pop-up can act on. Android's installer says only "App
 * not installed" whatever went wrong, which left learners uninstalling the app to get past it.
 */
sealed interface InstallProblem {
    /** Signed with a different key from the installed app. Android never replaces one with the other. */
    data object SignedDifferently : InstallProblem
    /** The phone is too full. [shortBytes] is roughly how much more space is needed, when known. */
    data class NoSpace(val shortBytes: Long?) : InstallProblem
    /** The downloaded file is incomplete or corrupt. It has been deleted, to be downloaded again. */
    data object Damaged : InstallProblem
    /** Android refused it for another reason. [code] is the installer's own number, for a bug report. */
    data class Refused(val code: Int) : InstallProblem
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

    private val _problem = MutableStateFlow<InstallProblem?>(null)
    /**
     * What stands between the downloaded update and an installed one, or null when nothing does.
     *
     * [InstallProblem.SignedDifferently] is found before the installer opens: a build from Android
     * Studio or another machine is signed with another key, and Android refuses an update over it.
     * Every official release is signed with the same key, so that only happens on a test build. The
     * rest come back from the installer itself, which reports why it refused when asked to.
     */
    val problem: StateFlow<InstallProblem?> = _problem.asStateFlow()

    private val folder: File? get() = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    private fun fileFor(release: AppReleaseDto) = folder?.let { File(it, "kasiguru-v${release.versionName}.apk") }

    /** Picks up a download for [release] started earlier, so reopening Home shows its progress. */
    fun watch(release: AppReleaseDto) {
        if (_versionCode.value == release.versionCode) return
        _versionCode.value = release.versionCode
        _state.value = UpdateDownload.Idle
        _problem.value = null
        val id = ids.getLong(key(release), -1L)
        if (id >= 0) poll(release, id)
    }

    /** Starts downloading [release]. Returns false when the system downloader cannot be used. */
    fun start(release: AppReleaseDto): Boolean {
        val dm = downloads ?: return false
        val target = fileFor(release) ?: return false
        _versionCode.value = release.versionCode
        _problem.value = null
        return try {
            // Older update files are this app's own, so they can go; DownloadManager would also
            // rename a new file "-1" rather than replace one of the same name.
            deleteFiles()
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

    /**
     * Deletes downloaded updates once the app is up to date. The file of the update just installed
     * would otherwise stay until the next one, and from 1.26 it is over 40 MB of a phone's storage.
     */
    fun deleteDownloads() {
        scope.launch {
            deleteFiles()
            ids.edit().clear().apply()
        }
    }

    private fun deleteFiles() {
        folder?.listFiles { f -> f.name.startsWith("kasiguru-") && f.name.endsWith(".apk") }?.forEach { it.delete() }
    }

    private fun poll(release: AppReleaseDto, id: Long) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                val next = query(id, release)
                if (_versionCode.value != release.versionCode) return@launch
                if (next == UpdateDownload.Ready && isSignedByAnotherKey(release)) {
                    _problem.value = InstallProblem.SignedDifferently
                }
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

    /**
     * Whether the installer should open now. Not when the phone looks too full for the update and
     * the learner has not been told yet: [problem] then says how much to free. Once told, the next
     * tap goes ahead anyway, since the estimate is a rule of thumb rather than Android's exact sum.
     */
    suspend fun checkBeforeInstall(release: AppReleaseDto): Boolean = withContext(Dispatchers.IO) {
        val current = _problem.value
        if (current == InstallProblem.SignedDifferently) return@withContext false
        val short = shortfall(release)
        if (short != null && current !is InstallProblem.NoSpace) {
            _problem.value = InstallProblem.NoSpace(short)
            false
        } else {
            _problem.value = null
            true
        }
    }

    /**
     * The system installer for the downloaded file, or null if it is gone.
     *
     * [Intent.EXTRA_RETURN_RESULT] has the installer hand back its result code instead of showing
     * "App not installed", so [onInstallerResult] can say why. An installer that ignores it shows
     * its own messages as before.
     */
    fun installIntent(release: AppReleaseDto): Intent? {
        val file = fileFor(release)?.takeIf { it.exists() } ?: return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
    }

    /**
     * What the installer reported, when it came back without replacing the app. A successful
     * update never gets here: Android stops the old app to install the new one.
     */
    fun onInstallerResult(release: AppReleaseDto, resultCode: Int, data: Intent?) {
        if (_versionCode.value != release.versionCode) return
        val installResult = data?.getIntExtra(EXTRA_INSTALL_RESULT, INSTALL_FAILED_INTERNAL_ERROR)
        val found = installProblemFor(resultCode, installResult)
        if (found != null) Log.w("UpdateDownloader", "The installer refused ${release.versionName}: $installResult")
        _problem.value = when (found) {
            is InstallProblem.NoSpace -> InstallProblem.NoSpace(shortfall(release))
            InstallProblem.Damaged -> {
                discard(release)
                found
            }
            else -> found
        }
    }

    /** A damaged download goes, so "Try again" fetches a fresh copy. */
    private fun discard(release: AppReleaseDto) {
        pollJob?.cancel()
        ids.getLong(key(release), -1L).takeIf { it >= 0 }?.let { downloads?.remove(it) }
        ids.edit().remove(key(release)).apply()
        fileFor(release)?.delete()
        _state.value = UpdateDownload.Failed
    }

    /** Bytes still to free before [release] fits, or null when it fits or the space cannot be read. */
    private fun shortfall(release: AppReleaseDto): Long? {
        val size = fileFor(release)?.takeIf { it.exists() }?.length() ?: return null
        val free = runCatching {
            context.getSystemService(StorageManager::class.java)?.getAllocatableBytes(StorageManager.UUID_DEFAULT)
        }.getOrNull() ?: return null
        return storageShortfall(size, free).takeIf { it > 0 }
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

    /**
     * Android 9 and 10 read an APK file's certificates only when GET_SIGNATURES is asked for, even
     * though GET_SIGNING_CERTIFICATES is the flag that fills [PackageInfo.signingInfo]. Asked for that
     * alone they returned none, so a test build there went to the installer and its bare "App not
     * installed" instead of being explained. Both flags are passed, as Android 11 and later need too.
     */
    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, file: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES)
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

// PackageManager's install result codes and the extra that carries them, which the SDK keeps
// hidden. The installer returns one when it is started with Intent.EXTRA_RETURN_RESULT.
private const val EXTRA_INSTALL_RESULT = "android.intent.extra.INSTALL_RESULT"
internal const val INSTALL_SUCCEEDED = 1
internal const val INSTALL_FAILED_INVALID_APK = -2
internal const val INSTALL_FAILED_INVALID_URI = -3
internal const val INSTALL_FAILED_INSUFFICIENT_STORAGE = -4
internal const val INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7
internal const val INSTALL_FAILED_INTERNAL_ERROR = -110
internal const val INSTALL_FAILED_ABORTED = -115
/** INSTALL_PARSE_FAILED_NOT_APK to INSTALL_PARSE_FAILED_MANIFEST_EMPTY: a file Android could not read. */
internal val INSTALL_PARSE_FAILED = -109..-100

/**
 * What the installer's answer means for the learner, or null when there is nothing to explain: it
 * installed, or the learner cancelled. [installResult] is null when the installer sent no code.
 */
internal fun installProblemFor(resultCode: Int, installResult: Int?): InstallProblem? {
    if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) return null
    return when (val code = installResult ?: INSTALL_FAILED_INTERNAL_ERROR) {
        INSTALL_SUCCEEDED, INSTALL_FAILED_ABORTED -> null
        INSTALL_FAILED_INSUFFICIENT_STORAGE -> InstallProblem.NoSpace(null)
        INSTALL_FAILED_UPDATE_INCOMPATIBLE -> InstallProblem.SignedDifferently
        INSTALL_FAILED_INVALID_APK, INSTALL_FAILED_INVALID_URI, in INSTALL_PARSE_FAILED -> InstallProblem.Damaged
        else -> InstallProblem.Refused(code)
    }
}

/**
 * Bytes to free before an update of [apkBytes] fits, given the [allocatableBytes] Android would let
 * an install use. The installer keeps its own copy of the file while it installs, and the installed
 * app takes as much again, so the downloaded file alone is not enough room.
 */
internal fun storageShortfall(apkBytes: Long, allocatableBytes: Long): Long =
    (apkBytes * 2 - allocatableBytes).coerceAtLeast(0L)

/** [bytes] as whole megabytes, rounded up to a ten, for "free up about 90 MB". */
internal fun megabytesToFree(bytes: Long): Long = ((bytes + 9_999_999L) / 10_000_000L) * 10L
