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

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgressDirect(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: UserProgressEntity)

    @Query("UPDATE user_progress SET totalXp = totalXp + :xp WHERE id = 1")
    suspend fun addXp(xp: Int)

    /**
     * Adds to today's XP ledger, resetting it first when the stored date is not [today].
     * Done in SQL so the read-modify-write cannot interleave with another coroutine.
     */
    @Query(
        """UPDATE user_progress
           SET dailyXpEarned = CASE WHEN dailyXpDate = :today THEN dailyXpEarned + :xp ELSE :xp END,
               dailyXpDate = :today
           WHERE id = 1"""
    )
    suspend fun addDailyXp(xp: Int, today: String)

    @Query("UPDATE user_progress SET wordsLearned = wordsLearned + 1 WHERE id = 1")
    suspend fun incrementWordsLearned()

    @Query("UPDATE user_progress SET storiesCompleted = storiesCompleted + 1 WHERE id = 1")
    suspend fun incrementStoriesCompleted()

    @Query("UPDATE user_progress SET gamesPlayed = gamesPlayed + 1 WHERE id = 1")
    suspend fun incrementGamesPlayed()

    @Query("UPDATE user_progress SET submissionsMade = submissionsMade + 1 WHERE id = 1")
    suspend fun incrementSubmissionsMade()

    @Query("UPDATE user_progress SET totalCorrectAnswers = totalCorrectAnswers + :correct, totalQuestionsAnswered = totalQuestionsAnswered + :total WHERE id = 1")
    suspend fun updateGameStats(correct: Int, total: Int)

    @Query("UPDATE user_progress SET currentStreak = :streak, longestStreak = CASE WHEN :streak > longestStreak THEN :streak ELSE longestStreak END, lastActiveDate = :date WHERE id = 1")
    suspend fun updateStreak(streak: Int, date: String)

    @Query("UPDATE user_progress SET level = :level WHERE id = 1")
    suspend fun updateLevel(level: Int)

    @Query("UPDATE user_progress SET userName = :name WHERE id = 1")
    suspend fun updateUserName(name: String)

    @Query("UPDATE user_progress SET fullName = :fullName, age = :age, address = :address, profileIconId = :iconId WHERE id = 1")
    suspend fun updateProfileDetails(fullName: String, age: Int?, address: String, iconId: Int)

    @Query("UPDATE user_progress SET isOnboardingCompleted = 1, userName = :userName, profileIconId = :avatarId, dailyGoalXp = :dailyGoalXp, titleBadge = :titleBadge, totalXp = totalXp + 50, currentStreak = CASE WHEN currentStreak < 1 THEN 1 ELSE currentStreak END, longestStreak = CASE WHEN longestStreak < 1 THEN 1 ELSE longestStreak END WHERE id = 1")
    suspend fun completeOnboarding(userName: String, avatarId: Int, dailyGoalXp: Int, titleBadge: String)

    @Query("UPDATE user_progress SET dailyReviewCompletedDate = :today WHERE id = 1")
    suspend fun recordDailyReviewCompleted(today: String)

    @Query(
        """UPDATE user_progress
           SET dailyGamesPlayedCount = CASE WHEN dailyGamesDate = :today THEN dailyGamesPlayedCount + 1 ELSE 1 END,
               dailyGamesDate = :today
           WHERE id = 1"""
    )
    suspend fun recordDailyGamePlayed(today: String)
}
