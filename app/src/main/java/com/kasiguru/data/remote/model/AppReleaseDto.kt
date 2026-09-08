package com.kasiguru.data.remote.model

import com.google.firebase.firestore.DocumentId

data class AppReleaseDto(
    @DocumentId
    val versionId: String = "",
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val releasedAt: Long = System.currentTimeMillis(),
    /**
     * Set by an admin in the release manager to pull a bad build. A yanked release is skipped when
     * the app and the download page pick "the latest one", so installs move to the previous good
     * build. It does not downgrade someone already on the yanked version — Android will not install
     * an older APK over a newer one — so recovering those users still means shipping a fixed build.
     */
    val yanked: Boolean = false
)
