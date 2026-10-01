package com.kasiguru.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FeedbackPreferencesViewModel @Inject constructor(preferences: UserPreferencesRepository) : ViewModel() {
    val soundEnabled = preferences.soundEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hapticsEnabled = preferences.hapticsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
