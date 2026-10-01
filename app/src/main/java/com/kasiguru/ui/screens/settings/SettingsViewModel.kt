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

    val hapticsEnabled = userPreferencesRepository.hapticsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    fun toggleHapticsEnabled(enabled: Boolean) { viewModelScope.launch { userPreferencesRepository.setHapticsEnabled(enabled) } }

    val soundEnabled: StateFlow<Boolean> = userPreferencesRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val streakReminders: StateFlow<Boolean> = userPreferencesRepository.streakReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wordOfDayReminders: StateFlow<Boolean> = userPreferencesRepository.wordOfDayReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val leaderboardAlerts: StateFlow<Boolean> = userPreferencesRepository.leaderboardAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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
        return progressSyncManager.syncNow(uid)
    }

    val securityQuestions: List<String> = AuthRepository.SECURITY_QUESTIONS

    private val _securityAnswers = MutableStateFlow(List(securityQuestions.size) { "" })
    val securityAnswers: StateFlow<List<String>> = _securityAnswers.asStateFlow()

    /** What is stored on the account, so the screen can tell an edit from what is already saved. */
    private val _savedSecurityAnswers = MutableStateFlow(List(securityQuestions.size) { "" })
    val savedSecurityAnswers: StateFlow<List<String>> = _savedSecurityAnswers.asStateFlow()

    private val _securitySaving = MutableStateFlow(false)
    val securitySaving: StateFlow<Boolean> = _securitySaving.asStateFlow()

    private val _securityQuestionsStatus = MutableStateFlow<String?>(null)
    val securityQuestionsStatus: StateFlow<String?> = _securityQuestionsStatus.asStateFlow()

    /** True when [securityQuestionsStatus] reports a failure rather than a save. */
    private val _securityStatusIsError = MutableStateFlow(false)
    val securityStatusIsError: StateFlow<Boolean> = _securityStatusIsError.asStateFlow()

    fun loadSecurityQuestions() {
        viewModelScope.launch {
            authRepository.getSecurityQuestionAnswers()
                .onSuccess {
                    _securityAnswers.value = it
                    _savedSecurityAnswers.value = it
                }
        }
    }

    fun onSecurityAnswerChanged(index: Int, value: String) {
        val current = _securityAnswers.value.toMutableList()
        if (index !in current.indices) return
        current[index] = value
        _securityAnswers.value = current
        // An edit after a save makes the old "Saved" untrue.
        _securityQuestionsStatus.value = null
    }

    fun saveSecurityQuestions() {
        if (_securitySaving.value) return
        viewModelScope.launch {
            _securitySaving.value = true
            val answers = _securityAnswers.value.map { it.trim() }
            val result = authRepository.saveSecurityQuestions(answers)
            _securitySaving.value = false
            if (result.isSuccess) {
                _securityAnswers.value = answers
                _savedSecurityAnswers.value = answers
                _securityStatusIsError.value = false
                _securityQuestionsStatus.value = "Saved to your account."
            } else {
                _securityStatusIsError.value = true
                _securityQuestionsStatus.value = "Couldn't save right now. Check your connection and try again."
            }
        }
    }

    fun dismissSecurityQuestionsStatus() {
        _securityQuestionsStatus.value = null
    }
}
