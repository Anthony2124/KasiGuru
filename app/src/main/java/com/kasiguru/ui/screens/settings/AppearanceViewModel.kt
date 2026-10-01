package com.kasiguru.ui.screens.settings

import androidx.lifecycle.*
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.domain.preferences.AppearanceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val preferences: UserPreferencesRepository) : ViewModel() {
    val mode = preferences.appearanceMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceMode.SYSTEM)
    val textSize = preferences.textSizePercent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)
    fun setMode(mode: AppearanceMode) { viewModelScope.launch { preferences.setAppearanceMode(mode) } }
    fun setTextSize(percent: Int) { viewModelScope.launch { preferences.setTextSizePercent(percent) } }
}
