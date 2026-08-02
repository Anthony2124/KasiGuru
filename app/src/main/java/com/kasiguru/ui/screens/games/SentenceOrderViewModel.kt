package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SentenceQuestion(
    val englishSentence: String,
    val correctKasiguraninWords: List<String>,
    val shuffledWords: List<String>
)

data class SentenceOrderUiState(
    val questions: List<SentenceQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val availableWords: List<String> = emptyList(),
    val constructedWords: List<String> = emptyList(),
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isGameFinished: Boolean = false
)

@HiltViewModel
class SentenceOrderViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SentenceOrderUiState())
    val uiState: StateFlow<SentenceOrderUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            val sampleSentences = listOf(
                SentenceQuestion(
                    englishSentence = "Good day to you all!",
                    correctKasiguraninWords = listOf("Magandang", "aldaw", "ha", "iyo", "'ttanan!"),
                    shuffledWords = listOf("Magandang", "aldaw", "ha", "iyo", "'ttanan!").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "How is your life?",
                    correctKasiguraninWords = listOf("Kumusta", "na", "ing", "buhay", "mo?"),
                    shuffledWords = listOf("Kumusta", "na", "ing", "buhay", "mo?").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "The child stood up.",
                    correctKasiguraninWords = listOf("Tinumáknəg", "ang", "anák."),
                    shuffledWords = listOf("Tinumáknəg", "ang", "anák.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "I will leave tomorrow.",
                    correctKasiguraninWords = listOf("Maglákad", "akú", "niiláw."),
                    shuffledWords = listOf("Maglákad", "akú", "niiláw.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "Kendy's child is a girl.",
                    correctKasiguraninWords = listOf("Bəbbi", "ang", "anak", "ni", "Kendy."),
                    shuffledWords = listOf("Bəbbi", "ang", "anak", "ni", "Kendy.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "The wind was strong last night.",
                    correctKasiguraninWords = listOf("Mabigsək", "ang", "parəs", "kagibi."),
                    shuffledWords = listOf("Mabigsək", "ang", "parəs", "kagibi.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "Let's go, let's eat!",
                    correctKasiguraninWords = listOf("Karon", "na,", "kuman", "tayo!"),
                    shuffledWords = listOf("Karon", "na,", "kuman", "tayo!").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "There is a person in the house.",
                    correctKasiguraninWords = listOf("Me", "tólay", "sa", "baláy."),
                    shuffledWords = listOf("Me", "tólay", "sa", "baláy.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "There is no person in the house.",
                    correctKasiguraninWords = listOf("Walang", "tólay", "sa", "baláy."),
                    shuffledWords = listOf("Walang", "tólay", "sa", "baláy.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "It is raining now.",
                    correctKasiguraninWords = listOf("Mag-uden", "ngayon."),
                    shuffledWords = listOf("Mag-uden", "ngayon.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "The river is deep.",
                    correctKasiguraninWords = listOf("Madisalad", "ang", "bulos."),
                    shuffledWords = listOf("Madisalad", "ang", "bulos.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "Where are you going?",
                    correctKasiguraninWords = listOf("Saan", "ka", "umangay?"),
                    shuffledWords = listOf("Saan", "ka", "umangay?").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "I bought a mango.",
                    correctKasiguraninWords = listOf("Namúgtong", "ang", "anák", "ng", "mángga."),
                    shuffledWords = listOf("Namúgtong", "ang", "anák", "ng", "mángga.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "I already ate rice.",
                    correctKasiguraninWords = listOf("Kinumán", "na", "ku'", "ng", "kanən."),
                    shuffledWords = listOf("Kinumán", "na", "ku'", "ng", "kanən.").shuffled()
                ),
                SentenceQuestion(
                    englishSentence = "Our viand is chicken.",
                    correctKasiguraninWords = listOf("Ang", "sida", "me", "ay", "manok."),
                    shuffledWords = listOf("Ang", "sida", "me", "ay", "manok.").shuffled()
                )
            )

            _uiState.update {
                it.copy(
                    questions = sampleSentences,
                    availableWords = sampleSentences.firstOrNull()?.shuffledWords ?: emptyList(),
                    constructedWords = emptyList(),
                    isCorrect = null
                )
            }
        }
    }

    fun selectWord(word: String) {
        val currentState = _uiState.value
        val newAvailable = currentState.availableWords.toMutableList()
        newAvailable.remove(word)
        val newConstructed = currentState.constructedWords + word

        _uiState.update {
            it.copy(
                availableWords = newAvailable,
                constructedWords = newConstructed
            )
        }
    }

    fun deselectWord(word: String) {
        val currentState = _uiState.value
        val newConstructed = currentState.constructedWords.toMutableList()
        newConstructed.remove(word)
        val newAvailable = currentState.availableWords + word

        _uiState.update {
            it.copy(
                availableWords = newAvailable,
                constructedWords = newConstructed
            )
        }
    }

    fun checkAnswer() {
        val currentState = _uiState.value
        val currentQuestion = currentState.questions.getOrNull(currentState.currentQuestionIndex) ?: return

        val userSentence = currentState.constructedWords.joinToString(" ")
        val correctSentence = currentQuestion.correctKasiguraninWords.joinToString(" ")
        val isCorrect = userSentence.trim() == correctSentence.trim()

        val newScore = if (isCorrect) currentState.score + 20 else currentState.score

        _uiState.update {
            it.copy(
                isCorrect = isCorrect,
                score = newScore
            )
        }

        if (isCorrect) {
            viewModelScope.launch {
                gameRepository.saveGameScore(
                    gameType = Constants.Games.SENTENCE_ORDER,
                    score = newScore,
                    totalQuestions = currentState.questions.size,
                    xpEarned = newScore
                )
            }
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentQuestionIndex + 1

        if (nextIndex < currentState.questions.size) {
            val nextQuestion = currentState.questions[nextIndex]
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    availableWords = nextQuestion.shuffledWords,
                    constructedWords = emptyList(),
                    isCorrect = null
                )
            }
        } else {
            _uiState.update { it.copy(isGameFinished = true) }
        }
    }

    fun resetGame() {
        _uiState.update {
            SentenceOrderUiState(
                questions = emptyList(),
                currentQuestionIndex = 0,
                availableWords = emptyList(),
                constructedWords = emptyList(),
                isCorrect = null,
                score = 0,
                isGameFinished = false
            )
        }
        loadQuestions()
    }
}
