package com.kasiguru.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.ProfileRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    fun completeOnboarding(
        userName: String,
        avatarId: Int,
        dailyGoalXp: Int,
        titleBadge: String,
        residentName: String
    ) {
        viewModelScope.launch {
            userProgressRepository.completeOnboarding(
                userName = userName,
                avatarId = avatarId,
                dailyGoalXp = dailyGoalXp,
                titleBadge = titleBadge
            )
            // Seeds the profile roster with this device's first profile, so a family that later
            // adds a second one already has this one to switch back to.
            val resolvedName = userName.ifBlank { "Kasiguranin Learner" }
            profileRepository.createProfile(name = resolvedName, residentName = residentName)
        }
    }
}
