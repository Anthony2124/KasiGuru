package com.kasiguru.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DailyStreakQuota(
    val reviewCompleted: Boolean = false,
    val gamesPlayed: Int = 0,
    val requiredGames: Int = 3
) {
    val isQuotaMet: Boolean get() = reviewCompleted && gamesPlayed >= requiredGames
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val STREAK_REMINDERS = booleanPreferencesKey("streak_reminders")
        val WORD_OF_DAY_REMINDERS = booleanPreferencesKey("word_of_day_reminders")
        val LEADERBOARD_ALERTS = booleanPreferencesKey("leaderboard_alerts")
        val DISMISSED_UPDATE_VERSION = intPreferencesKey("dismissed_update_version")
        val BACKUP_PROMPT_DISMISSED = booleanPreferencesKey("backup_prompt_dismissed")
        val GAME_RULES_SEEN = stringSetPreferencesKey("game_rules_seen")
        val TUTORIAL_PENDING = booleanPreferencesKey("tutorial_pending")
        val TOUR_COMPLETED_CHAPTERS = stringSetPreferencesKey("tour_completed_chapters")
        val TOUR_SKIPPED_CHAPTERS = stringSetPreferencesKey("tour_skipped_chapters")
        val TOUR_RESUME_CHAPTER = stringPreferencesKey("tour_resume_chapter")
        val TOUR_RESUME_STEP = intPreferencesKey("tour_resume_step")
        val TOUR_BASELINE_VERSION = intPreferencesKey("tour_baseline_version")
        val LAST_CONTENT_SYNC_AT = longPreferencesKey("last_content_sync_at")
        val LAST_FULL_RECONCILE_AT = longPreferencesKey("last_full_reconcile_at")
        val DAILY_REVIEW_COMPLETED_DATE = stringPreferencesKey("daily_review_completed_date")
        val DAILY_GAMES_DATE = stringPreferencesKey("daily_games_date")
        val DAILY_GAMES_COUNT = intPreferencesKey("daily_games_count")
    }

    /**
     * When the dictionary/stories pull last completed, as epoch millis. 0 means never.
     *
     * This exists to cap Firestore reads. [com.kasiguru.data.remote.FirestoreSyncManager]
     * pulls the whole `vocabulary` and `stories` collections, and it used to do that on
     * every single launch — roughly 400 document reads a time against the Spark plan's
     * 50,000/day project-wide ceiling, i.e. the entire quota for all users combined
     * exhausted by ~125 app opens. Reading this timestamp costs nothing and skips the
     * pull when the local copy is recent enough.
     */
    val lastContentSyncAt: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LAST_CONTENT_SYNC_AT] ?: 0L
    }

    suspend fun lastContentSyncAtOnce(): Long = lastContentSyncAt.first()

    suspend fun setLastContentSyncAt(epochMillis: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_CONTENT_SYNC_AT] = epochMillis
        }
    }

    /**
     * When the last *full* collection read completed, as epoch millis. 0 means never.
     *
     * Tracked separately from [lastContentSyncAt] because the two run on different
     * cadences: the ordinary sync is incremental and cheap, while this one re-reads
     * everything to catch documents the incremental query cannot see (see
     * FirestoreSyncManager.FULL_RECONCILE_INTERVAL_MS).
     */
    val lastFullReconcileAt: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LAST_FULL_RECONCILE_AT] ?: 0L
    }

    suspend fun lastFullReconcileAtOnce(): Long = lastFullReconcileAt.first()

    suspend fun setLastFullReconcileAt(epochMillis: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_FULL_RECONCILE_AT] = epochMillis
        }
    }

    /**
     * Mini-game types whose rules dialog the learner opted out of re-seeing. Before this existed,
     * [com.kasiguru.ui.screens.games.GameRulesDialog] reappeared in full on every single entry to a
     * game, even the 50th time.
     */
    val gameRulesSeen: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.GAME_RULES_SEEN] ?: emptySet()
    }

    suspend fun markGameRulesSeen(gameType: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.GAME_RULES_SEEN] = (prefs[PreferencesKeys.GAME_RULES_SEEN] ?: emptySet()) + gameType
        }
    }

    /**
     * Whether the learner is owed the guided tour of the interface.
     *
     * Deliberately "pending" rather than "seen". A `seen` flag defaults to false for everyone, so
     * shipping one would have ambushed every *existing* install with a tour on the first launch after
     * updating — a fresh preference key is indistinguishable from a learner who never took the tour.
     * This is set by [com.kasiguru.ui.screens.onboarding.OnboardingViewModel] when the wizard
     * finishes, and cleared when the tour is completed or skipped, so only someone who actually just
     * joined is owed one.
     *
     * It needs no entry in [clearUserSessionData]: signing out runs
     * [com.kasiguru.data.UserDataResetManager.resetAllLocalUserData], which reseeds `user_progress`
     * with `isOnboardingCompleted = false`, so the next person on the device goes through the wizard
     * again and is marked pending on the way out.
     */
    val tutorialPending: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TUTORIAL_PENDING] ?: false
    }

    suspend fun setTutorialPending(pending: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TUTORIAL_PENDING] = pending
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chaptered tutorial.
    //
    // [tutorialPending] above still means exactly one thing: this learner is owed the *core* chapter.
    // Every other chapter is opt-in from the help page, which is what keeps an app update from
    // ambushing anyone - a new chapter is offered, never started.
    //
    // Strings in, strings out. The "Dictionary:2" encoding and the rules about what counts as new
    // live in ui/tour/TourProgress.kt, so this repository keeps knowing nothing about the UI layer.
    // ─────────────────────────────────────────────────────────────────────────

    /** Stamps of chapters finished, as `"<ChapterId>:<version>"`. */
    val tourCompletedChapters: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TOUR_COMPLETED_CHAPTERS] ?: emptySet()
    }

    /** Chapter ids the learner started and skipped. Offered again, but never badged. */
    val tourSkippedChapters: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TOUR_SKIPPED_CHAPTERS] ?: emptySet()
    }

    /** Where a chapter was left, as a raw id and step for [com.kasiguru.ui.tour.sanitizeResume]. */
    val tourResumePoint: Flow<Pair<String, Int>?> = dataStore.data.map { prefs ->
        val chapter = prefs[PreferencesKeys.TOUR_RESUME_CHAPTER]
        val step = prefs[PreferencesKeys.TOUR_RESUME_STEP]
        if (chapter.isNullOrBlank() || step == null) null else chapter to step
    }

    /**
     * The highest chapter version that already existed when this install first ran.
     *
     * Written once. Without it, an existing learner updating from a build that had no chapters would
     * see every chapter badged as new at once, which is noise rather than guidance.
     */
    val tourBaselineVersion: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TOUR_BASELINE_VERSION] ?: 0
    }

    suspend fun markTourChapterCompleted(stamp: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TOUR_COMPLETED_CHAPTERS] =
                (prefs[PreferencesKeys.TOUR_COMPLETED_CHAPTERS] ?: emptySet()) + stamp
            prefs.remove(PreferencesKeys.TOUR_RESUME_CHAPTER)
            prefs.remove(PreferencesKeys.TOUR_RESUME_STEP)
        }
    }

    suspend fun markTourChapterSkipped(chapterId: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TOUR_SKIPPED_CHAPTERS] =
                (prefs[PreferencesKeys.TOUR_SKIPPED_CHAPTERS] ?: emptySet()) + chapterId
            prefs.remove(PreferencesKeys.TOUR_RESUME_CHAPTER)
            prefs.remove(PreferencesKeys.TOUR_RESUME_STEP)
        }
    }

    /**
     * Checkpoints the in-flight position.
     *
     * Deliberately not called on every Next: the position lives in the view model for responsiveness
     * and is flushed when the app is backgrounded and on a short debounce, so a ten-stop chapter
     * costs one or two writes rather than ten.
     */
    suspend fun setTourResumePoint(chapterId: String, step: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TOUR_RESUME_CHAPTER] = chapterId
            prefs[PreferencesKeys.TOUR_RESUME_STEP] = step
        }
    }

    suspend fun clearTourResumePoint() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.TOUR_RESUME_CHAPTER)
            prefs.remove(PreferencesKeys.TOUR_RESUME_STEP)
        }
    }

    /** Write-once. A second call on an install that already has a baseline is a no-op. */
    suspend fun ensureTourBaseline(version: Int) {
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.TOUR_BASELINE_VERSION] == null) {
                prefs[PreferencesKeys.TOUR_BASELINE_VERSION] = version
            }
        }
    }

    /**
     * Highest versionCode the user dismissed an optional update banner for, so
     * "Later" survives an app restart. A newer release still shows.
     */
    val dismissedUpdateVersion: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DISMISSED_UPDATE_VERSION] ?: 0
    }

    suspend fun setDismissedUpdateVersion(versionCode: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.DISMISSED_UPDATE_VERSION] = versionCode
        }
    }

    /** Whether the "secure your progress" prompt for guest accounts was dismissed. */
    val backupPromptDismissed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BACKUP_PROMPT_DISMISSED] ?: false
    }

    suspend fun setBackupPromptDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.BACKUP_PROMPT_DISMISSED] = dismissed
        }
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_DARK_MODE] = enabled
        }
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SOUND_ENABLED] ?: true
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    val streakReminders: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.STREAK_REMINDERS] ?: true
    }

    suspend fun setStreakReminders(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.STREAK_REMINDERS] = enabled
        }
    }

    val wordOfDayReminders: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.WORD_OF_DAY_REMINDERS] ?: true
    }

    suspend fun setWordOfDayReminders(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.WORD_OF_DAY_REMINDERS] = enabled
        }
    }

    val leaderboardAlerts: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LEADERBOARD_ALERTS] ?: true
    }

    suspend fun setLeaderboardAlerts(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LEADERBOARD_ALERTS] = enabled
        }
    }

    fun getDailyStreakQuota(today: String): Flow<DailyStreakQuota> = dataStore.data.map { prefs ->
        val reviewDate = prefs[PreferencesKeys.DAILY_REVIEW_COMPLETED_DATE].orEmpty()
        val gamesDate = prefs[PreferencesKeys.DAILY_GAMES_DATE].orEmpty()
        val gamesCount = if (gamesDate == today) prefs[PreferencesKeys.DAILY_GAMES_COUNT] ?: 0 else 0
        DailyStreakQuota(
            reviewCompleted = reviewDate == today,
            gamesPlayed = gamesCount,
            requiredGames = 3
        )
    }

    suspend fun getDailyStreakQuotaOnce(today: String): DailyStreakQuota =
        getDailyStreakQuota(today).first()

    suspend fun recordDailyReviewCompleted(today: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_REVIEW_COMPLETED_DATE] = today
        }
    }

    suspend fun recordDailyGamePlayed(today: String): Int {
        var newCount = 1
        dataStore.edit { prefs ->
            val gamesDate = prefs[PreferencesKeys.DAILY_GAMES_DATE].orEmpty()
            val currentCount = if (gamesDate == today) prefs[PreferencesKeys.DAILY_GAMES_COUNT] ?: 0 else 0
            newCount = currentCount + 1
            prefs[PreferencesKeys.DAILY_GAMES_DATE] = today
            prefs[PreferencesKeys.DAILY_GAMES_COUNT] = newCount
        }
        return newCount
    }

    suspend fun resetDailyStreakQuota() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.DAILY_REVIEW_COMPLETED_DATE)
            prefs.remove(PreferencesKeys.DAILY_GAMES_DATE)
            prefs.remove(PreferencesKeys.DAILY_GAMES_COUNT)
        }
    }

    suspend fun clearUserSessionData() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.DAILY_REVIEW_COMPLETED_DATE)
            prefs.remove(PreferencesKeys.DAILY_GAMES_DATE)
            prefs.remove(PreferencesKeys.DAILY_GAMES_COUNT)
            prefs.remove(PreferencesKeys.BACKUP_PROMPT_DISMISSED)
            prefs.remove(PreferencesKeys.GAME_RULES_SEEN)
            // Chapter progress belongs to the person, not the device. Sign-out reseeds user_progress
            // so the next learner runs the wizard and takes the core tour again; without these four
            // removals they would inherit the previous person's history and be told they had already
            // done Dictionary. TOUR_BASELINE_VERSION deliberately stays - it is a property of the
            // install, and clearing it would re-badge every chapter as new for the next profile.
            prefs.remove(PreferencesKeys.TOUR_COMPLETED_CHAPTERS)
            prefs.remove(PreferencesKeys.TOUR_SKIPPED_CHAPTERS)
            prefs.remove(PreferencesKeys.TOUR_RESUME_CHAPTER)
            prefs.remove(PreferencesKeys.TOUR_RESUME_STEP)
        }
    }
}
