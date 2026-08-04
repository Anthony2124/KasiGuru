package com.kasiguru.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.data.repository.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val leaderboard: List<LeaderboardEntity> = emptyList(),
    val selectedFilter: String = "All-Time XP",
    val currentUserRank: Int = 0,
    val currentUserEntry: LeaderboardEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            setFilter("All-Time XP")
        }
    }

    fun setFilter(filter: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedFilter = filter, isLoading = true)
            
            val flow = when (filter) {
                "Streak Masters" -> leaderboardRepository.getLeaderboardByStreak()
                else -> leaderboardRepository.getLeaderboardByXp()
            }

            flow.collect { list ->
                val userIndex = list.indexOfFirst { it.isCurrentUser }
                val userRank = if (userIndex != -1) userIndex + 1 else list.size + 1
                val userEntry = if (userIndex != -1) list[userIndex] else null

                _uiState.value = _uiState.value.copy(
                    leaderboard = list,
                    currentUserRank = userRank,
                    currentUserEntry = userEntry,
                    isLoading = false
                )
            }
        }
    }
}
