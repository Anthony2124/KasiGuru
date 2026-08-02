package com.kasiguru.ui.screens.vocabulary

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

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                vocabularyRepository.getCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            }
            launch {
                vocabularyRepository.getAllVocabulary().collect { allVocab ->
                    _uiState.value = _uiState.value.copy(
                        allVocabulary = allVocab,
                        isLoading = false
                    )
                    // Initial category setup
                    if (_uiState.value.selectedCategory == null && allVocab.isNotEmpty()) {
                        selectCategory("All")
                    }
                }
            }
        }
    }

    fun selectCategory(category: String) {
        val filteredList = if (category == "All") {
            _uiState.value.allVocabulary
        } else {
            _uiState.value.allVocabulary.filter { it.category == category }
        }
        
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            filteredVocabulary = filteredList
        )
    }

    fun markWordAsLearned(id: Int) {
        viewModelScope.launch {
            vocabularyRepository.markAsLearned(id)
        }
    }
}

data class VocabularyUiState(
    val categories: List<String> = emptyList(),
    val allVocabulary: List<VocabularyEntity> = emptyList(),
    val filteredVocabulary: List<VocabularyEntity> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = true
)
