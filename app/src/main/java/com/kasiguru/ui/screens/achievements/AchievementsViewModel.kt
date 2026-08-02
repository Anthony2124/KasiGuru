package com.kasiguru.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            launch {
                userProgressRepository.getAllAchievements().collect { achievements ->
                    _uiState.value = _uiState.value.copy(
                        achievements = achievements,
                        isLoading = false
                    )
                }
            }
            launch {
                userProgressRepository.getUnlockedAchievementCount().collect { count ->
                    _uiState.value = _uiState.value.copy(unlockedCount = count)
                }
            }
        }
    }
}

data class AchievementsUiState(
    val achievements: List<AchievementEntity> = emptyList(),
    val unlockedCount: Int = 0,
    val isLoading: Boolean = true
)
