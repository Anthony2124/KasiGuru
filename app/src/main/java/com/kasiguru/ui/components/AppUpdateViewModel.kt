package com.kasiguru.ui.components

import androidx.lifecycle.ViewModel
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.util.update.UpdateDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives [AppUpdateBanner] the app-wide [UpdateDownloader], so a download outlives the screen. */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(private val downloader: UpdateDownloader) : ViewModel() {
    val state = downloader.state
    fun watch(release: AppReleaseDto) = downloader.watch(release)
    fun start(release: AppReleaseDto) = downloader.start(release)
    fun canInstall() = downloader.canInstall()
    fun installPermissionIntent() = downloader.installPermissionIntent()
    fun installIntent(release: AppReleaseDto) = downloader.installIntent(release)
}
