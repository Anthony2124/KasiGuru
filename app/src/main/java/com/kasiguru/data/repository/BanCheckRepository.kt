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
     * @param reason   The human-readable reason the admin provided.
     * @param bannedAt Epoch milliseconds when the ban was placed.
     */
    data class Banned(val reason: String, val bannedAt: Long) : BanStatus
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
                trySend(BanStatus.Banned(reason = reason, bannedAt = bannedAt))
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
            BanStatus.Banned(reason = reason, bannedAt = bannedAt)
        } catch (e: Exception) {
            Log.w("BanCheckRepository", "Ban check failed — treating as clear", e)
            BanStatus.Clear
        }
    }
}
