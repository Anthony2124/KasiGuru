package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.toIsoString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class WordMatchViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordMatchUiState())
    val uiState: StateFlow<WordMatchUiState> = _uiState.asStateFlow()

    private val totalQuestions = 5
    private var currentWords: List<VocabularyEntity> = emptyList()

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = WordMatchUiState(isLoading = true)
            // Get random words for the game
            currentWords = vocabularyRepository.getRandomWords(totalQuestions)
            
            if (currentWords.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isGameOver = true
                )
                return@launch
            }
            
            loadNextQuestion()
        }
    }

    private fun loadNextQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex >= totalQuestions || state.currentQuestionIndex >= currentWords.size) {
            endGame()
            return
        }

        val targetWord = currentWords[state.currentQuestionIndex]
        
        // Generate options (1 correct, 3 wrong)
        viewModelScope.launch {
            val wrongOptions = vocabularyRepository.getRandomWords(10)
                .filter { it.id != targetWord.id && it.tagalog.isNotBlank() }
                .take(3)

            val allOptions = (wrongOptions.map { it.tagalog } + targetWord.tagalog).distinct().shuffled()

            _uiState.value = state.copy(
                isLoading = false,
                currentWord = targetWord,
                options = allOptions,
                selectedOption = null,
                isCorrect = null
            )
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return // Already answered

        val targetWord = state.currentWord ?: return
        val isCorrect = option == targetWord.tagalog
        
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )

        // Wait a moment then load next question
        viewModelScope.launch {
            delay(1500)
            _uiState.value = _uiState.value.copy(currentQuestionIndex = _uiState.value.currentQuestionIndex + 1)
            loadNextQuestion()
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val xpEarned = state.score * Constants.XP_PER_GAME_CORRECT + 
                if (state.score == totalQuestions) Constants.XP_BONUS_PERFECT_GAME else 0

        viewModelScope.launch {
            // Save Score
            val scoreEntity = GameScoreEntity(
                gameType = "word_match",
                score = state.score,
                totalQuestions = totalQuestions,
                xpEarned = xpEarned,
                playedAt = LocalDateTime.now().toIsoString()
            )
            gameRepository.saveScore(scoreEntity)

            // Update Progress
            userProgressRepository.addXp(xpEarned)
            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(state.score, totalQuestions)
            
            if (state.score == totalQuestions) {
                userProgressRepository.checkPerfectGameAchievement()
            }

            _uiState.value = state.copy(
                isGameOver = true,
                finalXp = xpEarned
            )
        }
    }
}

data class WordMatchUiState(
    val isLoading: Boolean = true,
    val currentQuestionIndex: Int = 0,
    val currentWord: VocabularyEntity? = null,
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val finalXp: Int = 0
)
