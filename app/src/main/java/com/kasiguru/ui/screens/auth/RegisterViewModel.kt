package com.kasiguru.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun registerUser(fullName: String, ageStr: String, address: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            val trimmedName = fullName.trim()
            val parsedAge = ageStr.trim().toIntOrNull()
            val trimmedAddress = address.trim()

            if (trimmedName.length < 2) {
                _uiState.value = AuthUiState(error = "Please enter your full name.")
                return@launch
            }

            userProgressRepository.registerSimpleUser(
                fullName = trimmedName,
                age = parsedAge,
                address = trimmedAddress
            )

            _uiState.value = AuthUiState(isSuccess = true)
        }
    }
}
