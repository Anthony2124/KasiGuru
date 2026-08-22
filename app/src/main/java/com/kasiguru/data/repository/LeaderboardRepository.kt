package com.kasiguru.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kasiguru.data.local.dao.LeaderboardDao
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.data.local.entity.MetricType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real, cross-user rankings.
 *
 * Reads `leaderboard_public`, which a Cloud Function maintains from each user's
 * own progress document — clients cannot write it, so a rank always reflects
 * what the database actually holds. Rows are cached locally so the screen still
 * renders offline; the cache is never seeded with placeholder players.
 */
@Singleton
class LeaderboardRepository @Inject constructor(
    private val leaderboardDao: LeaderboardDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userProgressRepository: UserProgressRepository
) {
    fun getLeaderboardByXp(): Flow<List<LeaderboardEntity>> = leaderboard(FIELD_XP)

    fun getLeaderboardByStreak(): Flow<List<LeaderboardEntity>> = leaderboard(FIELD_STREAK)

    /** This week's XP, reset by the weekly rollover function. */
    fun getWeeklyLeaderboard(): Flow<List<LeaderboardEntity>> = leaderboard(FIELD_WEEKLY_XP).map { entries ->
        // Backs the "Top of the Week" badge: checked here, not from a screen, so it unlocks
        // whenever the live ranking crosses top 10, not only while the leaderboard is open.
        if (entries.take(10).any { it.isCurrentUser }) {
            runCatching { userProgressRepository.checkAchievements(MetricType.WEEKLY_TOP_TEN, 1) }
        }
        entries
    }

    private fun leaderboard(orderField: String): Flow<List<LeaderboardEntity>> = flow {
        // Show the cached rankings immediately, then live values as they arrive.
        emit(leaderboardDao.getLeaderboardByXp().first())
        emitAll(remoteLeaderboard(orderField))
    }.catch { e ->
        Log.w(TAG, "leaderboard unavailable, serving cache", e)
        emitAll(leaderboardDao.getLeaderboardByXp())
    }

    private fun remoteLeaderboard(orderField: String): Flow<List<LeaderboardEntity>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .orderBy(orderField, Query.Direction.DESCENDING)
            .limit(TOP_N)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val currentUid = auth.currentUser?.uid
                val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                    val name = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                    LeaderboardEntity(
                        // Room needs a stable local id; rank position is enough for a cache.
                        id = index + 1,
                        name = name,
                        totalXp = (doc.getLong(FIELD_XP) ?: 0L).toInt(),
                        currentStreak = (doc.getLong(FIELD_STREAK) ?: 0L).toInt(),
                        avatarIconId = (doc.getLong("profileIconId") ?: 1L).toInt(),
                        levelTitle = doc.getString("titleBadge") ?: "Kasiguranin Apprentice",
                        isCurrentUser = doc.id == currentUid
                    )
                }
                trySend(entries)
            }
        awaitClose { registration.remove() }
    }.map { entries ->
        // Refresh the offline cache with the real rankings.
        runCatching {
            leaderboardDao.clearAll()
            leaderboardDao.insertAll(entries)
        }
        entries
    }

    private companion object {
        const val TAG = "Leaderboard"
        const val COLLECTION = "leaderboard_public"
        const val FIELD_XP = "totalXp"
        const val FIELD_STREAK = "currentStreak"
        const val FIELD_WEEKLY_XP = "weeklyXp"
        const val TOP_N = 50L
    }
}
