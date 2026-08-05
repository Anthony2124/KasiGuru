package com.kasiguru.ui.screens.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.Sm2Algorithm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardUiState(
    val cards: List<VocabularyEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isDeckComplete: Boolean = false
)

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            val dueWords = vocabularyRepository.getDueReviewWords(10)
            _uiState.update {
                it.copy(
                    cards = dueWords,
                    currentIndex = 0,
                    isLoading = false,
                    isDeckComplete = dueWords.isEmpty()
                )
            }
        }
    }

    fun rateCard(rating: ReviewRating) {
        val state = _uiState.value
        val currentCard = state.cards.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            // 1. Run SM-2 SRS calculation
            val sm2Result = Sm2Algorithm.calculateNextReview(currentCard, rating)

            // 2. Update vocabulary entity in database
            val updatedCard = currentCard.copy(
                easinessFactor = sm2Result.easinessFactor,
                intervalDays = sm2Result.intervalDays,
                nextReviewDate = sm2Result.nextReviewDate,
                timesReviewed = sm2Result.timesReviewed,
                isLearned = sm2Result.isLearned
            )
            vocabularyRepository.updateVocabulary(updatedCard)

            // 3. Reward XP for reviewing card
            val xpGain = when (rating) {
                ReviewRating.AGAIN -> 2
                ReviewRating.HARD -> 5
                ReviewRating.GOOD -> 10
                ReviewRating.EASY -> 15
            }
            userProgressRepository.addXp(xpGain)

            // 4. Advance deck
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.cards.size) {
                _uiState.update { it.copy(isDeckComplete = true) }
            } else {
                _uiState.update { it.copy(currentIndex = nextIndex) }
            }
        }
    }
}
