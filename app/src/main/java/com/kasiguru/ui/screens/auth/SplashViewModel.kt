package com.kasiguru.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.ProfileRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.tour.CurrentTutorialVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val profileRepository: ProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val progress = userProgressRepository.getUserProgressOnce()

            // Stamp the tutorial baseline once, here, because this is the one place that already
            // knows whether this install predates the chapters. Someone who had finished onboarding
            // before chapters existed gets today's version as their baseline, so the six chapters
            // shipping now are launchable but not badged; anything added in a later release is
            // genuinely new to them and does badge. A fresh install gets 0 and badges everything.
            userPreferencesRepository.ensureTourBaseline(
                if (progress?.isOnboardingCompleted == true) CurrentTutorialVersion else 0
            )

            _startDestination.value = when {
                progress == null || !progress.isOnboardingCompleted -> Screen.Onboarding.route
                // Only asked when there is more than one profile to choose between - a
                // single-profile device (by far the common case) skips straight to Learn exactly
                // as it always has.
                profileRepository.getAllProfiles().first().size > 1 -> Screen.ProfileSelection.route
                else -> Screen.Learn.route
            }
        }
    }
}
