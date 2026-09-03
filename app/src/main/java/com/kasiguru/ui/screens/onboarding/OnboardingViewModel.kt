package com.kasiguru.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.ProfileRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val profileRepository: ProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    fun completeOnboarding(
        userName: String,
        avatarId: Int,
        dailyGoalXp: Int,
        titleBadge: String,
        residentName: String
    ) {
        // NonCancellable, because this view model does not outlive the call that starts it. The
        // screen navigates to Learn with popUpTo(Onboarding) { inclusive = true } the moment this
        // returns, which destroys the Onboarding back stack entry and cancels viewModelScope
        // mid-flight. The first two writes were winning that race by luck; the third, added later,
        // lost it every time and the guided tour silently never ran. Nothing here is cancellable
        // work - it is the record that the learner finished the wizard.
        viewModelScope.launch {
            withContext(NonCancellable) {
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

                // Someone who just finished the wizard is owed the guided tour of the interface they are
                // about to land in. Marked here rather than defaulting a "seen" flag to false, so that
                // updating the app never ambushes an existing learner with a tour they did not ask for.
                userPreferencesRepository.setTutorialPending(true)
            }
        }
    }
}
