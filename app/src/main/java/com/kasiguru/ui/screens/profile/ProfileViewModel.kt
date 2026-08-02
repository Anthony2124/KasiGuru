package com.kasiguru.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userProgress: UserProgressEntity? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userProgressRepository.getUserProgress().collect { progress ->
                _uiState.value = _uiState.value.copy(
                    userProgress = progress,
                    isLoading = false
                )
            }
        }
    }

    fun updateProfile(fullName: String, age: Int?, address: String, iconId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                userProgressRepository.updateProfileDetails(fullName, age, address, iconId)
                _uiState.value = _uiState.value.copy(isSaving = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Failed to update profile.")
            }
        }
    }
}
