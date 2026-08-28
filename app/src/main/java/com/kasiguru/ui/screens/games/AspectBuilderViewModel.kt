package com.kasiguru.ui.screens.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.data.repository.buildPracticeRoundFromPool
import java.time.LocalDate
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.ReviewRatingMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.kasiguru.ui.components.GameReviewItem

data class AspectQuestion(
    val targetVocab: VocabularyEntity,
    val rootWord: String,
    val translation: String,
    val targetAspect: String, // Neutral, Imperfective, Perfective, Contemplative
    val correctAnswer: String,
    val options: List<String>
)

data class AspectBuilderUiState(
    val questions: List<AspectQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val hintRevealed: Boolean = false,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val xpEarned: Int = 0,
    val isGameOver: Boolean = false,
    val isUnavailable: Boolean = false,
    val isLoading: Boolean = true,
    val starsEarned: Int = 0,
    val totalQuestions: Int = 5,
    val reviewItems: List<GameReviewItem> = emptyList()
)

@HiltViewModel
class AspectBuilderViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelNumber = savedStateHandle.get<Int>("level") ?: 1

    private val _uiState = MutableStateFlow(AspectBuilderUiState())
    val uiState: StateFlow<AspectBuilderUiState> = _uiState.asStateFlow()

    private val questionQueue = mutableListOf<AspectQuestion>()
    private var questionStartTimeMs: Long = 0L
    private var totalInitialQuestions = 5
    private val reviewItems = mutableListOf<GameReviewItem>()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        reviewItems.clear()
        viewModelScope.launch {
            val levelInfo = gameLevelRepository.getLevel("aspect_builder", levelNumber)
            if (levelInfo != null) {
                totalInitialQuestions = levelInfo.questionsCount
            }

            val list = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
            val questions = generateAspectQuestions(list, totalInitialQuestions)
            if (questions.isEmpty()) {
                // No verbs with documented aspect forms yet: show the
                // "coming soon" state instead of an empty broken game.
                _uiState.update { it.copy(isLoading = false, isUnavailable = true) }
                return@launch
            }
            questionQueue.clear()
            questionQueue.addAll(questions)
            totalInitialQuestions = questions.size

            questionStartTimeMs = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    questions = questionQueue.toList(),
                    isLoading = false,
                    totalQuestions = totalInitialQuestions
                )
            }
        }
    }

    private suspend fun generateAspectQuestions(vocabList: List<VocabularyEntity>, questionCount: Int): List<AspectQuestion> {
        val aspectList = listOf("Neutral", "Imperfective", "Perfective", "Contemplative")
        val result = mutableListOf<AspectQuestion>()

        val conjugatable = vocabList.filter {
            it.neutralForm.isNotBlank() || it.perfectiveForm.isNotBlank() || it.imperfectiveForm.isNotBlank() || it.contemplativeForm.isNotBlank()
        }

        if (conjugatable.isEmpty()) return emptyList()

        // Review-first within the conjugatable pool, rather than plain timesReviewed order —
        // this game writes an SM-2 schedule like the others and should read it back too.
        val verbs = buildPracticeRoundFromPool(conjugatable, LocalDate.now().toString(), questionCount)

        for (vocab in verbs) {
            val aspect = aspectList.random()
            val correct = when (aspect) {
                "Neutral" -> vocab.neutralForm.ifEmpty { vocab.kasiguranin }
                "Imperfective" -> vocab.imperfectiveForm.ifEmpty { vocab.kasiguranin }
                "Perfective" -> vocab.perfectiveForm.ifEmpty { vocab.kasiguranin }
                "Contemplative" -> vocab.contemplativeForm.ifEmpty { vocab.kasiguranin }
                else -> vocab.kasiguranin
            }

            val distractorEntities = vocabularyRepository.getDistractorsForWord(vocab, 3)
            val distractors = distractorEntities.map { it.kasiguranin }.filter { it.isNotBlank() }

            val options = (distractors + correct).distinct().shuffled()

            result.add(
                AspectQuestion(
                    targetVocab = vocab,
                    rootWord = vocab.rootForm.ifEmpty { vocab.kasiguranin },
                    translation = vocab.english,
                    targetAspect = aspect,
                    correctAnswer = correct,
                    options = options
                )
            )
        }
        return result
    }

    /** The learner asked for the definition. Costs the speed bonus; see the rating in submitAnswer. */
    fun revealHint() {
        if (_uiState.value.selectedAnswer != null) return
        _uiState.value = _uiState.value.copy(hintRevealed = true)
    }

    fun submitAnswer(answer: String) {
        val state = _uiState.value
        val currentQ = questionQueue.getOrNull(state.currentIndex) ?: return
        if (state.selectedAnswer != null) return

        val correct = answer == currentQ.correctAnswer
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs

        // A hinted answer is graded HARD however fast it came back: it forfeits the speed bonus,
        // and it keeps the SM-2 signal honest, since recall that needed the definition shown is not
        // the same evidence of memory as recall that did not.
        val rating = if (state.hintRevealed) {
            ReviewRating.HARD
        } else {
            ReviewRatingMapper.ratingForAnswer(correct, responseTimeMs)
        }
        val questionXp = if (correct) {
            if (rating == ReviewRating.HARD) 5 else Constants.XP_PER_GAME_CORRECT
        } else {
            0
        }

        val newScore = if (correct) state.score + 1 else state.score
        val newXp = state.xpEarned + questionXp

        reviewItems.add(
            GameReviewItem(
                prompt = "${currentQ.rootWord} (${currentQ.targetAspect} Aspect)",
                userAnswer = answer,
                correctAnswer = currentQ.correctAnswer,
                isCorrect = correct,
                subPrompt = if (currentQ.translation.isNotBlank()) currentQ.translation else null
            )
        )

        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                isCorrect = correct,
                score = newScore,
                xpEarned = newXp,
                questions = questionQueue.toList()
            )
        }

        viewModelScope.launch {
            vocabularyRepository.processWordReview(currentQ.targetVocab, rating)
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex + 1 >= questionQueue.size) {
            viewModelScope.launch {
                val isPerfect = state.score >= totalInitialQuestions
                val finalXp = state.xpEarned + if (isPerfect) Constants.XP_BONUS_PERFECT_GAME else 0

                val successRate = state.score.toFloat() / totalInitialQuestions
                val starsEarned = when {
                    successRate >= 1.0f -> 3
                    successRate >= 0.7f -> 2
                    successRate >= 0.4f -> 1
                    else -> 0
                }
                gameLevelRepository.saveLevelResult("aspect_builder", levelNumber, starsEarned)

                gameRepository.saveGameScore(
                    gameType = "aspect_builder",
                    score = state.score,
                    totalQuestions = totalInitialQuestions,
                    xpEarned = finalXp
                )
                userProgressRepository.addXp(finalXp)
                userProgressRepository.incrementGamesPlayed()
                userProgressRepository.updateGameStats(state.score, totalInitialQuestions)

                if (isPerfect) {
                    userProgressRepository.checkPerfectGameAchievement()
                }

                _uiState.update {
                    it.copy(
                        isGameOver = true,
                        xpEarned = finalXp,
                        starsEarned = starsEarned,
                        totalQuestions = totalInitialQuestions,
                        reviewItems = reviewItems.toList()
                    )
                }
            }
        } else {
            questionStartTimeMs = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedAnswer = null,
                    isCorrect = null,
                    hintRevealed = false
                )
            }
        }
    }
}
