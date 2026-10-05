package com.kasiguru.util.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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

    private val folder: File? get() = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    private fun fileFor(release: AppReleaseDto) = folder?.let { File(it, "kasiguru-v${release.versionName}.apk") }

    /** Picks up a download for [release] started earlier, so reopening Home shows its progress. */
    fun watch(release: AppReleaseDto) {
        if (_versionCode.value == release.versionCode) return
        _versionCode.value = release.versionCode
        _state.value = UpdateDownload.Idle
        val id = ids.getLong(key(release), -1L)
        if (id >= 0) poll(release, id)
    }

    /** Starts downloading [release]. Returns false when the system downloader cannot be used. */
    fun start(release: AppReleaseDto): Boolean {
        val dm = downloads ?: return false
        val target = fileFor(release) ?: return false
        _versionCode.value = release.versionCode
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

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
