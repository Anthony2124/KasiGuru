package com.kasiguru.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
}
