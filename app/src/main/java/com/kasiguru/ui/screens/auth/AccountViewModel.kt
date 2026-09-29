package com.kasiguru.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.repository.AccountState
import com.kasiguru.data.repository.AuthOutcome
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.ProgressSyncManager
import com.kasiguru.data.repository.UserDataResetManager
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
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
    val didDeleteAccount: Boolean = false,
    /**
     * Whether name, age and address are on file. Null until the progress row has been read, so
     * the screen does not flash the details form at someone who already filled it in.
     */
    val hasPersonalDetails: Boolean? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: UserPreferencesRepository,
    private val database: KasiGuruDatabase,
    private val userDataResetManager: UserDataResetManager,
    private val userProgressRepository: UserProgressRepository,
    private val progressSyncManager: ProgressSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(account = authRepository.currentAccount()))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.accountState.collect { account ->
                _uiState.value = _uiState.value.copy(account = account)
            }
        }
        viewModelScope.launch {
            userProgressRepository.getUserProgress().collect { progress ->
                val complete = progress != null &&
                    progress.fullName.isNotBlank() &&
                    progress.age != null &&
                    progress.address.isNotBlank()
                _uiState.value = _uiState.value.copy(hasPersonalDetails = complete)
            }
        }
    }

    /**
     * Saves the one-time "About you" details. Once they are stored the form never shows again
     * and the sign-in options take its place; they sync to the account with the rest of progress.
     */
    fun savePersonalDetails(fullName: String, ageText: String, address: String) {
        val name = fullName.trim()
        val place = address.trim()
        val age = ageText.trim().toIntOrNull()
        val problem = when {
            name.isBlank() -> "Enter your name."
            name.length > MAX_NAME_LENGTH -> "Your name is too long."
            age == null -> "Enter your age."
            age !in MIN_AGE..MAX_AGE -> "Enter an age between $MIN_AGE and $MAX_AGE."
            place.isBlank() -> "Enter your address."
            place.length > MAX_ADDRESS_LENGTH -> "Your address is too long."
            else -> null
        }
        if (problem != null) {
            _uiState.value = _uiState.value.copy(error = problem)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            userProgressRepository.savePersonalDetails(name, age!!, place)
            _uiState.value = _uiState.value.copy(isBusy = false)
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
            // Flushes the local state to the account first: the promise in the message below
            // is only true if today's quota and review work actually reached the cloud before
            // this wipes them.
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
            // No flush here: deleteAccount() has already removed the cloud documents, and
            // re-uploading would resurrect the very data the user asked to be deleted.
            userDataResetManager.resetAllLocalUserData(uploadPendingChanges = false)
            authRepository.signOutToAnonymous()
            _uiState.value = _uiState.value.copy(isBusy = false, didDeleteAccount = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
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
                is AuthOutcome.Linked -> {
                    progressSyncManager.onAccountLinked()
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        didSucceed = true,
                        message = "Account secured. Your progress is now saved to it."
                    )
                }
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

    private companion object {
        // fullName is capped at 100 characters by firestore.rules; a longer one would make every
        // progress upload for this account fail validation.
        const val MAX_NAME_LENGTH = 100
        const val MAX_ADDRESS_LENGTH = 200
        const val MIN_AGE = 3
        const val MAX_AGE = 120
    }
}
