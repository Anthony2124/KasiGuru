package com.kasiguru.ui.screens.games.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.util.Constants
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
                    val highScores = listOf(
                        "word_match", "reverse_match", "fill_blank", Constants.Games.RECALL,
                        "aspect_builder", "sentence_order", Constants.Games.WORD_SEARCH, Constants.Games.WORD_WHEEL
                    ).associateWith { gameRepository.getHighScore(it)?.score ?: 0 }
                    _uiState.value = _uiState.value.copy(
                        recentScores = scores,
                        highScores = highScores,
                        isLoading = false
                    )
                }
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
