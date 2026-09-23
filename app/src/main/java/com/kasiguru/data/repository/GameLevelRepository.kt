package com.kasiguru.data.repository

import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.dao.GameLevelDao
import com.kasiguru.data.local.entity.GameLevelEntity
import com.kasiguru.util.LearningAnalytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/** A level is scored out of three stars everywhere in the game system. */
private const val STARS_PER_LEVEL = 3

@Singleton
class GameLevelRepository @Inject constructor(
    private val gameLevelDao: GameLevelDao
) {
    /**
     * Returns levels for a game type, seeding any missing level rows first.
     */
    fun getLevelsByGame(gameType: String): Flow<List<GameLevelEntity>> = flow {
        ensureLevelsSeeded()
        emitAll(gameLevelDao.getLevelsByGame(gameType))
    }

    /**
     * Ensures every game level row is present: the six games' 30 levels each, plus Word Search's
     * 30 per category. Called lazily on first access, and tops up installs that predate a new track.
     */
    suspend fun ensureLevelsSeeded() {
        val initial = DatabaseSeeder.getInitialGameLevels()
        if (gameLevelDao.getLevelCount() < initial.size) {
            // Not insertAll: that REPLACEs, and would wipe the stars on every level already played.
            gameLevelDao.insertMissing(initial)
        }
    }

    /** Every level row, for screens that summarise several level tracks at once (Word Search). */
    fun getAllLevels(): Flow<List<GameLevelEntity>> = flow {
        ensureLevelsSeeded()
        emitAll(gameLevelDao.getAllLevelsForSync())
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
        // Every mini-game routes its result through here, so this is the one place that knows
        // which game finished. Logging in the six ViewModels instead would be six chances to miss one.
        LearningAnalytics.gameFinished(gameType, starsEarned, STARS_PER_LEVEL)
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
