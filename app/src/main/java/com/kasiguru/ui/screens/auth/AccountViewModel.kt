package com.kasiguru.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.repository.AccountState
import com.kasiguru.data.repository.AuthOutcome
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.UserDataResetManager
import com.kasiguru.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AccountUiState(
    val account: AccountState = AccountState(null, isAnonymous = true, email = null),
    val isBusy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /**
     * Set when the entered credentials already belong to another account.
     * Signing in would leave this device's guest progress behind, so the user
     * confirms before we switch.
     */
    val pendingSignIn: AuthCredential? = null,
    val didSucceed: Boolean = false,
    /** True once account deletion has actually completed — the UI navigates away on this. */
    val didDeleteAccount: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: UserPreferencesRepository,
    private val database: KasiGuruDatabase,
    private val userDataResetManager: UserDataResetManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(account = authRepository.currentAccount()))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.accountState.collect { account ->
                _uiState.value = _uiState.value.copy(account = account)
            }
        }
    }

    fun createOrLinkAccount(email: String, password: String) {
        if (!validate(email, password)) return
        run { authRepository.linkEmailPassword(email, password) }
    }

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        run { authRepository.signInEmailPassword(email, password) }
    }

    fun linkGoogle(idToken: String) = run { authRepository.linkGoogle(idToken) }

    /** Proceeds with the sign-in the user confirmed after a collision. */
    fun confirmSignIn() {
        val credential = _uiState.value.pendingSignIn ?: return
        _uiState.value = _uiState.value.copy(pendingSignIn = null)
        run { authRepository.signIn(credential) }
    }

    fun cancelSignIn() {
        _uiState.value = _uiState.value.copy(pendingSignIn = null)
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your email first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            val result = authRepository.sendPasswordReset(email)
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                message = if (result.isSuccess) "Password reset email sent." else null,
                error = result.exceptionOrNull()?.let { "Could not send reset email." }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            userDataResetManager.resetAllLocalUserData()
            authRepository.signOutToAnonymous()
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                message = "Signed out. Sign back in any time to restore your progress."
            )
        }
    }

    fun dismissBackupPrompt() {
        viewModelScope.launch { preferences.setBackupPromptDismissed(true) }
    }

    /**
     * Permanently deletes the account and every document it owns, then clears local
     * data and starts a fresh anonymous session — unlike signOut(), the local copy
     * must not survive, or a new anonymous session would inherit the deleted
     * account's XP/streak/words on its next sync.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            val result = authRepository.deleteAccount()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: "Couldn't delete your account. Check your connection and try again."
                )
                return@launch
            }
            userDataResetManager.resetAllLocalUserData()
            authRepository.signOutToAnonymous()
            _uiState.value = _uiState.value.copy(isBusy = false, didDeleteAccount = true)
        }
    }

    fun consumeMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            message = null,
            didSucceed = false,
            didDeleteAccount = false
        )
    }

    private fun validate(email: String, password: String): Boolean {
        val problem = when {
            email.isBlank() -> "Enter your email address."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "That email doesn't look right."
            password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
        if (problem != null) _uiState.value = _uiState.value.copy(error = problem)
        return problem == null
    }

    private fun run(block: suspend () -> AuthOutcome) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null)
            when (val outcome = block()) {
                is AuthOutcome.Linked -> _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    didSucceed = true,
                    message = "Account secured. Your progress is now saved to it."
                )
                is AuthOutcome.SignedIn -> _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    didSucceed = true,
                    message = "Signed in. Restoring your progress…"
                )
                is AuthOutcome.AlreadyRegistered -> _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    pendingSignIn = outcome.credential
                )
                is AuthOutcome.Failed -> _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = outcome.message
                )
            }
        }
    }
}
