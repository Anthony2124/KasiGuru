package com.kasiguru.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.AccountState
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.ProgressSyncManager
import com.kasiguru.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val progressSyncManager: ProgressSyncManager
) : ViewModel() {

    val account: StateFlow<AccountState> = authRepository.accountState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentAccount())

    val isDarkMode: StateFlow<Boolean> = userPreferencesRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val soundEnabled: StateFlow<Boolean> = userPreferencesRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val streakReminders: StateFlow<Boolean> = userPreferencesRepository.streakReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wordOfDayReminders: StateFlow<Boolean> = userPreferencesRepository.wordOfDayReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val leaderboardAlerts: StateFlow<Boolean> = userPreferencesRepository.leaderboardAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(enabled)
        }
    }

    fun toggleSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundEnabled(enabled)
        }
    }

    fun toggleStreakReminders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setStreakReminders(enabled)
        }
    }

    fun toggleWordOfDayReminders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setWordOfDayReminders(enabled)
        }
    }

    fun toggleLeaderboardAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setLeaderboardAlerts(enabled)
        }
    }

    /** Pulls the account's cloud progress and merges it into the local database. */
    suspend fun syncNow(): Boolean {
        val uid = authRepository.currentAccount().uid ?: return false
        progressSyncManager.syncFromCloud(uid)
        progressSyncManager.syncLearningStateFromCloud(uid)
        return true
    }

    val securityQuestions: List<String> = AuthRepository.SECURITY_QUESTIONS

    private val _securityAnswers = MutableStateFlow(List(securityQuestions.size) { "" })
    val securityAnswers: StateFlow<List<String>> = _securityAnswers.asStateFlow()

    private val _securityQuestionsStatus = MutableStateFlow<String?>(null)
    val securityQuestionsStatus: StateFlow<String?> = _securityQuestionsStatus.asStateFlow()

    fun loadSecurityQuestions() {
        viewModelScope.launch {
            authRepository.getSecurityQuestionAnswers()
                .onSuccess { _securityAnswers.value = it }
        }
    }

    fun onSecurityAnswerChanged(index: Int, value: String) {
        val current = _securityAnswers.value.toMutableList()
        if (index !in current.indices) return
        current[index] = value
        _securityAnswers.value = current
    }

    fun saveSecurityQuestions() {
        viewModelScope.launch {
            val result = authRepository.saveSecurityQuestions(_securityAnswers.value)
            _securityQuestionsStatus.value = if (result.isSuccess) {
                "Saved. These are a lightweight identity hint, not a secret - anyone who unlocks this device is able to read them."
            } else {
                "Couldn't save right now. Please try again."
            }
        }
    }

    fun dismissSecurityQuestionsStatus() {
        _securityQuestionsStatus.value = null
    }
}
