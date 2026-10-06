package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.AchievementDao
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.domain.gamification.StreakRules
import com.kasiguru.util.LearningAnalytics
import com.kasiguru.util.toIsoString
import com.kasiguru.util.todayFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val achievementDao: AchievementDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gamification: GamificationRepository,
    private val encounters: WordEncounterRepository,
    // The DAO rather than VocabularyRepository, which already depends on this class.
    private val vocabularyDao: VocabularyDao
) {
    /**
     * A game and a review can finish at the same moment. Both would read `lastActiveDate` before
     * either wrote it, and the learner saw the streak celebration twice.
     */
    private val streakLock = Mutex()

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

    /** Today's quota, kept current — including across midnight, when it starts again empty. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDailyStreakQuota(): Flow<DailyStreakQuota> =
        todayFlow().flatMapLatest { date ->
            val today = date.toIsoString()
            combine(
                userProgressDao.getUserProgress(),
                vocabularyDao.observeScheduledDueCount(today)
            ) { progress, due -> quotaFor(progress, today, due) }
        }

    suspend fun getDailyStreakQuotaOnce(today: String): DailyStreakQuota =
        quotaFor(userProgressDao.getUserProgressOnce(), today, vocabularyDao.countScheduledDueWords(today))

    /** Words scheduled for review on or before [today], for the reminder to name. */
    suspend fun dueReviewCount(today: String): Int = vocabularyDao.countScheduledDueWords(today)

    private fun quotaFor(progress: UserProgressEntity?, today: String, dueCount: Int): DailyStreakQuota =
        DailyStreakQuota(
            reviewCompleted = progress?.dailyReviewCompletedDate == today,
            gamesPlayed = if (progress != null && progress.dailyGamesDate == today) progress.dailyGamesPlayedCount else 0,
            requiredGames = REQUIRED_GAMES,
            reviewDue = dueCount > 0
        )

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

    /** @param wordIds the words the lesson showed, for the Library's My words list. */
    suspend fun awardLesson(unit: String, index: Int, accuracy: Float, wordIds: List<Int> = emptyList()): Int {
        encounters.record(wordIds, "lesson")
        return award { gamification.lesson(unit,index,accuracy) }
    }
    /** @param wordIds the words the round showed, recorded as met in [mode] for My words. */
    suspend fun awardGame(mode: String, key: String, number: Int, correct: Int, total: Int, stars: Int, perfect: Boolean,
        wordIds: List<Int> = emptyList()): Int {
        encounters.record(wordIds, mode)
        return award { gamification.game(mode,key,number,correct,total,stars,perfect) }
    }
    suspend fun awardStory(id: Int): Int = award { gamification.story(id) }
    suspend fun recordWordReview(id: Int, rating: String, due: Boolean, mastered: Boolean,
        updatedWord: com.kasiguru.data.local.entity.VocabularyEntity? = null): Int {
        encounters.record(listOf(id), "review")
        return award { gamification.review(id,rating,due,mastered,updatedWord) }
    }
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
     * Checks if today's daily streak quota (finish the due review, or have none due, and play 3 mini
     * game levels) is met, advancing the streak only when all requirements are satisfied.
     */
    suspend fun checkStreakQuotaAndAdvance() {
        streakLock.withLock {
            val today = LocalDate.now().toIsoString()
            if (getDailyStreakQuotaOnce(today).isQuotaMet) updateStreak()
        }
    }

    /**
     * Checks if the user missed yesterday (or earlier) and resets the streak to 0 if so.
     * Safe and idempotent to call on app startup and whenever progress is observed.
     */
    suspend fun validateAndResetExpiredStreak() {
        val progress = userProgressDao.getUserProgressOnce() ?: return
        if (StreakRules.isExpired(progress.currentStreak, progress.lastActiveDate, LocalDate.now())) {
            userProgressDao.resetStreak()
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

        val newStreak = StreakRules.advancedStreak(progress.currentStreak, progress.lastActiveDate, LocalDate.now())

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

    companion object {
        /** Mini-game levels a day needs, beside the review, to count toward the streak. */
        const val REQUIRED_GAMES = 3
    }
}
