package com.kasiguru.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the ban state for a user's UID.
 */
sealed interface BanStatus {
    /** User is not banned — proceed normally. */
    data object Clear : BanStatus

    /**
     * User has an active ban.
     *
     * @param reason              The human-readable reason the admin provided.
     * @param bannedAt            Epoch milliseconds when the ban was placed.
     * @param appealText          The user's appeal statement, if submitted.
     * @param appealSubmittedAt   Epoch milliseconds when the appeal was submitted.
     * @param appealStatus        Status of the appeal: "pending", "rejected", etc.
     * @param appealReviewNotes   Moderator's notes or reason upon review.
     * @param appealReviewedAt    Epoch milliseconds when the appeal was reviewed.
     */
    data class Banned(
        val reason: String,
        val bannedAt: Long,
        val appealText: String? = null,
        val appealSubmittedAt: Long? = null,
        val appealStatus: String? = null,
        val appealReviewNotes: String? = null,
        val appealReviewedAt: Long? = null
    ) : BanStatus
}

/**
 * Checks and observes whether a user has an active ban in `user_bans/{uid}`.
 *
 * Admins manage ban records from the web dashboard. The Firestore security rule
 * (`user_bans/{userId}`) grants each user owner-read on their own document,
 * enabling real-time ban enforcement without requiring a Cloud Function.
 */
@Singleton
class BanCheckRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Real-time listener on `user_bans/{uid}`.
     * Emits immediately on first read and re-emits whenever the ban record is
     * created, modified, or deleted by an admin.
     */
    fun observeBan(uid: String): Flow<BanStatus> = callbackFlow {
        val listener = firestore.collection("user_bans").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Fail-open: network hiccups or permission sync delays should not lock out users
                    Log.w("BanCheckRepository", "Ban snapshot error for $uid — treating as clear", error)
                    trySend(BanStatus.Clear)
                    return@addSnapshotListener
                }

                if (snap == null || !snap.exists()) {
                    trySend(BanStatus.Clear)
                    return@addSnapshotListener
                }

                val isBanned = snap.getBoolean("isBanned") ?: false
                if (!isBanned) {
                    trySend(BanStatus.Clear)
                    return@addSnapshotListener
                }

                val reason = snap.getString("reason")?.takeIf { it.isNotBlank() }
                    ?: "Your account has been suspended. Please contact support."
                val bannedAt = snap.getLong("bannedAt") ?: 0L
                val appealText = snap.getString("appealText")
                val appealSubmittedAt = snap.getLong("appealSubmittedAt")
                val appealStatus = snap.getString("appealStatus")
                val appealReviewNotes = snap.getString("appealReviewNotes")
                val appealReviewedAt = snap.getLong("appealReviewedAt")

                trySend(
                    BanStatus.Banned(
                        reason = reason,
                        bannedAt = bannedAt,
                        appealText = appealText,
                        appealSubmittedAt = appealSubmittedAt,
                        appealStatus = appealStatus,
                        appealReviewNotes = appealReviewNotes,
                        appealReviewedAt = appealReviewedAt
                    )
                )
            }

        awaitClose { listener.remove() }
    }

    /**
     * One-shot read of `user_bans/{uid}`.
     */
    suspend fun checkBan(uid: String): BanStatus {
        return try {
            val snap = firestore.collection("user_bans").document(uid).get().await()
            if (!snap.exists()) return BanStatus.Clear
            val isBanned = snap.getBoolean("isBanned") ?: false
            if (!isBanned) return BanStatus.Clear
            val reason = snap.getString("reason")?.takeIf { it.isNotBlank() }
                ?: "Your account has been suspended. Please contact support."
            val bannedAt = snap.getLong("bannedAt") ?: 0L
            val appealText = snap.getString("appealText")
            val appealSubmittedAt = snap.getLong("appealSubmittedAt")
            val appealStatus = snap.getString("appealStatus")
            val appealReviewNotes = snap.getString("appealReviewNotes")
            val appealReviewedAt = snap.getLong("appealReviewedAt")

            BanStatus.Banned(
                reason = reason,
                bannedAt = bannedAt,
                appealText = appealText,
                appealSubmittedAt = appealSubmittedAt,
                appealStatus = appealStatus,
                appealReviewNotes = appealReviewNotes,
                appealReviewedAt = appealReviewedAt
            )
        } catch (e: Exception) {
            Log.w("BanCheckRepository", "Ban check failed — treating as clear", e)
            BanStatus.Clear
        }
    }

    /**
     * Submits an appeal on behalf of the suspended user.
     * Updates `user_bans/{uid}` with appealText and sets status to "pending".
     */
    suspend fun submitAppeal(uid: String, appealText: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "appealText" to appealText.trim(),
                "appealSubmittedAt" to System.currentTimeMillis(),
                "appealStatus" to "pending"
            )
            firestore.collection("user_bans").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BanCheckRepository", "Failed to submit appeal for $uid", e)
            Result.failure(e)
        }
    }
}
