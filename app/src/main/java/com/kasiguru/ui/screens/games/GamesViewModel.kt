package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.UserPreferencesRepository
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
    private val gameLevelRepository: GameLevelRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun markRulesSeen(gameType: String) {
        viewModelScope.launch { userPreferencesRepository.markGameRulesSeen(gameType) }
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                gameLevelRepository.getTotalStarsFlow().collect { stars ->
                    _uiState.value = _uiState.value.copy(totalStars = stars)
                }
            }
            launch {
                userPreferencesRepository.gameRulesSeen.collect { seen ->
                    _uiState.value = _uiState.value.copy(seenGameRules = seen)
                }
            }
            launch {
                userProgressRepository.getUserProgress().collect { progress ->
                    val accuracy = userProgressRepository.getRollingAccuracyRate()
                    _uiState.value = _uiState.value.copy(
                        userProgress = progress,
                        accuracyRate = accuracy
                    )
                }
            }
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
                val rmHigh = gameRepository.getHighScore("reverse_match")
                val fbHigh = gameRepository.getHighScore("fill_blank")
                val aqHigh = gameRepository.getHighScore("audio_quiz")
                val abHigh = gameRepository.getHighScore("aspect_builder")
                val soHigh = gameRepository.getHighScore("sentence_order")
                
                _uiState.value = _uiState.value.copy(
                    highScores = mapOf(
                        "word_match" to (wmHigh?.score ?: 0),
                        "reverse_match" to (rmHigh?.score ?: 0),
                        "fill_blank" to (fbHigh?.score ?: 0),
                        "audio_quiz" to (aqHigh?.score ?: 0),
                        "aspect_builder" to (abHigh?.score ?: 0),
                        "sentence_order" to (soHigh?.score ?: 0)
                    )
                )
            }
        }
    }
}

data class GamesUiState(
    val userProgress: UserProgressEntity? = null,
    val accuracyRate: Float = 1.0f,
    val recentScores: List<GameScoreEntity> = emptyList(),
    val highScores: Map<String, Int> = emptyMap(),
    val totalStars: Int = 0,
    val isLoading: Boolean = true,
    val seenGameRules: Set<String> = emptySet()
)
