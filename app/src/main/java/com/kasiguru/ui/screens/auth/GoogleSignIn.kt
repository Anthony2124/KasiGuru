package com.kasiguru.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Returns a launcher for Google sign-in, or null when the project has no OAuth
 * client configured yet (no `default_web_client_id` resource). Returning null
 * lets the UI hide the button instead of showing one that always fails; it
 * lights up on its own once google-services.json includes an OAuth client.
 */
@Composable
fun rememberGoogleSignIn(onIdToken: (String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val webClientId = remember {
        val resId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        if (resId == 0) null else context.getString(resId)
    } ?: return null

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            account.idToken?.let(onIdToken)
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Google sign-in failed", e)
        }
    }

    val client = remember(webClientId) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        )
    }

    return {
        // Sign out of the Google client first so the account picker always shows,
        // rather than silently reusing the last account on a shared device.
        client.signOut().addOnCompleteListener { launcher.launch(client.signInIntent) }
    }
}
