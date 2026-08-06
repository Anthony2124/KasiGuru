package com.kasiguru.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    fun completeOnboarding(
        userName: String,
        avatarId: Int,
        dailyGoalXp: Int,
        titleBadge: String
    ) {
        viewModelScope.launch {
            userProgressRepository.completeOnboarding(
                userName = userName,
                avatarId = avatarId,
                dailyGoalXp = dailyGoalXp,
                titleBadge = titleBadge
            )
        }
    }
}
