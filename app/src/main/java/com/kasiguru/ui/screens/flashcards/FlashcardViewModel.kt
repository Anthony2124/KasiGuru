package com.kasiguru.ui.screens.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
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
    val isDeckComplete: Boolean = false,
    val isRating: Boolean = false,
    /**
     * Nothing was scheduled for today, as distinct from having worked through a deck.
     *
     * These were the same state, which produced "Daily Deck Complete! You reviewed 0 flashcards"
     * — and before that, the deck quietly filled itself with random words so the screen rarely
     * admitted an empty schedule at all. An honest empty day is the correct outcome of spaced
     * repetition working, not a failure to paper over.
     */
    val isNothingDue: Boolean = false,
    /** Reviewed anyway, past the schedule, at the learner's explicit request. */
    val isExtraPractice: Boolean = false
)

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private companion object {
        /** Cards per sitting. A cap, not a target — a short honest deck beats a padded one. */
        const val DECK_SIZE = 10
    }

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            // Strict, not getDueReviewWords: the loose query matches nextReviewDate = '' (every
            // never-seen word) and substitutes random words when nothing is due, so the deck
            // served ten arbitrary cards while the Learn screen said "Nothing due today". Review
            // that isn't scheduled isn't review — it's a quiz wearing review's clothes, and it
            // silently rewrites the schedule for words that were not ready.
            val dueWords = vocabularyRepository.getDueReviewWordsStrict(DECK_SIZE)
            _uiState.update {
                it.copy(
                    cards = dueWords,
                    currentIndex = 0,
                    isLoading = false,
                    isDeckComplete = false,
                    isNothingDue = dueWords.isEmpty(),
                    isExtraPractice = false
                )
            }
        }
    }

    /**
     * Practises words that are not due yet, on request.
     *
     * The learner asked for this, so it is not the schedule lying to them. Words are still rated
     * through SM-2 — an early correct answer legitimately lengthens the interval.
     */
    fun practiseAnyway() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val extra = vocabularyRepository.getPracticeWords(DECK_SIZE)
            _uiState.update {
                it.copy(
                    cards = extra,
                    currentIndex = 0,
                    isLoading = false,
                    isDeckComplete = false,
                    isNothingDue = extra.isEmpty(),
                    isExtraPractice = extra.isNotEmpty()
                )
            }
        }
    }

    fun rateCard(rating: ReviewRating) {
        val state = _uiState.value
        // Guards against a second tap landing while the first rating's suspend write is still in
        // flight — both would otherwise read the same currentIndex and double-rate one card.
        if (state.isRating) return
        val currentCard = state.cards.getOrNull(state.currentIndex) ?: return
        _uiState.update { it.copy(isRating = true) }

        viewModelScope.launch {
            // Routed through the repository rather than running SM-2 here. This screen used to
            // duplicate the calculation and write the entity itself, which meant it set isLearned
            // without ever calling incrementWordsLearned, awarding the word-learned XP, or
            // checking category mastery — so a word first mastered on a flashcard raised the row
            // flag but not the counter the profile displays, and the two drifted apart
            // permanently. One path for reviews, whatever surface they happen on.
            vocabularyRepository.processWordReview(currentCard, rating)

            // Per-card XP for the act of reviewing, on top of any word-learned bonus the
            // repository awards when a word crosses the threshold.
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
                _uiState.update { it.copy(isDeckComplete = true, isRating = false) }
            } else {
                _uiState.update { it.copy(currentIndex = nextIndex, isRating = false) }
            }
        }
    }
}
