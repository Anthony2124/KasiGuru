package com.kasiguru.ui.screens.banned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.BanCheckRepository
import com.kasiguru.data.repository.BanStatus
import com.kasiguru.data.repository.UserDataResetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the ban-check gate and the suspended screen. */
sealed interface BanState {
    /** Haven't heard back from Firestore yet. */
    data object Loading : BanState
    /** No active ban — the app should proceed normally. */
    data object Clear : BanState
    /** Active ban: show the AccountSuspendedScreen with this reason and appeal details. */
    data class Banned(
        val reason: String,
        val bannedAt: Long,
        val appealText: String? = null,
        val appealSubmittedAt: Long? = null,
        val appealStatus: String? = null,
        val appealReviewNotes: String? = null,
        val appealReviewedAt: Long? = null
    ) : BanState
}

@HiltViewModel
class BanCheckViewModel @Inject constructor(
    private val banCheckRepository: BanCheckRepository,
    private val authRepository: AuthRepository,
    private val userDataResetManager: UserDataResetManager
) : ViewModel() {

    private val _banState = MutableStateFlow<BanState>(BanState.Loading)
    val banState: StateFlow<BanState> = _banState.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _isSubmittingAppeal = MutableStateFlow(false)
    val isSubmittingAppeal: StateFlow<Boolean> = _isSubmittingAppeal.asStateFlow()

    private val _appealError = MutableStateFlow<String?>(null)
    val appealError: StateFlow<String?> = _appealError.asStateFlow()

    fun clearAppealError() {
        _appealError.value = null
    }

    init {
        // Observe auth state changes in real time.
        // Whenever the user starts the app, logs in, links an account, or creates a session,
        // we dynamically attach a real-time Firestore listener to their UID.
        viewModelScope.launch {
            authRepository.accountState.collectLatest { account ->
                val uid = account.uid
                if (uid == null) {
                    _banState.value = BanState.Clear
                } else {
                    banCheckRepository.observeBan(uid).collect { status ->
                        _banState.value = when (status) {
                            is BanStatus.Clear -> BanState.Clear
                            is BanStatus.Banned -> BanState.Banned(
                                reason = status.reason,
                                bannedAt = status.bannedAt,
                                appealText = status.appealText,
                                appealSubmittedAt = status.appealSubmittedAt,
                                appealStatus = status.appealStatus,
                                appealReviewNotes = status.appealReviewNotes,
                                appealReviewedAt = status.appealReviewedAt
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Submits an appeal for the currently suspended user.
     */
    fun submitAppeal(appealText: String, onComplete: (Boolean) -> Unit = {}) {
        val uid = authRepository.currentAccount().uid
        if (uid.isNullOrBlank()) {
            _appealError.value = "No active user session found."
            onComplete(false)
            return
        }
        val trimmed = appealText.trim()
        if (trimmed.length < 10) {
            _appealError.value = "Please provide at least 10 characters explaining your appeal."
            onComplete(false)
            return
        }
        if (trimmed.length > 1000) {
            _appealError.value = "Appeal must be 1000 characters or fewer."
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isSubmittingAppeal.value = true
            _appealError.value = null
            val result = banCheckRepository.submitAppeal(uid, trimmed)
            _isSubmittingAppeal.value = false
            if (result.isSuccess) {
                onComplete(true)
            } else {
                val msg = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit appeal. Please try again."
                _appealError.value = msg
                onComplete(false)
            }
        }
    }

    /**
     * Signs the user out of the suspended account.
     * Clears all local user progress, profile, and session data so the banned
     * account's info does not remain in local storage, and establishes a clean
     * anonymous guest session.
     */
    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSigningOut.value = true
            try {
                // Wipe local Room tables, DataStore preferences, and sync cache.
                // uploadPendingChanges = false so we don't attempt to push data to the banned account.
                userDataResetManager.resetAllLocalUserData(uploadPendingChanges = false)
                // Sign out of Firebase and start a clean anonymous session
                authRepository.signOutToAnonymous()
            } catch (e: Exception) {
                android.util.Log.e("BanCheckViewModel", "Failed to reset data on suspended sign out", e)
            } finally {
                _isSigningOut.value = false
                onComplete()
            }
        }
    }
}
