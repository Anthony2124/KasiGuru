package com.kasiguru.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userProgress: UserProgressEntity? = null,
    val achievements: List<AchievementEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val unlockedCount: Int get() = achievements.count { it.isUnlocked }

    /**
     * Most recently earned first. Achievements seeded but never unlocked carry a null date, so they
     * sort last rather than being mistaken for the newest.
     */
    val recentlyUnlocked: List<AchievementEntity>
        get() = achievements
            .filter { it.isUnlocked }
            .sortedByDescending { it.unlockedDate ?: "" }
            .take(6)

    /**
     * The locked achievement closest to completion, for the empty state. A learner with no badges yet
     * should be told which one is within reach, not shown a blank row.
     */
    val closestLocked: AchievementEntity?
        get() = achievements
            .filter { !it.isUnlocked && it.requiredValue > 0 }
            .maxByOrNull { it.currentValue.toFloat() / it.requiredValue }

    /** Guarded against the zero-questions case a new account is always in. */
    val accuracy: Float
        get() {
            val progress = userProgress ?: return 0f
            if (progress.totalQuestionsAnswered <= 0) return 0f
            return progress.totalCorrectAnswers.toFloat() / progress.totalQuestionsAnswered
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeAchievements()
    }

    /**
     * Achievements come from the repository this view model already holds, so the identity rebuild
     * added no new dependency - the data was simply never asked for.
     */
    private fun observeAchievements() {
        viewModelScope.launch {
            userProgressRepository.getAllAchievements().collect { achievements ->
                _uiState.value = _uiState.value.copy(achievements = achievements)
            }
        }
    }

    private fun loadProfile() {
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

    /**
     * Suspends until the write finishes and reports whether it succeeded, so the caller can wait for
     * a real result before navigating away — previously the screen fired this and navigated back in
     * the same tap, so a failure was captured in [ProfileUiState.error] but never shown to anyone.
     */
    suspend fun updateProfile(fullName: String, age: Int?, address: String, iconId: Int): Boolean {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        return try {
            userProgressRepository.updateProfileDetails(fullName, age, address, iconId)
            _uiState.value = _uiState.value.copy(isSaving = false, error = null)
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isSaving = false, error = "Couldn't save your changes. Check your connection and try again.")
            false
        }
    }
}
