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

data class CategoryProgressStats(
    val totalWords: Int = 0,
    val learnedWords: Int = 0
)

data class VocabularyUiState(
    val categories: List<String> = emptyList(),
    val allVocabulary: List<VocabularyEntity> = emptyList(),
    val filteredVocabulary: List<VocabularyEntity> = emptyList(),
    val selectedCategory: String = "All",
    val categoryStats: Map<String, CategoryProgressStats> = emptyMap(),
    val totalLearnedCount: Int = 0,
    val isLoading: Boolean = true
)

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
                    // Calculate stats per category
                    val stats = allVocab.groupBy { it.category }.mapValues { entry ->
                        CategoryProgressStats(
                            totalWords = entry.value.size,
                            learnedWords = entry.value.count { it.isLearned }
                        )
                    }

                    val totalLearned = allVocab.count { it.isLearned }

                    val filteredList = if (_uiState.value.selectedCategory == "All") {
                        allVocab
                    } else {
                        allVocab.filter { it.category == _uiState.value.selectedCategory }
                    }

                    _uiState.value = _uiState.value.copy(
                        allVocabulary = allVocab,
                        filteredVocabulary = filteredList,
                        categoryStats = stats,
                        totalLearnedCount = totalLearned,
                        isLoading = false
                    )
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

    fun unmarkWordAsLearned(id: Int) {
        viewModelScope.launch {
            vocabularyRepository.unmarkAsLearned(id)
        }
    }
}
