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
    val releasedAt: Long = System.currentTimeMillis()
)
