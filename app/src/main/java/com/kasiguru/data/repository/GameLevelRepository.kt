package com.kasiguru.data.repository

import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.dao.GameLevelDao
import com.kasiguru.data.local.entity.GameLevelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameLevelRepository @Inject constructor(
    private val gameLevelDao: GameLevelDao
) {
    /**
     * Returns levels for a game type. If no levels exist yet in the DB,
     * seeds all 150 levels first (5 games × 30 levels), then emits.
     */
    fun getLevelsByGame(gameType: String): Flow<List<GameLevelEntity>> = flow {
        ensureLevelsSeeded()
        emitAll(gameLevelDao.getLevelsByGame(gameType))
    }

    /**
     * Ensures all 150 game levels are present. Called lazily on first access.
     */
    suspend fun ensureLevelsSeeded() {
        if (gameLevelDao.getLevelCount() == 0) {
            gameLevelDao.insertAll(DatabaseSeeder.getInitialGameLevels())
        }
    }

    suspend fun getLevel(gameType: String, levelNumber: Int): GameLevelEntity? {
        ensureLevelsSeeded()
        return gameLevelDao.getLevel(gameType, levelNumber)
    }

    suspend fun getTotalStars(): Int =
        gameLevelDao.getTotalStars()

    fun getTotalStarsFlow(): Flow<Int> =
        gameLevelDao.getTotalStarsFlow()

    suspend fun getTotalStarsForGame(gameType: String): Int =
        gameLevelDao.getTotalStarsForGame(gameType)

    /**
     * Saves the stars earned for a level. If it's a new high score, it saves it and unlocks the next level.
     */
    suspend fun saveLevelResult(gameType: String, levelNumber: Int, starsEarned: Int) {
        ensureLevelsSeeded()
        val currentLevel = gameLevelDao.getLevel(gameType, levelNumber)
        
        if (currentLevel != null && starsEarned > currentLevel.starsEarned) {
            gameLevelDao.updateStars(gameType, levelNumber, starsEarned)
        }
        
        // If they earned at least 1 star, unlock the next level
        if (starsEarned >= 1) {
            val nextLevel = gameLevelDao.getLevel(gameType, levelNumber + 1)
            if (nextLevel != null && !nextLevel.isUnlocked) {
                gameLevelDao.unlockLevel(gameType, levelNumber + 1)
            }
        }
    }
}
