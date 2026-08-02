package com.kasiguru.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.toIsoString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val totalQuestions = 5
    private var currentVerbs: List<VocabularyEntity> = emptyList()

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = FillBlankUiState(isLoading = true)
            // Query all vocabulary from the repository and filter for verbs with full aspect forms
            vocabularyRepository.getAllVocabulary().collect { list ->
                val verbsWithAspects = list.filter { it.neutralForm.isNotBlank() && it.perfectiveForm.isNotBlank() }
                if (verbsWithAspects.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isGameOver = true)
                    return@collect
                }
                currentVerbs = verbsWithAspects.shuffled().take(totalQuestions)
                loadNextQuestion()
            }
        }
    }

    private fun loadNextQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex >= totalQuestions || state.currentQuestionIndex >= currentVerbs.size) {
            endGame()
            return
        }

        val targetVerb = currentVerbs[state.currentQuestionIndex]
        val aspects = listOf(
            targetVerb.neutralForm,
            targetVerb.imperfectiveForm,
            targetVerb.perfectiveForm,
            targetVerb.contemplativeForm
        ).filter { it.isNotBlank() }

        if (aspects.isEmpty()) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = state.currentQuestionIndex + 1)
            loadNextQuestion()
            return
        }

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

        _uiState.value = state.copy(
            isLoading = false,
            currentVerb = targetVerb,
            sentenceTemplate = sentenceContext,
            options = (aspects + listOf("magluto", "tumáknəg", "namúgtong")).distinct().take(4).shuffled(),
            selectedOption = null,
            isCorrect = null,
            correctAnswer = correctAnswer
        )
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.selectedOption != null) return

        val isCorrect = option == state.correctAnswer
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            selectedOption = option,
            isCorrect = isCorrect,
            score = newScore
        )

        viewModelScope.launch {
            delay(1500)
            _uiState.value = _uiState.value.copy(currentQuestionIndex = _uiState.value.currentQuestionIndex + 1)
            loadNextQuestion()
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val xpEarned = state.score * Constants.XP_PER_GAME_CORRECT + 
                if (state.score == totalQuestions) Constants.XP_BONUS_PERFECT_GAME else 0

        viewModelScope.launch {
            val scoreEntity = GameScoreEntity(
                gameType = "fill_blank",
                score = state.score,
                totalQuestions = totalQuestions,
                xpEarned = xpEarned,
                playedAt = LocalDateTime.now().toIsoString()
            )
            gameRepository.saveScore(scoreEntity)

            userProgressRepository.addXp(xpEarned)
            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(state.score, totalQuestions)
            
            if (state.score == totalQuestions) {
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
