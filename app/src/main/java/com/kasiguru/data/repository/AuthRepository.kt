package com.kasiguru.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The identity a user's cloud data is keyed by.
 *
 * [uid] is stable across a link (anonymous -> permanent keeps the same uid), which
 * is what lets an existing player keep their progress when they add credentials.
 */
data class AccountState(
    val uid: String?,
    val isAnonymous: Boolean,
    val email: String?,
    val providers: List<String> = emptyList()
) {
    val isSignedIn: Boolean get() = uid != null
    /** Anonymous progress is lost on uninstall: the uid cannot be re-obtained. */
    val isRecoverable: Boolean get() = uid != null && !isAnonymous
}

/** Outcome of linking/sign-in, so the UI can react without knowing Firebase types. */
sealed interface AuthOutcome {
    /** Credentials attached to the current uid; progress carried over untouched. */
    data object Linked : AuthOutcome
    /** Signed in to a pre-existing account; its cloud progress will be pulled down. */
    data object SignedIn : AuthOutcome
    /**
     * The credential already belongs to another account. Signing in would abandon
     * the current anonymous progress, so the choice is handed to the user.
     */
    data class AlreadyRegistered(val credential: AuthCredential) : AuthOutcome
    data class Failed(val message: String) : AuthOutcome
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val accountState: Flow<AccountState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toAccountState()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentAccount(): AccountState = auth.currentUser.toAccountState()

    /**
     * Makes the current anonymous session permanent, keeping the same uid so no
     * progress has to be moved. Falls back to reporting a collision when the
     * credential is already registered, rather than silently switching accounts.
     */
    suspend fun linkEmailPassword(email: String, password: String): AuthOutcome =
        link(EmailAuthProvider.getCredential(email.trim(), password))

    suspend fun linkGoogle(idToken: String): AuthOutcome =
        link(GoogleAuthProvider.getCredential(idToken, null))

    private suspend fun link(credential: AuthCredential): AuthOutcome {
        val user = auth.currentUser
        // No anonymous session to upgrade (e.g. sign-in failed at startup): just sign in.
        if (user == null || !user.isAnonymous) return signIn(credential)

        return try {
            user.linkWithCredential(credential).await()
            AuthOutcome.Linked
        } catch (e: FirebaseAuthUserCollisionException) {
            AuthOutcome.AlreadyRegistered(credential)
        } catch (e: Exception) {
            AuthOutcome.Failed(e.friendlyMessage())
        }
    }

    /**
     * Signs in to an existing account. Used after a reinstall or on a second
     * device; ProgressSyncManager reacts to the uid change and pulls that
     * account's saved progress down.
     */
    suspend fun signInEmailPassword(email: String, password: String): AuthOutcome =
        signIn(EmailAuthProvider.getCredential(email.trim(), password))

    suspend fun signInGoogle(idToken: String): AuthOutcome =
        signIn(GoogleAuthProvider.getCredential(idToken, null))

    suspend fun signIn(credential: AuthCredential): AuthOutcome = try {
        auth.signInWithCredential(credential).await()
        AuthOutcome.SignedIn
    } catch (e: Exception) {
        AuthOutcome.Failed(e.friendlyMessage())
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /**
     * Returns to a fresh anonymous session so the app still works signed out.
     * Local Room data is deliberately left alone here; the caller decides
     * whether to clear it (see AuthViewModel.signOut).
     */
    suspend fun signOutToAnonymous(): Result<Unit> = runCatching {
        auth.signOut()
        auth.signInAnonymously().await()
        Unit
    }

    private fun com.google.firebase.auth.FirebaseUser?.toAccountState() = AccountState(
        uid = this?.uid,
        isAnonymous = this?.isAnonymous ?: true,
        email = this?.email?.takeIf { it.isNotBlank() },
        providers = this?.providerData?.map { it.providerId }?.filter { it != "firebase" } ?: emptyList()
    )
}

private fun Exception.friendlyMessage(): String = when (this) {
    is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
        "Password is too weak. Use at least 6 characters."
    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
        "That email or password looks incorrect."
    is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
        "No account found for that email."
    is FirebaseAuthUserCollisionException ->
        "An account already exists for that email."
    is com.google.firebase.FirebaseNetworkException ->
        "No internet connection. Please try again."
    else -> message ?: "Something went wrong. Please try again."
}
