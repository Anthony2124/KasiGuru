package com.kasiguru.ui.screens.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.RecallAnswerMatcher
import com.kasiguru.util.RecallGrading
import com.kasiguru.util.RecallMatch
import com.kasiguru.util.RecallPrompt
import com.kasiguru.util.srs.ReviewRating
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
 * Word Recall — the only game that asks the learner to *produce* a word.
 *
 * It replaces Audio Quiz in this slot. That game showed a speaker button and four options, but no
 * Kasiguranin recording has ever existed in the corpus (`res/raw/` is empty and no entry carries an
 * audio file name), so every question fell through to the device's Filipino text-to-speech voice
 * reading a Kasiguranin headword. Practising against a synthetic Tagalog approximation of an
 * endangered language teaches a pronunciation the language does not have, which is worse for the
 * thesis than having one game fewer.
 *
 * Recall keeps the whole round, level, star and XP scaffolding of the slot it took over, including
 * its stored key (see [Constants.Games.RECALL]), and swaps only what the learner does: the meaning
 * is the prompt, and the Kasiguranin word must be typed from memory.
 */
@HiltViewModel
class RecallGameViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelNumber = savedStateHandle.get<Int>("level") ?: 1

    private val _uiState = MutableStateFlow(RecallGameUiState())
    val uiState: StateFlow<RecallGameUiState> = _uiState.asStateFlow()

    private var totalInitialQuestions = 5
    private val questionQueue = mutableListOf<VocabularyEntity>()
    private var questionStartTimeMs: Long = 0L
    private var earnedXpTotal = 0

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = RecallGameUiState(isLoading = true)

            val levelInfo = gameLevelRepository.getLevel(Constants.Games.RECALL, levelNumber)
            if (levelInfo != null) {
                totalInitialQuestions = levelInfo.questionsCount
            }

            // Review-first, then new material — see VocabularyRepository.buildPracticeRound.
            var words = vocabularyRepository.getPracticeWords(totalInitialQuestions)
            if (words.isEmpty()) {
                val all = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
                words = all.sortedBy { it.timesReviewed }.take(totalInitialQuestions).shuffled()
            }

            // A word whose only gloss is the headword itself cannot be asked for here: the
            // meaning *is* the question, where the other games can still fall back on a headword or
            // a sentence. See RecallPrompt.
            questionQueue.clear()
            questionQueue.addAll(words.filter { promptFor(it) != null })
            earnedXpTotal = 0

            if (questionQueue.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false, isUnavailable = true)
                return@launch
            }

            // The round is as long as the words that survived that filter, so the progress bar and
            // the perfect-round bonus both measure against what the learner is actually asked.
            totalInitialQuestions = questionQueue.size

            loadNextQuestion()
        }
    }

    private fun promptFor(word: VocabularyEntity): String? =
        RecallPrompt.meaningFor(word.kasiguranin, word.tagalog, word.english)

    private fun loadNextQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex >= questionQueue.size) {
            endGame()
            return
        }

        val targetWord = questionQueue[state.currentQuestionIndex]
        questionStartTimeMs = System.currentTimeMillis()

        _uiState.value = state.copy(
            isLoading = false,
            currentWord = targetWord,
            promptMeaning = promptFor(targetWord).orEmpty(),
            typedAnswer = "",
            match = null,
            hintRevealed = false,
            totalQuestions = totalInitialQuestions
        )
    }

    /** The learner asked for the definition of the word they are trying to produce. */
    fun revealHint() {
        if (_uiState.value.hasAnswered) return
        _uiState.value = _uiState.value.copy(hintRevealed = true)
    }

    /** Live text from the input field. Committing it is [submit]. */
    fun updateTypedAnswer(text: String) {
        if (_uiState.value.hasAnswered) return
        _uiState.value = _uiState.value.copy(typedAnswer = text)
    }

    fun submit() {
        val state = _uiState.value
        if (state.hasAnswered) return

        val targetWord = state.currentWord ?: return
        if (state.typedAnswer.isBlank()) return

        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs
        val match = RecallAnswerMatcher.match(state.typedAnswer, targetWord.kasiguranin)
        // A hinted answer never grades EASY: producing a word with its definition in front of you
        // is weaker evidence of recall than producing it cold, and SM-2 should schedule it as such.
        val rating = RecallGrading.ratingFor(match, targetWord.kasiguranin, responseTimeMs)
            .let { if (state.hintRevealed && it == ReviewRating.EASY) ReviewRating.GOOD else it }

        earnedXpTotal += RecallGrading.xpFor(match)
        val newScore = if (RecallGrading.isCorrect(match)) state.score + 1 else state.score

        _uiState.value = state.copy(match = match, score = newScore)

        viewModelScope.launch {
            vocabularyRepository.processWordReview(targetWord, rating)
        }
    }

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
                gameType = Constants.Games.RECALL,
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
            gameLevelRepository.saveLevelResult(Constants.Games.RECALL, levelNumber, starsEarned)

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

data class RecallGameUiState(
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val currentWord: VocabularyEntity? = null,
    /** The meaning shown as the question; the headword is what must be produced. */
    val promptMeaning: String = "",
    val typedAnswer: String = "",
    val hintRevealed: Boolean = false,
    /** Null until the answer is committed, then how close it was. */
    val match: RecallMatch? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val finalXp: Int = 0,
    val starsEarned: Int = 0,
    val totalQuestions: Int = 5
) {
    val hasAnswered: Boolean get() = match != null
}
