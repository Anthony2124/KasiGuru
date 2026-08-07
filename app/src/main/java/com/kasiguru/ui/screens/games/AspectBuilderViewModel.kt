package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val xpEarned: Int = 0,
    val isGameOver: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class AspectBuilderViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AspectBuilderUiState())
    val uiState: StateFlow<AspectBuilderUiState> = _uiState.asStateFlow()

    private val questionQueue = mutableListOf<AspectQuestion>()
    private val requeuedVerbIds = mutableSetOf<Int>()
    private var questionStartTimeMs: Long = 0L
    private var totalInitialQuestions = 5

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            val list = vocabularyRepository.getAllVocabulary().firstOrNull { it.isNotEmpty() } ?: emptyList()
            val questions = generateAspectQuestions(list)
            questionQueue.clear()
            questionQueue.addAll(questions)
            requeuedVerbIds.clear()
            totalInitialQuestions = questions.size

            questionStartTimeMs = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    questions = questionQueue.toList(),
                    isLoading = false
                )
            }
        }
    }

    private suspend fun generateAspectQuestions(vocabList: List<VocabularyEntity>): List<AspectQuestion> {
        val aspectList = listOf("Neutral", "Imperfective", "Perfective", "Contemplative")
        val result = mutableListOf<AspectQuestion>()

        val verbList = vocabList.filter { it.neutralForm.isNotBlank() || it.perfectiveForm.isNotBlank() }
        val pool = if (verbList.isNotEmpty()) verbList else vocabList

        for (vocab in pool.shuffled().take(5)) {
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

    fun submitAnswer(answer: String) {
        val state = _uiState.value
        val currentQ = questionQueue.getOrNull(state.currentIndex) ?: return
        if (state.selectedAnswer != null) return

        val correct = answer == currentQ.correctAnswer
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs
        val isRequeued = requeuedVerbIds.contains(currentQ.targetVocab.id)

        val rating: ReviewRating
        val questionXp: Int

        if (correct) {
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
                requeuedVerbIds.add(currentQ.targetVocab.id)
                val insertIndex = (state.currentIndex + 3).coerceAtMost(questionQueue.size)
                questionQueue.add(insertIndex, currentQ)
            }
        }

        val newScore = if (correct) state.score + 1 else state.score
        val newXp = state.xpEarned + questionXp

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
                val isPerfect = state.score >= totalInitialQuestions && requeuedVerbIds.isEmpty()
                val finalXp = state.xpEarned + if (isPerfect) Constants.XP_BONUS_PERFECT_GAME else 0

                gameRepository.saveGameScore(
                    gameType = "aspect_builder",
                    score = state.score,
                    totalQuestions = questionQueue.size,
                    xpEarned = finalXp
                )
                userProgressRepository.addXp(finalXp)
                userProgressRepository.incrementGamesPlayed()
                userProgressRepository.updateGameStats(state.score, questionQueue.size)

                if (isPerfect) {
                    userProgressRepository.checkPerfectGameAchievement()
                }

                _uiState.update { it.copy(isGameOver = true, xpEarned = finalXp) }
            }
        } else {
            questionStartTimeMs = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedAnswer = null,
                    isCorrect = null
                )
            }
        }
    }
}
