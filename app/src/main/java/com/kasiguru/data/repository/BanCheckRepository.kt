package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the ban state for the current user's UID.
 */
sealed interface BanStatus {
    /** User is not banned — proceed normally. */
    data object Clear : BanStatus

    /**
     * User has an active ban.
     *
     * @param reason   The human-readable reason the admin provided.
     * @param bannedAt Epoch milliseconds when the ban was placed.
     */
    data class Banned(val reason: String, val bannedAt: Long) : BanStatus
}

/**
 * Checks whether the current user has an active ban in `user_bans/{uid}`.
 *
 * The admin writes ban records from the admin dashboard. The Firestore rule
 * (`user_bans/{userId}`) grants each user owner-read on their own document,
 * so this check requires no Cloud Function on the Spark plan.
 *
 * A missing document or any read error is treated as "not banned" — a transient
 * network error or a permission setup issue should never prevent a user from
 * accessing the app.
 */
@Singleton
class BanCheckRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * One-shot read of `user_bans/{uid}`.
     *
     * Returns [BanStatus.Clear] when:
     *  - the document does not exist (never banned / already unblocked), OR
     *  - the `isBanned` field is false or absent, OR
     *  - any Firestore error occurs (fail-open: don't block the app on a network hiccup).
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
            BanStatus.Banned(reason = reason, bannedAt = bannedAt)
        } catch (e: Exception) {
            // Fail-open: a network error or misconfigured rule should not lock out
            // an innocent user. Log in debug builds and proceed.
            android.util.Log.w("BanCheckRepository", "Ban check failed — treating as clear", e)
            BanStatus.Clear
        }
    }
}
