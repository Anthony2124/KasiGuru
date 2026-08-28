package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.AchievementDao
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.MetricType
import com.kasiguru.data.local.entity.UserProgressEntity
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
    private val achievementDao: AchievementDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    // A level-up is a screen-agnostic celebratory moment (LevelUpDialog): XP is earned from lessons,
    // flashcards and all six mini-games, so the event lives here at the one place that already
    // detects a level change, rather than being duplicated at every XP-awarding call site.
    private val _levelUpEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val levelUpEvents: SharedFlow<Int> = _levelUpEvents.asSharedFlow()

    private val _streakActivatedEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val streakActivatedEvents: SharedFlow<Int> = _streakActivatedEvents.asSharedFlow()

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
        val today = LocalDate.now().toIsoString()
        userPreferencesRepository.recordDailyGamePlayed(today)
        checkStreakQuotaAndAdvance()
        val progress = userProgressDao.getUserProgressOnce() ?: return
        checkGameAchievements(progress.gamesPlayed)
    }

    suspend fun recordDailyReviewCompleted() {
        val today = LocalDate.now().toIsoString()
        userPreferencesRepository.recordDailyReviewCompleted(today)
        checkStreakQuotaAndAdvance()
    }

    suspend fun updateGameStats(correct: Int, total: Int) =
        userProgressDao.updateGameStats(correct, total)

    suspend fun getRollingAccuracyRate(): Float {
        val progress = userProgressDao.getUserProgressOnce() ?: return 1.0f
        if (progress.totalQuestionsAnswered < 10) return 1.0f // Grace period for new users
        return (progress.totalCorrectAnswers.toFloat() / progress.totalQuestionsAnswered.toFloat()).coerceIn(0.0f, 1.0f)
    }

    /**
     * Checks if today's daily streak quota (complete review words + play 3 mini game levels) is met,
     * advancing the streak only when all requirements are satisfied.
     */
    suspend fun checkStreakQuotaAndAdvance() {
        val today = LocalDate.now().toIsoString()
        val quota = userPreferencesRepository.getDailyStreakQuotaOnce(today)
        if (quota.isQuotaMet) {
            updateStreak()
        }
    }

    /**
     * Backward-compatible hook for general learning activity.
     */
    suspend fun recordLearningActivity() {
        checkStreakQuotaAndAdvance()
    }

    private suspend fun updateStreak() {
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
        _streakActivatedEvents.tryEmit(newStreak)
    }

    suspend fun updateUserName(name: String) =
        userProgressDao.updateUserName(name)

    /** Called on every successful word/story/poem submission, approved or not. */
    suspend fun incrementSubmissionsMade() {
        userProgressDao.incrementSubmissionsMade()
        val progress = userProgressDao.getUserProgressOnce() ?: return
        checkAchievements(MetricType.SUBMISSIONS_MADE, progress.submissionsMade)
    }

    // ─── Achievement Checks ───
    //
    // One generic evaluator replaces what used to be five near-identical functions, each
    // hardcoding its own thresholds instead of reading AchievementEntity.requiredValue. Adding a
    // badge in an existing metric family (e.g. a 30-day streak badge) is now a seeded row, not a
    // new `if` here - see AchievementEntity's MetricType doc.

    private suspend fun checkWordAchievements(wordsLearned: Int) =
        checkAchievements(MetricType.WORDS_LEARNED, wordsLearned)

    private suspend fun checkStoryAchievements(storiesCompleted: Int) =
        checkAchievements(MetricType.STORIES_COMPLETED, storiesCompleted)

    private suspend fun checkGameAchievements(gamesPlayed: Int) =
        checkAchievements(MetricType.GAMES_PLAYED, gamesPlayed)

    /** Not a threshold comparison - a perfect game either just happened or it didn't. */
    suspend fun checkPerfectGameAchievement() =
        checkAchievements(MetricType.PERFECT_GAME, currentValue = 1)

    private suspend fun checkStreakAchievements(streak: Int) =
        checkAchievements(MetricType.STREAK, streak)

    private suspend fun checkLevelAchievements(level: Int) =
        checkAchievements(MetricType.LEVEL, level)

    /**
     * Unlocks every not-yet-unlocked badge in [metricType] whose requiredValue [currentValue]
     * now meets. The single place unlock logic lives, regardless of which metric family it is.
     */
    suspend fun checkAchievements(metricType: String, currentValue: Int) {
        val today = LocalDate.now().toIsoString()
        achievementDao.getLockedByMetricType(metricType)
            .filter { currentValue >= it.requiredValue }
            .forEach { tryUnlock(it.id, today) }
    }

    private suspend fun tryUnlock(id: String, date: String) {
        val achievement = achievementDao.getAchievementById(id)
        if (achievement != null && !achievement.isUnlocked) {
            achievementDao.unlockAchievement(id, date)
            // Award bonus XP for unlocking
            userProgressDao.addXp(achievement.xpReward)
        }
    }

    /**
     * Adds any achievement DatabaseSeeder now defines that an existing install's table (seeded
     * before this badge was added) doesn't have yet. A fresh install already gets every badge
     * through the normal empty-table seed; this is only for upgrading installs, called once from
     * AchievementsViewModel's init so a new badge type appears without the user reinstalling.
     */
    suspend fun seedNewAchievements(all: List<AchievementEntity>) {
        val existingIds = achievementDao.getAllIds().toSet()
        val missing = all.filter { it.id !in existingIds }
        if (missing.isNotEmpty()) achievementDao.insertAll(missing)
    }

    // Achievements
    fun getAllAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getAllAchievements()

    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getUnlockedAchievements()

    fun getUnlockedAchievementCount(): Flow<Int> =
        achievementDao.getUnlockedCount()
}
