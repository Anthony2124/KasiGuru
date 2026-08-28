package com.kasiguru.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
    suspend fun linkEmailPassword(email: String, password: String): AuthOutcome {
        val outcome = link(EmailAuthProvider.getCredential(email.trim(), password))
        // Only on a genuinely new account (not a sign-in, not a collision) — email
        // ownership was never confirmed before this, leaving password reset as the
        // only recovery path and it went to an address nobody had verified.
        if (outcome is AuthOutcome.Linked) {
            runCatching { auth.currentUser?.sendEmailVerification()?.await() }
        }
        return outcome
    }

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
     * A lightweight identity hint, not a secret: three fixed prompts (see
     * [SECURITY_QUESTIONS]) whose answers are stored owner-only at
     * `security_questions/{uid}`, the same per-account access shape `users/{uid}` already uses.
     *
     * This is deliberately NOT wired into [sendPasswordReset] as a pre-authentication gate.
     * Doing that would require the answers to be readable by someone who is not yet signed in as
     * this account, which either means making them public (defeats the point) or standing up a
     * Cloud Function to broker the check server-side (the Spark plan this project stays on
     * cannot deploy - see functions/index.js). Firebase's own emailed reset link already does
     * real, secure identity verification; these questions are a same-session confirmation step
     * for a user who is already signed in, e.g. before a sensitive change, not a substitute for
     * that link.
     */
    suspend fun saveSecurityQuestions(answers: List<String>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in.")
        require(answers.size == SECURITY_QUESTIONS.size) { "Expected ${SECURITY_QUESTIONS.size} answers." }
        val data = SECURITY_QUESTIONS.indices.associate { i -> "answer$i" to answers[i].trim() }
        firestore.collection("security_questions").document(uid)
            .set(data + mapOf("updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** Empty strings for any question never answered yet. */
    suspend fun getSecurityQuestionAnswers(): Result<List<String>> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in.")
        val doc = firestore.collection("security_questions").document(uid).get().await()
        SECURITY_QUESTIONS.indices.map { i -> doc.getString("answer$i") ?: "" }
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

    /**
     * Permanently deletes the current user's account and every Firestore document
     * keyed by their uid, done directly from the client (the project stays on the
     * free Spark plan, which can't run the Cloud Function this would otherwise be —
     * see functions/index.js). firestore.rules grants each user delete rights on
     * exactly these documents for this reason. Firestore data is removed before
     * the Auth user so a failed retry finds an intact, still-authenticatable
     * account rather than an orphaned one. Leaves the app signed out; the caller
     * is responsible for starting a fresh anonymous session afterward (mirrors
     * signOutToAnonymous, kept separate since a failed deletion shouldn't
     * silently start a new session over the still-intact old one).
     */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in.")
        val uid = user.uid
        val progress = firestore.collection("users").document(uid).collection("progress")
        // Taken from the shared list rather than spelled out here. Written out, this loop
        // omitted `lessonProgress` — added to the sync manager long after this was written —
        // so every deleted account left its lesson history behind in the cloud.
        for (docId in ProgressDocuments.ALL) {
            progress.document(docId).delete().await()
        }
        firestore.collection("leaderboard_public").document(uid).delete().await()
        firestore.collection("device_tokens").document(uid).delete().await()
        try {
            user.delete().await()
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            throw IllegalStateException("Please sign out and back in, then try deleting your account again.", e)
        }
        Unit
    }

    private fun com.google.firebase.auth.FirebaseUser?.toAccountState() = AccountState(
        uid = this?.uid,
        isAnonymous = this?.isAnonymous ?: true,
        email = this?.email?.takeIf { it.isNotBlank() },
        providers = this?.providerData?.map { it.providerId }?.filter { it != "firebase" } ?: emptyList()
    )

    companion object {
        val SECURITY_QUESTIONS = listOf(
            "What was the name of your barangay or hometown?",
            "What is the name of a family member who still speaks Kasiguranin?",
            "What was your first pet's name?"
        )
    }
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
