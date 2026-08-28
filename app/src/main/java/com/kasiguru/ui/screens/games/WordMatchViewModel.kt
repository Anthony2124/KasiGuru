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
import com.kasiguru.util.srs.ReviewRatingMapper
import com.kasiguru.util.toIsoString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

import com.kasiguru.ui.components.GameReviewItem

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
    private val reviewItems = mutableListOf<GameReviewItem>()

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
            
            var words = vocabularyRepository.getPracticeWords(totalInitialQuestions)
            if (words.isEmpty()) {
                val all = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
                words = all.sortedBy { it.timesReviewed }.take(totalInitialQuestions).shuffled()
            }

            questionQueue.clear()
            questionQueue.addAll(words)
            earnedXpTotal = 0
            reviewItems.clear()

            if (questionQueue.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUnavailable = true
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
                hintRevealed = false,
                totalQuestions = totalInitialQuestions
            )
        }
    }

    /** The learner asked for the definition. Costs the speed bonus; see [selectOption]. */
    fun revealHint() {
        if (_uiState.value.selectedOption != null) return
        _uiState.value = _uiState.value.copy(hintRevealed = true)
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val targetWord = state.currentWord ?: return
        val isCorrect = option == targetWord.tagalog
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs

        // A hinted answer is graded HARD however fast it came back. That forfeits the speed
        // bonus and, more importantly, keeps the SM-2 signal honest: recall that needed the
        // definition shown is not the same evidence of memory as recall that did not.
        val rating = if (state.hintRevealed) {
            ReviewRating.HARD
        } else {
            ReviewRatingMapper.ratingForAnswer(isCorrect, responseTimeMs)
        }
        val questionXp = if (isCorrect) {
            if (rating == ReviewRating.HARD) 5 else Constants.XP_PER_GAME_CORRECT
        } else {
            0
        }

        earnedXpTotal += questionXp
        val newScore = if (isCorrect) state.score + 1 else state.score

        reviewItems.add(
            GameReviewItem(
                prompt = targetWord.kasiguranin,
                userAnswer = option,
                correctAnswer = targetWord.tagalog,
                isCorrect = isCorrect,
                subPrompt = if (targetWord.english.isNotBlank()) targetWord.english else null
            )
        )

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )

        viewModelScope.launch {
            vocabularyRepository.processWordReview(targetWord, rating)
        }
    }

    /** Called from a tap on "Continue," not a timer — the learner reads the feedback at their pace. */
    fun nextQuestion() {
        _uiState.value = _uiState.value.copy(currentQuestionIndex = _uiState.value.currentQuestionIndex + 1)
        loadNextQuestion()
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

            val nextLevel = if (levelNumber < 30 && (starsEarned >= 1 || gameLevelRepository.getLevel("word_match", levelNumber + 1)?.isUnlocked == true)) {
                levelNumber + 1
            } else null

            _uiState.value = state.copy(
                isGameOver = true,
                finalXp = xpEarned,
                starsEarned = starsEarned,
                totalQuestions = totalInitialQuestions,
                reviewItems = reviewItems.toList(),
                nextLevel = nextLevel
            )
        }
    }
}

data class WordMatchUiState(
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val currentWord: VocabularyEntity? = null,
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val hintRevealed: Boolean = false,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val finalXp: Int = 0,
    val starsEarned: Int = 0,
    val totalQuestions: Int = 5,
    val reviewItems: List<GameReviewItem> = emptyList(),
    val nextLevel: Int? = null
)
