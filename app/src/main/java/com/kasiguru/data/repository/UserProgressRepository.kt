package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.AchievementDao
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.util.Constants
import com.kasiguru.util.calculateLevel
import com.kasiguru.util.toIsoString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val achievementDao: AchievementDao
) {
    // A level-up is a screen-agnostic celebratory moment (LevelUpDialog): XP is earned from lessons,
    // flashcards and all six mini-games, so the event lives here at the one place that already
    // detects a level change, rather than being duplicated at every XP-awarding call site.
    private val _levelUpEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val levelUpEvents: SharedFlow<Int> = _levelUpEvents.asSharedFlow()

    fun getUserProgress(): Flow<UserProgressEntity?> =
        userProgressDao.getUserProgress()

    suspend fun getUserProgressOnce(): UserProgressEntity? =
        userProgressDao.getUserProgressOnce()

    suspend fun initializeProgress(progress: UserProgressEntity) =
        userProgressDao.insertOrUpdate(progress)

    suspend fun registerSimpleUser(fullName: String, age: Int?, address: String) {
        val current = userProgressDao.getUserProgressOnce()
        val updated = (current ?: UserProgressEntity()).copy(
            userName = fullName,
            fullName = fullName,
            age = age,
            address = address
        )
        userProgressDao.insertOrUpdate(updated)
    }

    suspend fun updateProfileDetails(fullName: String, age: Int?, address: String, iconId: Int) =
        userProgressDao.updateProfileDetails(fullName, age, address, iconId)

    /**
     * The row is created if it is missing before the update runs.
     *
     * [UserProgressDao.completeOnboarding] is an `UPDATE ... WHERE id = 1`, so on a fresh install it
     * would otherwise affect zero rows and fail silently — the learner would finish the whole wizard,
     * lose the +50 XP welcome bonus and the day-1 streak, and be shown onboarding again on the next
     * cold start. The database callback seeds this row on create, but that seed runs asynchronously,
     * so this guard closes the race rather than relying on the ordering.
     */
    suspend fun completeOnboarding(userName: String, avatarId: Int, dailyGoalXp: Int, titleBadge: String) {
        if (userProgressDao.getUserProgressOnce() == null) {
            userProgressDao.insertOrUpdate(UserProgressEntity())
        }
        userProgressDao.completeOnboarding(userName, avatarId, dailyGoalXp, titleBadge)
    }

    suspend fun addXp(xp: Int) {
        userProgressDao.addXp(xp)
        // Every XP award also lands in today's ledger, so the daily-goal ring reflects real activity
        // no matter which surface earned it.
        userProgressDao.addDailyXp(xp, LocalDate.now().toIsoString())
        // Recalculate level
        val progress = userProgressDao.getUserProgressOnce() ?: return
        val newLevel = calculateLevel(progress.totalXp)
        if (newLevel != progress.level) {
            userProgressDao.updateLevel(newLevel)
            // Check level achievements
            checkLevelAchievements(newLevel)
            _levelUpEvents.tryEmit(newLevel)
        }
    }

    suspend fun incrementWordsLearned() {
        userProgressDao.incrementWordsLearned()
        val progress = userProgressDao.getUserProgressOnce() ?: return
        checkWordAchievements(progress.wordsLearned)
    }

    suspend fun incrementStoriesCompleted() {
        userProgressDao.incrementStoriesCompleted()
        val progress = userProgressDao.getUserProgressOnce() ?: return
        checkStoryAchievements(progress.storiesCompleted)
    }

    suspend fun incrementGamesPlayed() {
        userProgressDao.incrementGamesPlayed()
        val progress = userProgressDao.getUserProgressOnce() ?: return
        checkGameAchievements(progress.gamesPlayed)
    }

    suspend fun updateGameStats(correct: Int, total: Int) =
        userProgressDao.updateGameStats(correct, total)

    suspend fun getRollingAccuracyRate(): Float {
        val progress = userProgressDao.getUserProgressOnce() ?: return 1.0f
        if (progress.totalQuestionsAnswered < 10) return 1.0f // Grace period for new users
        return (progress.totalCorrectAnswers.toFloat() / progress.totalQuestionsAnswered.toFloat()).coerceIn(0.0f, 1.0f)
    }

    suspend fun updateStreak() {
        val progress = userProgressDao.getUserProgressOnce() ?: return
        val today = LocalDate.now().toIsoString()

        if (progress.lastActiveDate == today) return // Already updated today

        val newStreak = if (progress.lastActiveDate.isNotEmpty()) {
            val lastDate = LocalDate.parse(progress.lastActiveDate)
            val daysBetween = ChronoUnit.DAYS.between(lastDate, LocalDate.now())
            if (daysBetween <= 1) progress.currentStreak + 1 else 1
        } else {
            1
        }

        userProgressDao.updateStreak(newStreak, today)
        checkStreakAchievements(newStreak)
    }

    suspend fun updateUserName(name: String) =
        userProgressDao.updateUserName(name)

    // ─── Achievement Checks ───

    private suspend fun checkWordAchievements(wordsLearned: Int) {
        val today = LocalDate.now().toIsoString()
        if (wordsLearned >= 1) tryUnlock(Constants.Achievements.FIRST_WORD, today)
        if (wordsLearned >= 10) tryUnlock(Constants.Achievements.TEN_WORDS, today)
        if (wordsLearned >= 50) tryUnlock(Constants.Achievements.FIFTY_WORDS, today)
    }

    private suspend fun checkStoryAchievements(storiesCompleted: Int) {
        val today = LocalDate.now().toIsoString()
        if (storiesCompleted >= 1) tryUnlock(Constants.Achievements.FIRST_STORY, today)
        if (storiesCompleted >= 3) tryUnlock(Constants.Achievements.ALL_STORIES, today)
    }

    private suspend fun checkGameAchievements(gamesPlayed: Int) {
        val today = LocalDate.now().toIsoString()
        if (gamesPlayed >= 1) tryUnlock(Constants.Achievements.FIRST_GAME, today)
    }

    suspend fun checkPerfectGameAchievement() {
        val today = LocalDate.now().toIsoString()
        tryUnlock(Constants.Achievements.PERFECT_GAME, today)
    }

    private suspend fun checkStreakAchievements(streak: Int) {
        val today = LocalDate.now().toIsoString()
        if (streak >= 3) tryUnlock(Constants.Achievements.THREE_DAY_STREAK, today)
        if (streak >= 7) tryUnlock(Constants.Achievements.SEVEN_DAY_STREAK, today)
    }

    private suspend fun checkLevelAchievements(level: Int) {
        val today = LocalDate.now().toIsoString()
        if (level >= 5) tryUnlock(Constants.Achievements.LEVEL_FIVE, today)
        if (level >= 10) tryUnlock(Constants.Achievements.LEVEL_TEN, today)
    }

    private suspend fun tryUnlock(id: String, date: String) {
        val achievement = achievementDao.getAchievementById(id)
        if (achievement != null && !achievement.isUnlocked) {
            achievementDao.unlockAchievement(id, date)
            // Award bonus XP for unlocking
            userProgressDao.addXp(achievement.xpReward)
        }
    }

    // Achievements
    fun getAllAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getAllAchievements()

    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getUnlockedAchievements()

    fun getUnlockedAchievementCount(): Flow<Int> =
        achievementDao.getUnlockedCount()
}
