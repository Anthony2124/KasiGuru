package com.kasiguru.data.remote.model

import com.google.firebase.firestore.DocumentId

/**
 * An admin-authored system announcement, read live from Firestore `announcements` while the app
 * is open. There is no push-while-closed delivery here - that would need FCM, and sending an FCM
 * message from the admin dashboard itself is not possible on the Spark plan (the browser cannot
 * safely hold the service-account key FCM's v1 API needs; see functions/send_push.js, which sends
 * pushes from a local script instead). This is the fully free-tier-compatible half of that: a
 * live Firestore listener, the same shape LeaderboardRepository already uses.
 */
data class AnnouncementDto(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
