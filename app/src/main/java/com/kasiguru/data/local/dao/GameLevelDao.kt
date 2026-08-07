package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.GameLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameLevelDao {

    @Query("SELECT * FROM game_levels WHERE gameType = :gameType ORDER BY levelNumber ASC")
    fun getLevelsByGame(gameType: String): Flow<List<GameLevelEntity>>

    @Query("SELECT * FROM game_levels WHERE gameType = :gameType AND levelNumber = :levelNumber")
    suspend fun getLevel(gameType: String, levelNumber: Int): GameLevelEntity?

    @Query("SELECT COALESCE(SUM(starsEarned), 0) FROM game_levels")
    suspend fun getTotalStars(): Int

    @Query("SELECT COALESCE(SUM(starsEarned), 0) FROM game_levels")
    fun getTotalStarsFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(starsEarned), 0) FROM game_levels WHERE gameType = :gameType")
    suspend fun getTotalStarsForGame(gameType: String): Int

    @Query("UPDATE game_levels SET starsEarned = :stars WHERE gameType = :gameType AND levelNumber = :levelNumber")
    suspend fun updateStars(gameType: String, levelNumber: Int, stars: Int)

    @Query("UPDATE game_levels SET isUnlocked = 1 WHERE gameType = :gameType AND levelNumber = :levelNumber")
    suspend fun unlockLevel(gameType: String, levelNumber: Int)

    @Query("SELECT COUNT(*) FROM game_levels")
    suspend fun getLevelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(levels: List<GameLevelEntity>)

    @Update
    suspend fun update(level: GameLevelEntity)
}
