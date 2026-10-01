package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.AchievementDao
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.util.LearningAnalytics
import com.kasiguru.util.toIsoString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val achievementDao: AchievementDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gamification: GamificationRepository
) {
    // A level-up is a screen-agnostic celebratory moment (LevelUpDialog): XP is earned from lessons,
    // flashcards and all eight mini-games, so the event lives here at the one place that already
    // detects a level change, rather than being duplicated at every XP-awarding call site.
    private val _levelUpEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val levelUpEvents: SharedFlow<Int> = _levelUpEvents.asSharedFlow()

    private val _streakActivatedEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val streakActivatedEvents: SharedFlow<Int> = _streakActivatedEvents.asSharedFlow()

    fun getUserProgress(): Flow<UserProgressEntity?> = kotlinx.coroutines.flow.flow {
        gamification.ensureNormalized()
        userProgressDao.getUserProgress().collect { emit(it) }
    }

    suspend fun getUserProgressOnce(): UserProgressEntity? {
        gamification.ensureNormalized()
        return userProgressDao.getUserProgressOnce()
    }

    fun getDailyStreakQuota(today: String): Flow<DailyStreakQuota> =
        userProgressDao.getUserProgress().map { progress ->
            if (progress == null) DailyStreakQuota()
            else DailyStreakQuota(
                reviewCompleted = progress.dailyReviewCompletedDate == today,
                gamesPlayed = if (progress.dailyGamesDate == today) progress.dailyGamesPlayedCount else 0,
                requiredGames = 3
            )
        }

    suspend fun getDailyStreakQuotaOnce(today: String): DailyStreakQuota {
        val progress = userProgressDao.getUserProgressOnce() ?: return DailyStreakQuota()
        return DailyStreakQuota(
            reviewCompleted = progress.dailyReviewCompletedDate == today,
            gamesPlayed = if (progress.dailyGamesDate == today) progress.dailyGamesPlayedCount else 0,
            requiredGames = 3
        )
    }

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

    suspend fun updateBackground(id: String) {
        val background = com.kasiguru.domain.gamification.ProfileBackgroundCatalog.find(id) ?: return
        val progress = getUserProgressOnce() ?: return
        require(background.isUnlocked(progress.level, progress.longestStreak)) { "This background is still locked." }
        userProgressDao.updateBackground(background.id, System.currentTimeMillis())
    }

    suspend fun updateProfileDetails(fullName: String, age: Int?, address: String, iconId: Int) =
        userProgressDao.updateProfileDetails(fullName, age, address, iconId)

    /**
     * The one-time "About you" details asked before sign-in. Only the personal fields change —
     * userName (the public leaderboard name) and the avatar stay as the learner set them. The row
     * is created first when missing, for the same reason as [completeOnboarding]: someone can
     * reach sign-in from the wizard's first step, before the row exists.
     */
    suspend fun savePersonalDetails(fullName: String, age: Int, address: String) {
        if (userProgressDao.getUserProgressOnce() == null) {
            userProgressDao.insertOrUpdate(UserProgressEntity())
        }
        userProgressDao.updatePersonalDetails(fullName, age, address)
    }

    /** Personal onboarding preferences grant no XP or practice-day streak. */
    suspend fun completeOnboarding(userName: String, avatarId: Int, dailyGoalXp: Int, titleBadge: String) {
        gamification.ensureNormalized()
        if (userProgressDao.getUserProgressOnce() == null) {
            userProgressDao.insertOrUpdate(UserProgressEntity())
        }
        userProgressDao.completeOnboarding(userName, avatarId, dailyGoalXp, titleBadge)
    }

    private suspend fun award(action: suspend () -> RewardOutcome): Int {
        val oldLevel = getUserProgressOnce()?.level ?: 1
        val result = action()
        if(result.level > oldLevel) {
            LearningAnalytics.levelReached(result.level)
            _levelUpEvents.tryEmit(result.level)
        }
        return result.activityXp
    }

    suspend fun awardLesson(unit: String, index: Int, accuracy: Float): Int =
        award { gamification.lesson(unit,index,accuracy) }
    suspend fun awardGame(mode: String, key: String, number: Int, correct: Int, total: Int, stars: Int, perfect: Boolean): Int =
        award { gamification.game(mode,key,number,correct,total,stars,perfect) }
    suspend fun awardStory(id: Int): Int = award { gamification.story(id) }
    suspend fun recordWordReview(id: Int, rating: String, due: Boolean, mastered: Boolean,
        updatedWord: com.kasiguru.data.local.entity.VocabularyEntity? = null): Int =
        award { gamification.review(id,rating,due,mastered,updatedWord) }
    suspend fun recordApprovals(contributions: List<ApprovedContribution>): Int =
        award { gamification.approved(contributions.map { it.id },contributions.associate { it.id to it.approvedDay }) }

    suspend fun incrementGamesPlayed() {
        userProgressDao.incrementGamesPlayed()
        val today = LocalDate.now().toIsoString()
        userProgressDao.recordDailyGamePlayed(today)
        userPreferencesRepository.recordDailyGamePlayed(today)
        checkStreakQuotaAndAdvance()
    }

    suspend fun recordDailyReviewCompleted() {
        val today = LocalDate.now().toIsoString()
        userProgressDao.recordDailyReviewCompleted(today)
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
        val quota = getDailyStreakQuotaOnce(today)
        if (quota.isQuotaMet) {
            updateStreak()
        }
    }

    /**
     * Checks if the user missed yesterday (or earlier) and resets the streak to 0 if so.
     * Safe and idempotent to call on app startup and whenever progress is observed.
     */
    suspend fun validateAndResetExpiredStreak() {
        val progress = userProgressDao.getUserProgressOnce() ?: return
        if (progress.currentStreak > 0 && progress.lastActiveDate.isNotEmpty()) {
            val lastDate = runCatching { LocalDate.parse(progress.lastActiveDate) }.getOrNull()
            if (lastDate != null) {
                val daysBetween = ChronoUnit.DAYS.between(lastDate, LocalDate.now())
                if (daysBetween > 1) {
                    userProgressDao.resetStreak()
                }
            }
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
        gamification.updateMetrics()
        _streakActivatedEvents.tryEmit(newStreak)
    }

    suspend fun updateUserName(name: String) =
        userProgressDao.updateUserName(name)

    /** Called on every successful word/story/poem submission, approved or not. */
    suspend fun incrementSubmissionsMade() {
        userProgressDao.incrementSubmissionsMade()
    }

    // Achievements
    fun getAllAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getAllAchievements().map { rows -> rows.filter { it.id.startsWith(com.kasiguru.domain.gamification.BadgeCatalog.PREFIX) } }

    fun getLegacyAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getAllAchievements().map { rows -> rows.filter { !it.id.startsWith(com.kasiguru.domain.gamification.BadgeCatalog.PREFIX) && it.isUnlocked } }

    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> =
        getAllAchievements().map { it.filter { row -> row.isUnlocked } }

    fun getUnlockedAchievementCount(): Flow<Int> =
        getUnlockedAchievements().map { it.size }
}
