package com.kasiguru.ui.screens.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyDetailUiState(
    val word: VocabularyEntity? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false
)

/**
 * Backs the `vocabulary/{wordId}` destination that push notifications deep-link into
 * (see functions/send_push.js). Before this existed, the route was declared in [com.kasiguru.ui.navigation.Screen]
 * but had no matching `composable()`, so the notification silently failed to navigate.
 */
@HiltViewModel
class VocabularyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val wordId: Int = checkNotNull(savedStateHandle["wordId"])

    private val _uiState = MutableStateFlow(VocabularyDetailUiState())
    val uiState: StateFlow<VocabularyDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val word = vocabularyRepository.getVocabularyById(wordId)
            _uiState.value = if (word != null) {
                VocabularyDetailUiState(word = word, isLoading = false)
            } else {
                VocabularyDetailUiState(isLoading = false, notFound = true)
            }
        }
    }
}
