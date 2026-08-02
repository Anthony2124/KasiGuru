package com.kasiguru.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.util.EmailService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _showOtpDialog = MutableStateFlow(false)
    val showOtpDialog: StateFlow<Boolean> = _showOtpDialog.asStateFlow()

    private var generatedOtp: String = ""
    private var pendingUsername = ""
    private var pendingPassword = ""
    private var pendingEmail = ""

    fun registerUser(username: String, password: String, confirmPassword: String, email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            when {
                username.length < 3 -> {
                    _uiState.value = AuthUiState(error = "Username must be at least 3 characters.")
                }
                password.length < 6 -> {
                    _uiState.value = AuthUiState(error = "Password must be at least 6 characters.")
                }
                password != confirmPassword -> {
                    _uiState.value = AuthUiState(error = "Passwords do not match. Please try again.")
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    _uiState.value = AuthUiState(error = "Please enter a valid email address.")
                }
                else -> {
                    // Generate 6 digit OTP
                    generatedOtp = String.format("%06d", Random.nextInt(999999))
                    pendingUsername = username
                    pendingPassword = password
                    pendingEmail = email

                    val emailSent = EmailService.sendOtpEmail(email, generatedOtp)
                    if (emailSent) {
                        _showOtpDialog.value = true
                        _uiState.value = AuthUiState(isLoading = false)
                    } else {
                        _uiState.value = AuthUiState(error = "Failed to send OTP. Check your EmailService credentials or internet connection.")
                    }
                }
            }
        }
    }

    fun verifyOtp(enteredOtp: String) {
        if (enteredOtp == generatedOtp) {
            viewModelScope.launch {
                userProgressRepository.registerUser(pendingUsername, pendingPassword, pendingEmail)
                _showOtpDialog.value = false
                _uiState.value = AuthUiState(isSuccess = true)
            }
        } else {
            _uiState.value = AuthUiState(error = "Incorrect OTP code.")
        }
    }

    fun dismissOtpDialog() {
        _showOtpDialog.value = false
    }
}
