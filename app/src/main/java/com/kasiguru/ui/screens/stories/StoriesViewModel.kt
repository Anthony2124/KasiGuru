package com.kasiguru.ui.screens.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.data.repository.StoryRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoriesUiState())
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                storyRepository.getAllStories().collect { stories ->
                    _uiState.value = _uiState.value.copy(
                        stories = stories,
                        isLoading = false
                    )
                }
            }
            launch {
                userProgressRepository.getUserProgress().collect { progress ->
                    if (progress != null) {
                        _uiState.value = _uiState.value.copy(currentXp = progress.totalXp)
                        // Auto-unlock stories based on new XP
                        storyRepository.unlockStoriesByXp(progress.totalXp)
                    }
                }
            }
        }
    }
}

data class StoriesUiState(
    val stories: List<StoryEntity> = emptyList(),
    val currentXp: Int = 0,
    val isLoading: Boolean = true
)
