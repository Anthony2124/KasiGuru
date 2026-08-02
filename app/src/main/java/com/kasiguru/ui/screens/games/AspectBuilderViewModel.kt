package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AspectQuestion(
    val rootWord: String,
    val translation: String,
    val targetAspect: String, // neutral, imperfective, perfective, contemplative
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
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AspectBuilderUiState())
    val uiState: StateFlow<AspectBuilderUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            vocabularyRepository.getAllVocabulary().collect { list ->
                val questions = generateAspectQuestions(list)
                _uiState.update {
                    it.copy(
                        questions = questions,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun generateAspectQuestions(vocabList: List<VocabularyEntity>): List<AspectQuestion> {
        val aspectList = listOf("Neutral", "Imperfective", "Perfective", "Contemplative")
        val result = mutableListOf<AspectQuestion>()

        val verbList = vocabList.filter { it.neutralForm.isNotBlank() || it.perfectiveForm.isNotBlank() }
        val pool = if (verbList.isNotEmpty()) verbList else vocabList

        pool.shuffled().take(5).forEach { vocab ->
            val aspect = aspectList.random()
            val correct = when (aspect) {
                "Neutral" -> vocab.neutralForm.ifEmpty { vocab.kasiguranin }
                "Imperfective" -> vocab.imperfectiveForm.ifEmpty { vocab.kasiguranin }
                "Perfective" -> vocab.perfectiveForm.ifEmpty { vocab.kasiguranin }
                "Contemplative" -> vocab.contemplativeForm.ifEmpty { vocab.kasiguranin }
                else -> vocab.kasiguranin
            }

            val distractors = vocabList
                .filter { it.id != vocab.id }
                .map { it.kasiguranin }
                .shuffled()
                .take(3)

            val options = (distractors + correct).distinct().shuffled()

            result.add(
                AspectQuestion(
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
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val correct = answer == currentQ.correctAnswer

        val newScore = if (correct) state.score + 1 else state.score
        val newXp = if (correct) state.xpEarned + 10 else state.xpEarned

        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                isCorrect = correct,
                score = newScore,
                xpEarned = newXp
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex + 1 >= state.questions.size) {
            viewModelScope.launch {
                gameRepository.saveGameScore(
                    gameType = "aspect_builder",
                    score = state.score,
                    totalQuestions = state.questions.size,
                    xpEarned = state.xpEarned
                )
                _uiState.update { it.copy(isGameOver = true) }
            }
        } else {
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
