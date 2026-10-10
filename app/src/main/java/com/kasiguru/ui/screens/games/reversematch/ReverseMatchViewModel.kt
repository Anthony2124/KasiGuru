package com.kasiguru.ui.screens.games.reversematch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.SavedGameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.games.SavedRound
import com.kasiguru.ui.screens.games.shared.toReviewItem
import com.kasiguru.ui.screens.games.shared.toSavedAnswer
import com.kasiguru.ui.screens.games.shared.wordsOf
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

/**
 * Reverse Match: the learner sees the Tagalog meaning and must pick the
 * Kasiguranin word — the opposite direction of Word Match, which forces
 * recall of the language itself rather than recognition of its meaning.
 */
import com.kasiguru.ui.components.GameReviewItem

@HiltViewModel
class ReverseMatchViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    private val savedGames: SavedGameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** The level being played; Start over replays it. */
    val levelNumber = savedStateHandle.get<Int>("level") ?: 1

    private var usedHint = false
    private var finishing = false
    private val _uiState = MutableStateFlow(ReverseMatchUiState())
    val uiState: StateFlow<ReverseMatchUiState> = _uiState.asStateFlow()

    private var totalInitialQuestions = 5
    private val questionQueue = mutableListOf<VocabularyEntity>()
    /** Every word this round has asked, for the Library's My words list. */
    private val metWordIds = linkedSetOf<Int>()
    private var questionStartTimeMs: Long = 0L
    private val reviewItems = mutableListOf<GameReviewItem>()

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = ReverseMatchUiState(isLoading = true)

            val levelInfo = gameLevelRepository.getLevel("reverse_match", levelNumber)
            if (levelInfo != null) {
                totalInitialQuestions = levelInfo.questionsCount
            }

            val saved = savedGames.load("reverse_match", levelNumber) as? SavedRound
            val savedWords = saved?.let { vocabularyRepository.wordsOf(it) }
            if (saved != null && savedWords != null) {
                questionQueue.clear()
                questionQueue.addAll(savedWords)
                totalInitialQuestions = saved.totalQuestions
                reviewItems.clear()
                reviewItems.addAll(saved.answers.map { it.toReviewItem() })
                metWordIds += savedWords.take(saved.nextIndex).map { it.id }
                usedHint = saved.usedHint
                _uiState.value = _uiState.value.copy(currentQuestionIndex = saved.nextIndex, score = saved.score)
                loadNextQuestion(hintRevealed = saved.hintRevealed)
                return@launch
            }

            var words = vocabularyRepository.getPracticeWords(totalInitialQuestions)
            if (words.isEmpty()) {
                val all = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
                words = all.sortedBy { it.timesReviewed }.take(totalInitialQuestions).shuffled()
            }

            questionQueue.clear()
            questionQueue.addAll(words)
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

    private fun loadNextQuestion(hintRevealed: Boolean = false) {
        val state = _uiState.value
        if (state.currentQuestionIndex >= questionQueue.size) {
            endGame()
            return
        }

        val targetWord = questionQueue[state.currentQuestionIndex]

        viewModelScope.launch {
            val wrongEntities = vocabularyRepository.getDistractorsForWord(targetWord, 3)
            val wrongOptions = wrongEntities.map { it.kasiguranin }.filter { it.isNotBlank() }

            val allOptions = (wrongOptions + targetWord.kasiguranin).distinct().shuffled()
            questionStartTimeMs = System.currentTimeMillis()

            metWordIds += targetWord.id
            _uiState.value = state.copy(
                isLoading = false,
                currentWord = targetWord,
                options = allOptions,
                selectedOption = null,
                isCorrect = null,
                hintRevealed = hintRevealed,
                totalQuestions = totalInitialQuestions
            )
        }
    }

    /** The learner asked for the definition. Costs the speed bonus; see the rating above. */
    fun revealHint() {
        if (_uiState.value.selectedOption != null) return
        usedHint = true
        _uiState.value = _uiState.value.copy(hintRevealed = true)
        saveProgress(hintRevealed = true)
    }

    /** Keeps the round so far, so that leaving the game does not lose it. */
    private fun saveProgress(hintRevealed: Boolean = false) {
        savedGames.save(
            "reverse_match", levelNumber,
            SavedRound(
                questionKeys = questionQueue.map { it.id.toString() },
                answers = reviewItems.map { it.toSavedAnswer() },
                score = _uiState.value.score,
                totalQuestions = totalInitialQuestions,
                usedHint = usedHint,
                hintRevealed = hintRevealed
            )
        )
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val targetWord = state.currentWord ?: return
        val isCorrect = option == targetWord.kasiguranin
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs

        // A hinted answer is graded HARD however fast it came back: it forfeits the speed bonus,
        // and it keeps the SM-2 signal honest, since recall that needed the definition shown is not
        // the same evidence of memory as recall that did not.
        val rating = ReviewRatingMapper.ratingForAnswer(isCorrect, responseTimeMs, state.hintRevealed)
        val newScore = if (isCorrect) state.score + 1 else state.score

        reviewItems.add(
            GameReviewItem(
                prompt = targetWord.tagalog,
                userAnswer = option,
                correctAnswer = targetWord.kasiguranin,
                isCorrect = isCorrect,
                subPrompt = if (targetWord.english.isNotBlank()) targetWord.english else null
            )
        )

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )
        saveProgress()

        viewModelScope.launch {
            vocabularyRepository.processWordReview(targetWord, rating)
        }
    }

    fun nextQuestion() {
        _uiState.value = _uiState.value.copy(currentQuestionIndex = _uiState.value.currentQuestionIndex + 1)
        loadNextQuestion()
    }

    private fun endGame() {
        if (finishing) return
        finishing = true
        // Before the reward, so the round can never be picked up and rewarded again.
        savedGames.clear("reverse_match", levelNumber)
        val state = _uiState.value
        val isPerfect = state.score >= totalInitialQuestions && !usedHint

        viewModelScope.launch {
            val successRate = state.score.toFloat() / totalInitialQuestions
            val starsEarned = when {
                successRate >= 1.0f -> 3
                successRate >= 0.7f -> 2
                successRate >= 0.4f -> 1
                else -> 0
            }
            val xpEarned = userProgressRepository.awardGame("reverse_match","reverse_match",levelNumber,state.score,totalInitialQuestions,starsEarned,isPerfect,metWordIds.toList())
            val scoreEntity = GameScoreEntity(
                gameType = "reverse_match",
                score = state.score,
                totalQuestions = totalInitialQuestions,
                xpEarned = xpEarned,
                playedAt = LocalDateTime.now().toIsoString()
            )
            gameRepository.saveScore(scoreEntity)

            gameLevelRepository.saveLevelResult("reverse_match", levelNumber, starsEarned)

            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(state.score, totalInitialQuestions)


            val nextLevel = if (levelNumber < 30 && (starsEarned >= 1 || gameLevelRepository.getLevel("reverse_match", levelNumber + 1)?.isUnlocked == true)) {
                levelNumber + 1
            } else null

            _uiState.value = state.copy(
                // Still loading when a resumed round had every question answered already.
                isLoading = false,
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

data class ReverseMatchUiState(
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
