package com.kasiguru.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.kasiguru.data.local.dao.LeaderboardDao
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.domain.gamification.LeaderboardRefreshGate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Public rankings with independent caches for each period; private profiles are never queried. */
@Singleton
class LeaderboardRepository @Inject constructor(
    private val leaderboardDao: LeaderboardDao, private val firestore: FirebaseFirestore, private val auth: FirebaseAuth
) {
    private val refreshGate = LeaderboardRefreshGate()
    fun getLeaderboardByXp() = leaderboard("totalXp", "alltime")
    fun getLeaderboardByStreak() = leaderboard("currentStreak", "streaks")
    fun getWeeklyLeaderboard() = leaderboard("weeklyXp", "weekly")
    private fun weekId(): String {
        val today = java.time.LocalDate.now(); val fields = java.time.temporal.WeekFields.ISO
        return "${today.get(fields.weekBasedYear())}-W${today.get(fields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
    }
    private fun query(board: String, week: String): Query {
        val collection = firestore.collection("leaderboard_public")
        return if (board == "weekly") collection.whereEqualTo("weekStartDate", week) else collection
    }
    private fun entry(doc: DocumentSnapshot, id: Int, board: String, rank: Int): LeaderboardEntity? {
        if (!doc.exists() || doc.getBoolean("isAnonymous") == true) return null
        val name = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: return null
        return LeaderboardEntity(id = id, name = name, totalXp = (doc.getLong("totalXp") ?: 0).toInt(),
            currentStreak = (doc.getLong("currentStreak") ?: 0).toInt(), avatarIconId = (doc.getLong("profileIconId") ?: 1).toInt(),
            levelTitle = doc.getString("titleBadge") ?: "Learner", isCurrentUser = doc.id == auth.currentUser?.uid,
            firebaseUid = doc.id, weeklyXp = if (doc.getString("weekStartDate") == weekId()) (doc.getLong("weeklyXp") ?: 0).toInt() else 0,
            weekId = doc.getString("weekStartDate").orEmpty(), level = (doc.getLong("level") ?: 1).toInt(), boardId = board, rank = rank)
    }
    private fun normalizeCache(rows: List<LeaderboardEntity>, board: String) = rows
        .filter { board != "weekly" || it.weekId == weekId() }
        .map { it.copy(isCurrentUser = it.firebaseUid.isNotBlank() && it.firebaseUid == auth.currentUser?.uid,
            weeklyXp = if (it.weekId == weekId()) it.weeklyXp else 0) }

    private fun leaderboard(field: String, board: String): Flow<List<LeaderboardEntity>> = flow {
        val offset = when (board) { "weekly" -> 100; "streaks" -> 200; else -> 0 }
        emit(normalizeCache(leaderboardDao.board(board).first(), board))
        val week = weekId()
        val user = auth.currentUser
        refreshGate.refresh(board, "${user?.uid}:${user?.isAnonymous}:$week") {
            val base = query(board, week)
            val snapshot = base.orderBy(field, Query.Direction.DESCENDING).limit(50).get(Source.SERVER).await()
            var previousScore: Long? = null
            var previousRank = 0
            val rows = snapshot.documents.filter { it.getBoolean("isAnonymous") != true }.mapIndexedNotNull { i, doc ->
                val score = doc.getLong(field) ?: 0L
                val rank = if (score == previousScore) previousRank else i + 1
                previousScore = score; previousRank = rank
                entry(doc, offset + i + 1, board, rank)
            }.toMutableList()
            if (user != null && !user.isAnonymous && rows.none { it.firebaseUid == user.uid }) {
                try {
                    val doc = firestore.collection("leaderboard_public").document(user.uid).get(Source.SERVER).await()
                    val mine = entry(doc, offset + 99, board, 0)
                    if (mine != null && (board != "weekly" || mine.weekId == week)) {
                        val score = if (board == "weekly") mine.weeklyXp else if (board == "streaks") mine.currentStreak else mine.totalXp
                        val ahead = base.whereGreaterThan(field, score).count().get(AggregateSource.SERVER).await().count
                        rows += mine.copy(rank = (ahead + 1).toInt())
                    }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { /* The board remains usable if your own row cannot be fetched. */ }
            }
            leaderboardDao.replaceBoard(board, rows)
        }
        emitAll(leaderboardDao.board(board).map { normalizeCache(it, board) })
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        emitAll(leaderboardDao.board(board).map { normalizeCache(it, board) })
    }
}
