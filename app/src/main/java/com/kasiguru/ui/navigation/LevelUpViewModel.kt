package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.util.gamification.GamificationEngine
import com.kasiguru.util.gamification.LevelInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the app-wide level-up celebration: XP is earned from Lesson Player, Flashcards, and all six
 * mini-games, so the moment belongs to the navigation shell rather than to any one screen. This is now
 * the only view model the shell owns — the continue action moved onto Learn when the docked FAB was
 * removed, and its view model went with it. Before this existed, `LevelUpDialog` was a fully
 * built component with no caller anywhere in the app — reaching a new level produced no in-app moment
 * at all.
 */
@HiltViewModel
class LevelUpViewModel @Inject constructor(
    userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _pendingLevelUp = MutableStateFlow<LevelInfo?>(null)
    val pendingLevelUp: StateFlow<LevelInfo?> = _pendingLevelUp.asStateFlow()

    init {
        viewModelScope.launch {
            userProgressRepository.levelUpEvents.collect { newLevel ->
                _pendingLevelUp.value = GamificationEngine.LEVELS.firstOrNull { it.level == newLevel }
            }
        }
    }

    fun dismiss() {
        _pendingLevelUp.value = null
    }
}
