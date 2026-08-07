package com.kasiguru.ui.screens.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.toIsoString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class WordMatchViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelNumber = savedStateHandle.get<Int>("level") ?: 1

    private val _uiState = MutableStateFlow(WordMatchUiState())
    val uiState: StateFlow<WordMatchUiState> = _uiState.asStateFlow()

    private var totalInitialQuestions = 5
    private val questionQueue = mutableListOf<VocabularyEntity>()
    private var questionStartTimeMs: Long = 0L
    private var earnedXpTotal = 0

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = WordMatchUiState(isLoading = true)
            
            val levelInfo = gameLevelRepository.getLevel("word_match", levelNumber)
            if (levelInfo != null) {
                totalInitialQuestions = levelInfo.questionsCount
            }
            
            var words = vocabularyRepository.getRandomWords(totalInitialQuestions)
            if (words.isEmpty()) {
                val all = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
                words = all.shuffled().take(totalInitialQuestions)
            }

            questionQueue.clear()
            questionQueue.addAll(words)
            earnedXpTotal = 0
            
            if (questionQueue.isEmpty()) {
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
        if (state.currentQuestionIndex >= questionQueue.size) {
            endGame()
            return
        }

        val targetWord = questionQueue[state.currentQuestionIndex]
        
        viewModelScope.launch {
            val wrongEntities = vocabularyRepository.getDistractorsForWord(targetWord, 3)
            val wrongOptions = wrongEntities.map { it.tagalog }.filter { it.isNotBlank() }

            val allOptions = (wrongOptions + targetWord.tagalog).distinct().shuffled()
            questionStartTimeMs = System.currentTimeMillis()

            _uiState.value = state.copy(
                isLoading = false,
                currentWord = targetWord,
                options = allOptions,
                selectedOption = null,
                isCorrect = null,
                totalQuestions = totalInitialQuestions
            )
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val targetWord = state.currentWord ?: return
        val isCorrect = option == targetWord.tagalog
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs

        val rating: ReviewRating
        val questionXp: Int

        if (isCorrect) {
            rating = if (responseTimeMs < 1200) ReviewRating.HARD else ReviewRating.GOOD
            questionXp = when {
                rating == ReviewRating.HARD -> 5
                else -> Constants.XP_PER_GAME_CORRECT
            }
        } else {
            rating = ReviewRating.AGAIN
            questionXp = 0
        }

        earnedXpTotal += questionXp
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )

        viewModelScope.launch {
            vocabularyRepository.processWordReview(targetWord, rating)

            delay(1500)
            _uiState.value = _uiState.value.copy(currentQuestionIndex = _uiState.value.currentQuestionIndex + 1)
            loadNextQuestion()
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val isPerfect = state.score >= totalInitialQuestions
        val xpEarned = earnedXpTotal + if (isPerfect) Constants.XP_BONUS_PERFECT_GAME else 0

        viewModelScope.launch {
            val scoreEntity = GameScoreEntity(
                gameType = "word_match",
                score = state.score,
                totalQuestions = totalInitialQuestions,
                xpEarned = xpEarned,
                playedAt = LocalDateTime.now().toIsoString()
            )
            gameRepository.saveScore(scoreEntity)

            val successRate = state.score.toFloat() / totalInitialQuestions
            val starsEarned = when {
                successRate >= 1.0f -> 3
                successRate >= 0.7f -> 2
                successRate >= 0.4f -> 1
                else -> 0
            }
            gameLevelRepository.saveLevelResult("word_match", levelNumber, starsEarned)

            userProgressRepository.addXp(xpEarned)
            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(state.score, totalInitialQuestions)
            
            if (isPerfect) {
                userProgressRepository.checkPerfectGameAchievement()
            }

            _uiState.value = state.copy(
                isGameOver = true,
                finalXp = xpEarned,
                starsEarned = starsEarned,
                totalQuestions = totalInitialQuestions
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
    val finalXp: Int = 0,
    val starsEarned: Int = 0,
    val totalQuestions: Int = 5
)
