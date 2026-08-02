package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgressOnce(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: UserProgressEntity)

    @Query("UPDATE user_progress SET totalXp = totalXp + :xp WHERE id = 1")
    suspend fun addXp(xp: Int)

    @Query("UPDATE user_progress SET wordsLearned = wordsLearned + 1 WHERE id = 1")
    suspend fun incrementWordsLearned()

    @Query("UPDATE user_progress SET storiesCompleted = storiesCompleted + 1 WHERE id = 1")
    suspend fun incrementStoriesCompleted()

    @Query("UPDATE user_progress SET gamesPlayed = gamesPlayed + 1 WHERE id = 1")
    suspend fun incrementGamesPlayed()

    @Query("UPDATE user_progress SET totalCorrectAnswers = totalCorrectAnswers + :correct, totalQuestionsAnswered = totalQuestionsAnswered + :total WHERE id = 1")
    suspend fun updateGameStats(correct: Int, total: Int)

    @Query("UPDATE user_progress SET currentStreak = :streak, longestStreak = CASE WHEN :streak > longestStreak THEN :streak ELSE longestStreak END, lastActiveDate = :date WHERE id = 1")
    suspend fun updateStreak(streak: Int, date: String)

    @Query("UPDATE user_progress SET level = :level WHERE id = 1")
    suspend fun updateLevel(level: Int)

    @Query("UPDATE user_progress SET userName = :name WHERE id = 1")
    suspend fun updateUserName(name: String)
}
