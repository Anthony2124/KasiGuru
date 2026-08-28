package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.GameScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, name ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedDate DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): AchievementEntity?

    /**
     * The one query the generic evaluator needs: every not-yet-unlocked badge in a metric
     * family, so UserProgressRepository.checkAchievements can compare [AchievementEntity.requiredValue]
     * itself rather than a threshold hardcoded per badge id.
     */
    @Query("SELECT * FROM achievements WHERE metricType = :metricType AND isUnlocked = 0")
    suspend fun getLockedByMetricType(metricType: String): List<AchievementEntity>

    @Query("SELECT id FROM achievements")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getAchievementCount(): Int

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>

    @Query("SELECT * FROM achievements")
    fun getAllAchievementsForSync(): Flow<List<AchievementEntity>>

    @Query("UPDATE achievements SET currentValue = :value WHERE id = :id")
    suspend fun updateProgress(id: String, value: Int)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedDate = :date, currentValue = requiredValue WHERE id = :id")
    suspend fun unlockAchievement(id: String, date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity)
}

@Dao
interface GameScoreDao {

    @Query("SELECT * FROM game_scores ORDER BY playedAt DESC")
    fun getAllScores(): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores WHERE gameType = :type ORDER BY score DESC LIMIT 1")
    suspend fun getHighScore(type: String): GameScoreEntity?

    @Query("SELECT * FROM game_scores ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentScores(limit: Int = 10): Flow<List<GameScoreEntity>>

    @Insert
    suspend fun insert(score: GameScoreEntity)

    /** Backs the "Six for Six" badge: how many of the six modes have ever been played. */
    @Query("SELECT COUNT(DISTINCT gameType) FROM game_scores")
    suspend fun getDistinctGameTypesPlayed(): Int

    @Query("DELETE FROM game_scores")
    suspend fun clearAll()
}
