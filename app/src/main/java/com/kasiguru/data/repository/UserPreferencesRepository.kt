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
}
