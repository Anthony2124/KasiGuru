package com.kasiguru.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkAndSeedVocabulary()
        loadProgress()
        updateDailyStreak()
    }

    private fun checkAndSeedVocabulary() {
        viewModelScope.launch {
            if (vocabularyRepository.getTotalCountDirect() == 0) {
                vocabularyRepository.insertAll(DatabaseSeeder.getInitialVocabulary())
            }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            userProgressRepository.getUserProgress().collect { progress ->
                if (progress != null) {
                    _uiState.value = _uiState.value.copy(
                        userProgress = progress,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun updateDailyStreak() {
        viewModelScope.launch {
            userProgressRepository.updateStreak()
        }
    }
}

data class HomeUiState(
    val userProgress: UserProgressEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
