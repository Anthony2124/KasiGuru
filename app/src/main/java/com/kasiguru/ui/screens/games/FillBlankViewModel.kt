package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class FillBlankViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FillBlankUiState())
    val uiState: StateFlow<FillBlankUiState> = _uiState.asStateFlow()

    private val totalInitialQuestions = 5
    private val questionQueue = mutableListOf<VocabularyEntity>()
    private val requeuedVerbIds = mutableSetOf<Int>()
    private var questionStartTimeMs: Long = 0L
    private var earnedXpTotal = 0

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = FillBlankUiState(isLoading = true)
            val list = vocabularyRepository.getAllVocabulary().filter { it.isNotEmpty() }.first()
            val verbsWithAspects = list.filter { 
                it.neutralForm.isNotBlank() || it.perfectiveForm.isNotBlank() || it.exampleSentence.isNotBlank() 
            }
            val pool = if (verbsWithAspects.isNotEmpty()) verbsWithAspects else list
            
            if (pool.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false, isGameOver = true)
                return@launch
            }

            questionQueue.clear()
            questionQueue.addAll(pool.shuffled().take(totalInitialQuestions))
            requeuedVerbIds.clear()
            earnedXpTotal = 0

            loadNextQuestion()
        }
    }

    private fun loadNextQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex >= questionQueue.size) {
            endGame()
            return
        }

        val targetVerb = questionQueue[state.currentQuestionIndex]
        val rawAspects = listOf(
            targetVerb.neutralForm,
            targetVerb.imperfectiveForm,
            targetVerb.perfectiveForm,
            targetVerb.contemplativeForm
        ).filter { it.isNotBlank() }

        val aspects = if (rawAspects.isNotEmpty()) rawAspects else listOf(targetVerb.kasiguranin)

        val correctAnswer = aspects.random()
        val sentenceContext = if (targetVerb.exampleSentence.contains(correctAnswer, ignoreCase = true)) {
            targetVerb.exampleSentence.replace(correctAnswer, "____", ignoreCase = true)
        } else {
            when (correctAnswer) {
                targetVerb.neutralForm -> "Karon na, ____ tam!"
                targetVerb.imperfectiveForm -> "Ngayon, siya ay ____."
                targetVerb.perfectiveForm -> "Kagibi, ____ na siya."
                targetVerb.contemplativeForm -> "Niilaw, ____ akú."
                else -> "____ ang anák."
            }
        }

        viewModelScope.launch {
            val distractorEntities = vocabularyRepository.getDistractorsForWord(targetVerb, 3)
            val distractors = distractorEntities.map { it.kasiguranin }.filter { it.isNotBlank() }
            val options = (aspects + distractors).distinct().take(4).shuffled()
            questionStartTimeMs = System.currentTimeMillis()

            _uiState.value = state.copy(
                isLoading = false,
                currentVerb = targetVerb,
                sentenceTemplate = sentenceContext,
                options = options,
                selectedOption = null,
                isCorrect = null,
                correctAnswer = correctAnswer
            )
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val targetVerb = state.currentVerb ?: return
        val isCorrect = option == state.correctAnswer
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs
        val isRequeued = requeuedVerbIds.contains(targetVerb.id)

        val rating: ReviewRating
        val questionXp: Int

        if (isCorrect) {
            rating = if (responseTimeMs < 1200) ReviewRating.HARD else ReviewRating.GOOD
            questionXp = when {
                isRequeued -> 0
                rating == ReviewRating.HARD -> 5
                else -> Constants.XP_PER_GAME_CORRECT
            }
        } else {
            rating = ReviewRating.AGAIN
            questionXp = 0

            if (!isRequeued) {
                requeuedVerbIds.add(targetVerb.id)
                val insertIndex = (state.currentQuestionIndex + 3).coerceAtMost(questionQueue.size)
                questionQueue.add(insertIndex, targetVerb)
            }
        }

        earnedXpTotal += questionXp
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )

        viewModelScope.launch {
            vocabularyRepository.processWordReview(targetVerb, rating)

            delay(1500)
            _uiState.value = _uiState.value.copy(currentQuestionIndex = _uiState.value.currentQuestionIndex + 1)
            loadNextQuestion()
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val isPerfect = state.score >= totalInitialQuestions && requeuedVerbIds.isEmpty()
        val xpEarned = earnedXpTotal + if (isPerfect) Constants.XP_BONUS_PERFECT_GAME else 0

        viewModelScope.launch {
            val scoreEntity = GameScoreEntity(
                gameType = "fill_blank",
                score = state.score,
                totalQuestions = questionQueue.size,
                xpEarned = xpEarned,
                playedAt = LocalDateTime.now().toIsoString()
            )
            gameRepository.saveScore(scoreEntity)

            userProgressRepository.addXp(xpEarned)
            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(state.score, questionQueue.size)
            
            if (isPerfect) {
                userProgressRepository.checkPerfectGameAchievement()
            }

            _uiState.value = state.copy(
                isGameOver = true,
                finalXp = xpEarned
            )
        }
    }
}

data class FillBlankUiState(
    val isLoading: Boolean = true,
    val currentQuestionIndex: Int = 0,
    val currentVerb: VocabularyEntity? = null,
    val sentenceTemplate: String = "",
    val correctAnswer: String = "",
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val finalXp: Int = 0
)
