package com.kasiguru.data.remote.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * An admin-authored system announcement, read live from Firestore `announcements` while the app
 * is open. There is no push-while-closed delivery here - that would need FCM, and sending an FCM
 * message from the admin dashboard itself is not possible on the Spark plan (the browser cannot
 * safely hold the service-account key FCM's v1 API needs; see functions/send_push.js, which sends
 * pushes from a local script instead). This is the fully free-tier-compatible half of that: a
 * live Firestore listener, the same shape LeaderboardRepository already uses.
 *
 * Read field by field with [from], never with `toObject`. This class used to mark [id] with
 * `@DocumentId`, and Firestore refuses to map a document that also has a field named `id`, which
 * the admin portal wrote into every announcement. The refusal was thrown inside the snapshot
 * listener, where nothing could catch it, so posting an announcement stopped the app from
 * opening for every learner (2026-10-11).
 */
data class AnnouncementDto(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * [doc] as an announcement, or null when it has no message to show. The id is the
         * document's own; an `id` field inside it, if any, is ignored. Values of the wrong type
         * read as their defaults rather than failing.
         */
        fun from(doc: DocumentSnapshot): AnnouncementDto? {
            val message = (doc.get("message") as? String)?.trim().orEmpty()
            if (message.isEmpty()) return null
            return AnnouncementDto(
                id = doc.id,
                title = (doc.get("title") as? String)?.trim().orEmpty(),
                message = message,
                active = doc.get("active") as? Boolean ?: true,
                createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
            )
        }
    }
}
