package com.kasiguru.ui.screens.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    // Backs the floating dictionary search bar, separate from selectCategory's in-memory filter:
    // this one queries Room directly (VocabularyDao.searchVocabulary) so it can find a word in any
    // category, not just the one currently selected. Debounced so every keystroke doesn't issue a
    // new query - the first debounce anywhere in this app outside an unrelated sync timer.
    private val searchQuery = MutableStateFlow("")
    val dictionarySearchResults: StateFlow<List<VocabularyEntity>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else vocabularyRepository.searchVocabulary(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadData()
    }

    fun onDictionarySearchQueryChange(query: String) {
        searchQuery.value = query
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
