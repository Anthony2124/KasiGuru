package com.kasiguru.ui.screens.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
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
            val randomWords = vocabularyRepository.getRandomWords(10)
            _uiState.update {
                it.copy(
                    cards = randomWords,
                    currentIndex = 0,
                    isLoading = false,
                    isDeckComplete = randomWords.isEmpty()
                )
            }
        }
    }

    fun rateCard(rating: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            // Reward XP for reviewing cards
            userProgressRepository.addXp(Constants.XP_PER_WORD_LEARNED)
            
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.cards.size) {
                _uiState.update { it.copy(isDeckComplete = true) }
            } else {
                _uiState.update { it.copy(currentIndex = nextIndex) }
            }
        }
    }
}
