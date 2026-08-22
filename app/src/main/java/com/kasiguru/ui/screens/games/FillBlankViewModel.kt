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
import com.kasiguru.data.repository.buildPracticeRoundFromPool
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
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class FillBlankViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelNumber = savedStateHandle.get<Int>("level") ?: 1

    private val _uiState = MutableStateFlow(FillBlankUiState())
    val uiState: StateFlow<FillBlankUiState> = _uiState.asStateFlow()

    private var totalInitialQuestions = 5
    private val questionQueue = mutableListOf<VocabularyEntity>()
    private var questionStartTimeMs: Long = 0L
    private var earnedXpTotal = 0

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = FillBlankUiState(isLoading = true)
            
            val levelInfo = gameLevelRepository.getLevel("fill_blank", levelNumber)
            if (levelInfo != null) {
                totalInitialQuestions = levelInfo.questionsCount
            }
            
            val list = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
            val verbsWithAspects = list.filter { 
                it.neutralForm.isNotBlank() || it.perfectiveForm.isNotBlank() || it.exampleSentence.isNotBlank() 
            }
            val pool = if (verbsWithAspects.isNotEmpty()) verbsWithAspects else list
            
            if (pool.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false, isUnavailable = true)
                return@launch
            }

            questionQueue.clear()
            // Review-first within the filtered pool, rather than plain timesReviewed order.
            questionQueue.addAll(
                buildPracticeRoundFromPool(pool, LocalDate.now().toString(), totalInitialQuestions)
            )
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
                targetVerb.neutralForm -> listOf(
                    "Karon na, ____ tam!",
                    "Gusto ko ____.",
                    "Kailangan tam ____.",
                    "Dapat kang ____."
                ).random()
                targetVerb.imperfectiveForm -> listOf(
                    "Ngayon, siya ay ____.",
                    "Habang maaga, kami ay ____.",
                    "Palagi siyang ____.",
                    "Bakit ka ____?"
                ).random()
                targetVerb.perfectiveForm -> listOf(
                    "Kagibi, ____ na siya.",
                    "Sino ang ____ kahapon?",
                    "Kanina, ____ ako.",
                    "Nang makita ko siya, ____ na siya."
                ).random()
                targetVerb.contemplativeForm -> listOf(
                    "Niilaw, ____ akú.",
                    "Mamaya, ____ tayo.",
                    "Sa susunod na linggo, ____ siya.",
                    "Sigurado akong ____ sila bukas."
                ).random()
                else -> listOf(
                    "____ ang anák.",
                    "Maganda ang ____.",
                    "Gusto ko ng ____.",
                    "Nasaan ang ____?",
                    "Malaki ang ____."
                ).random()
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
                correctAnswer = correctAnswer,
                totalQuestions = totalInitialQuestions
            )
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val targetVerb = state.currentVerb ?: return
        val isCorrect = option == state.correctAnswer
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs

        val rating = ReviewRatingMapper.ratingForAnswer(isCorrect, responseTimeMs)
        val questionXp = if (isCorrect) {
            if (rating == ReviewRating.HARD) 5 else Constants.XP_PER_GAME_CORRECT
        } else {
            0
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
                gameType = "fill_blank",
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
            gameLevelRepository.saveLevelResult("fill_blank", levelNumber, starsEarned)

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

data class FillBlankUiState(
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val currentVerb: VocabularyEntity? = null,
    val sentenceTemplate: String = "",
    val correctAnswer: String = "",
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val finalXp: Int = 0,
    val starsEarned: Int = 0,
    val totalQuestions: Int = 5
)
