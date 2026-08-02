package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                gameRepository.getRecentScores(5).collect { scores ->
                    _uiState.value = _uiState.value.copy(
                        recentScores = scores,
                        isLoading = false
                    )
                }
            }
            launch {
                val wmHigh = gameRepository.getHighScore("word_match")
                val fbHigh = gameRepository.getHighScore("fill_blank")
                val aqHigh = gameRepository.getHighScore("audio_quiz")
                
                _uiState.value = _uiState.value.copy(
                    highScores = mapOf(
                        "word_match" to (wmHigh?.score ?: 0),
                        "fill_blank" to (fbHigh?.score ?: 0),
                        "audio_quiz" to (aqHigh?.score ?: 0)
                    )
                )
            }
        }
    }
}

data class GamesUiState(
    val recentScores: List<GameScoreEntity> = emptyList(),
    val highScores: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true
)
