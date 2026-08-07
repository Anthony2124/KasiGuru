package com.kasiguru.ui.screens.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameLevelEntity
import com.kasiguru.data.repository.GameLevelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelSelectionViewModel @Inject constructor(
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameType: String = savedStateHandle.get<String>("gameType") ?: "word_match"

    private val _uiState = MutableStateFlow(LevelSelectionUiState(gameType = gameType))
    val uiState: StateFlow<LevelSelectionUiState> = _uiState.asStateFlow()

    init {
        loadLevels()
    }

    private fun loadLevels() {
        viewModelScope.launch {
            val totalStars = gameLevelRepository.getTotalStars()
            _uiState.value = _uiState.value.copy(totalStars = totalStars)

            gameLevelRepository.getLevelsByGame(gameType).collect { levels ->
                _uiState.value = _uiState.value.copy(
                    levels = levels,
                    isLoading = false
                )
            }
        }
    }
}

data class LevelSelectionUiState(
    val gameType: String,
    val totalStars: Int = 0,
    val levels: List<GameLevelEntity> = emptyList(),
    val isLoading: Boolean = true
)
