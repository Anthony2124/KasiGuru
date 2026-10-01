package com.kasiguru.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.data.repository.LeaderboardRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val leaderboard: List<LeaderboardEntity> = emptyList(),
    val selectedFilter: String = "Weekly XP",
    val currentUserRank: Int = 0,
    val currentUserEntry: LeaderboardEntity? = null,
    val isLoading: Boolean = true,
    /**
     * The learner's own avatar, from their progress rather than the leaderboard row, so the rank
     * card can show it even while they are not ranked (guests never are).
     */
    val myAvatarId: Int? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    /** Collector for the active filter; replaced (not stacked) on every switch. */
    private var filterJob: Job? = null

    init {
        setFilter("Weekly XP")
        viewModelScope.launch {
            userProgressRepository.getUserProgress().collect { progress ->
                _uiState.value = _uiState.value.copy(myAvatarId = progress?.profileIconId)
            }
        }
    }

    fun setFilter(filter: String) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedFilter = filter, isLoading = true)
            
            val flow = when (filter) {
                "Streak Masters" -> leaderboardRepository.getLeaderboardByStreak()
                "Weekly XP" -> leaderboardRepository.getWeeklyLeaderboard()
                else -> leaderboardRepository.getLeaderboardByXp()
            }

            flow.collect { list ->
                val userIndex = list.indexOfFirst { it.isCurrentUser }
                val userRank = if (userIndex != -1) list[userIndex].rank else 0
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
