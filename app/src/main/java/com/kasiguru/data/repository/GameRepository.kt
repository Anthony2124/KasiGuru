package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.GameScoreDao
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.MetricType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameScoreDao: GameScoreDao,
    private val userProgressRepository: UserProgressRepository
) {
    fun getAllScores(): Flow<List<GameScoreEntity>> =
        gameScoreDao.getAllScores()

    fun getRecentScores(limit: Int = 10): Flow<List<GameScoreEntity>> =
        gameScoreDao.getRecentScores(limit)

    suspend fun getHighScore(gameType: String): GameScoreEntity? =
        gameScoreDao.getHighScore(gameType)

    suspend fun saveScore(score: GameScoreEntity) =
        gameScoreDao.insert(score)

    suspend fun saveGameScore(gameType: String, score: Int, totalQuestions: Int, xpEarned: Int) {
        gameScoreDao.insert(
            GameScoreEntity(
                gameType = gameType,
                score = score,
                totalQuestions = totalQuestions,
                xpEarned = xpEarned
            )
        )
        userProgressRepository.checkAchievements(
            MetricType.GAME_MODES_PLAYED,
            gameScoreDao.getDistinctGameTypesPlayed()
        )
    }
}
