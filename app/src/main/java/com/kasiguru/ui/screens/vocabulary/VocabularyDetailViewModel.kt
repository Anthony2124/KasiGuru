package com.kasiguru.ui.screens.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyDetailUiState(
    val word: VocabularyEntity? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false
)

@HiltViewModel
class VocabularyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val wordId: Int = checkNotNull(savedStateHandle["wordId"])

    private val _uiState = MutableStateFlow(VocabularyDetailUiState())
    val uiState: StateFlow<VocabularyDetailUiState> = _uiState.asStateFlow()

    val allWords: StateFlow<List<VocabularyEntity>> = vocabularyRepository.getAllVocabulary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadWord()
    }

    private fun loadWord() {
        viewModelScope.launch {
            val word = vocabularyRepository.getVocabularyById(wordId)
            _uiState.value = if (word != null) {
                VocabularyDetailUiState(word = word, isLoading = false)
            } else {
                VocabularyDetailUiState(isLoading = false, notFound = true)
            }
        }
    }

    fun markWordAsLearned() {
        viewModelScope.launch {
            vocabularyRepository.markAsLearned(wordId)
            loadWord()
        }
    }
}
