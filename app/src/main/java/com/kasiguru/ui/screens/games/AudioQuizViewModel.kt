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
class AudioQuizViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioQuizUiState())
    val uiState: StateFlow<AudioQuizUiState> = _uiState.asStateFlow()

    private val totalQuestions = 5
    private var currentWords: List<VocabularyEntity> = emptyList()

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = AudioQuizUiState(isLoading = true)
            // For MVP, randomly select words. In prod, only select words with actual audio files.
            currentWords = vocabularyRepository.getRandomWords(totalQuestions)
            
            if (currentWords.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false, isGameOver = true)
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
        
        viewModelScope.launch {
            val wrongOptions = vocabularyRepository.getRandomWords(10)
                .filter { it.id != targetWord.id && it.kasiguranin.isNotBlank() }
                .take(3)

            val allOptions = (wrongOptions.map { it.kasiguranin } + targetWord.kasiguranin).distinct().shuffled()

            _uiState.value = state.copy(
                isLoading = false,
                currentWord = targetWord,
                options = allOptions,
                selectedOption = null,
                isCorrect = null,
                isPlayingAudio = true // Simulating auto-play on load
            )
            
            // Simulate audio playback finishing
            delay(1500)
            _uiState.value = _uiState.value.copy(isPlayingAudio = false)
        }
    }

    fun playAudio() {
        _uiState.value = _uiState.value.copy(isPlayingAudio = true)
        viewModelScope.launch {
            // Actual ExoPlayer playback would happen here
            delay(1500)
            _uiState.value = _uiState.value.copy(isPlayingAudio = false)
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val targetWord = state.currentWord ?: return
        val isCorrect = option == targetWord.kasiguranin
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )

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
            val scoreEntity = GameScoreEntity(
                gameType = "audio_quiz",
                score = state.score,
                totalQuestions = totalQuestions,
                xpEarned = xpEarned,
                playedAt = LocalDateTime.now().toIsoString()
            )
            gameRepository.saveScore(scoreEntity)

            userProgressRepository.addXp(xpEarned)
            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(state.score, totalQuestions)
            
            if (state.score == totalQuestions) {
                userProgressRepository.checkPerfectGameAchievement()
            }

            _uiState.value = state.copy(isGameOver = true, finalXp = xpEarned)
        }
    }
}

data class AudioQuizUiState(
    val isLoading: Boolean = true,
    val currentQuestionIndex: Int = 0,
    val currentWord: VocabularyEntity? = null,
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isPlayingAudio: Boolean = false,
    val isGameOver: Boolean = false,
    val finalXp: Int = 0
)
