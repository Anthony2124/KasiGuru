package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.LeaderboardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {

    @Query("SELECT * FROM leaderboard ORDER BY totalXp DESC")
    fun getLeaderboardByXp(): Flow<List<LeaderboardEntity>>

    @Query("SELECT * FROM leaderboard ORDER BY currentStreak DESC")
    fun getLeaderboardByStreak(): Flow<List<LeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LeaderboardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LeaderboardEntity)

    @Query("SELECT * FROM leaderboard WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserEntry(): LeaderboardEntity?

    @Query("UPDATE leaderboard SET totalXp = :xp, currentStreak = :streak, name = :name, levelTitle = :levelTitle WHERE isCurrentUser = 1")
    suspend fun updateCurrentUser(xp: Int, streak: Int, name: String, levelTitle: String)

    @Query("DELETE FROM leaderboard WHERE isCurrentUser = 1")
    suspend fun deleteCurrentUser()

    @Query("SELECT COUNT(*) FROM leaderboard")
    suspend fun getLeaderboardCount(): Int

    @Query("DELETE FROM leaderboard")
    suspend fun clearAll()
}
