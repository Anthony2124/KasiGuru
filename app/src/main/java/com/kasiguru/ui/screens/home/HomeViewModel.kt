package com.kasiguru.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.BuildConfig
import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.data.repository.AppUpdateRepository
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val BACKUP_PROMPT_MIN_XP = 150

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkAndSeedVocabulary()
        loadProgress()
        updateDailyStreak()
        checkForUpdate()
        observeAccountState()
    }

    /**
     * Fetches the latest published release and exposes it only when it is newer
     * than the installed build (BuildConfig.VERSION_CODE, not a hardcoded value).
     * Optional updates the user already dismissed stay hidden until a newer
     * release appears; required updates always show.
     */
    private fun checkForUpdate() {
        viewModelScope.launch {
            val latest = appUpdateRepository.getLatestRelease().getOrNull() ?: return@launch
            if (latest.versionCode <= BuildConfig.VERSION_CODE) return@launch

            val dismissed = userPreferencesRepository.dismissedUpdateVersion.first()
            if (!latest.forceUpdate && latest.versionCode <= dismissed) return@launch

            _uiState.value = _uiState.value.copy(updateRelease = latest)
        }
    }

    fun dismissUpdate() {
        val dismissedVersion = _uiState.value.updateRelease?.versionCode
        _uiState.value = _uiState.value.copy(updateRelease = null)
        if (dismissedVersion != null) {
            viewModelScope.launch {
                userPreferencesRepository.setDismissedUpdateVersion(dismissedVersion)
            }
        }
    }

    fun dismissBackupPrompt() {
        _uiState.value = _uiState.value.copy(showBackupPrompt = false)
        viewModelScope.launch { userPreferencesRepository.setBackupPromptDismissed(true) }
    }

    /**
     * Guest sessions cannot survive an uninstall, so surface a one-time prompt
     * once the user has enough progress to care about losing.
     */
    private fun observeAccountState() {
        viewModelScope.launch {
            combine(
                authRepository.accountState,
                userProgressRepository.getUserProgress(),
                userPreferencesRepository.backupPromptDismissed
            ) { account, progress, dismissed ->
                !dismissed &&
                    account.isSignedIn &&
                    !account.isRecoverable &&
                    (progress?.totalXp ?: 0) >= BACKUP_PROMPT_MIN_XP
            }.collect { show ->
                _uiState.value = _uiState.value.copy(showBackupPrompt = show)
            }
        }
    }

    private fun checkAndSeedVocabulary() {
        viewModelScope.launch {
            if (vocabularyRepository.getTotalCountDirect() == 0) {
                vocabularyRepository.insertAll(DatabaseSeeder.getInitialVocabulary())
            }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            val currentOnce = userProgressRepository.getUserProgressOnce()
            val initialProgress = currentOnce ?: UserProgressEntity().also { defaultProgress ->
                userProgressRepository.initializeProgress(defaultProgress)
            }
            _uiState.value = _uiState.value.copy(
                userProgress = initialProgress,
                isLoading = false
            )

            userProgressRepository.getUserProgress().collect { progress ->
                if (progress != null) {
                    _uiState.value = _uiState.value.copy(
                        userProgress = progress,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun updateDailyStreak() {
        viewModelScope.launch {
            userProgressRepository.updateStreak()
        }
    }
}

data class HomeUiState(
    val userProgress: UserProgressEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val updateRelease: AppReleaseDto? = null,
    val showBackupPrompt: Boolean = false
)
