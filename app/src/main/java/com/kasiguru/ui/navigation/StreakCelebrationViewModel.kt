package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the app-wide daily streak activation celebration.
 * When a learner completes all daily requirements (review words + 3 mini-games),
 * the streak activation moment triggers across the navigation shell.
 */
@HiltViewModel
class StreakCelebrationViewModel @Inject constructor(
    userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _pendingStreakActivation = MutableStateFlow<Int?>(null)
    val pendingStreakActivation: StateFlow<Int?> = _pendingStreakActivation.asStateFlow()

    init {
        viewModelScope.launch {
            userProgressRepository.streakActivatedEvents.collect { streakDays ->
                _pendingStreakActivation.value = streakDays
            }
        }
    }

    fun dismiss() {
        _pendingStreakActivation.value = null
    }
}
