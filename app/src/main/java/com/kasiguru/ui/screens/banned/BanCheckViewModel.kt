package com.kasiguru.ui.screens.banned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.BanCheckRepository
import com.kasiguru.data.repository.BanStatus
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
    /** Active ban: show the AccountSuspendedScreen with this reason. */
    data class Banned(val reason: String, val bannedAt: Long) : BanState
}

@HiltViewModel
class BanCheckViewModel @Inject constructor(
    private val banCheckRepository: BanCheckRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _banState = MutableStateFlow<BanState>(BanState.Loading)
    val banState: StateFlow<BanState> = _banState.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

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
                                bannedAt = status.bannedAt
                            )
                        }
                    }
                }
            }
        }
    }

    /** Signs the user out to a fresh anonymous session so they can still use the public features. */
    fun signOut() {
        viewModelScope.launch {
            _isSigningOut.value = true
            authRepository.signOutToAnonymous()
            _isSigningOut.value = false
        }
    }
}
