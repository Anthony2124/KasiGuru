package com.kasiguru.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.ProfileRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.ui.navigation.Screen
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
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val progress = userProgressRepository.getUserProgressOnce()
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
