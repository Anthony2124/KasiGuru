package com.kasiguru.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.R
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*

/**
 * Turns a guest (anonymous) session into a permanent account, or signs an
 * existing account back in after a reinstall / on another device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignInMode by remember { mutableStateOf(false) }

    val googleSignIn = rememberGoogleSignIn(onIdToken = { viewModel.linkGoogle(it) })

    val hasUnsavedChanges = !uiState.account.isRecoverable && (email.isNotBlank() || password.isNotBlank())
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val attemptBack: () -> Unit = { if (hasUnsavedChanges) showDiscardConfirm = true else onNavigateBack() }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardConfirm = true }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this?", fontWeight = FontWeight.Bold) },
            text = { Text("What you've typed hasn't been submitted yet.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onNavigateBack() }) {
                    Text("Discard", fontWeight = FontWeight.Bold, color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep editing") }
            }
        )
    }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessages()
        }
    }

    LaunchedEffect(uiState.didDeleteAccount) {
        if (uiState.didDeleteAccount) {
            snackbarHostState.showSnackbar("Account deleted. You're starting fresh as a guest.")
            viewModel.consumeMessages()
        }
    }

    uiState.pendingSignIn?.let {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSignIn() },
            title = { Text("Account already exists", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "An account already uses those details. Signing in loads that account's " +
                        "saved progress and combines it with what is on this device — for each " +
                        "stat the higher value is kept, so nothing is lost."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSignIn() }) {
                    Text("Sign in", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelSignIn() }) { Text("Cancel") }
            }
        )
    }

    GroundScaffold(
        title = "Account",
        subtitle = "Keep your progress safe across devices",
        onBack = attemptBack,
        pattern = GroundPattern.Orbs,
        content = {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.gutter),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    GroundTitleBlock(
                        title = "Account",
                        subtitle = "Keep your progress safe across devices"
                    )
                    Spacer(Modifier.height(Space.xs))

                    AccountStatusCard(
                        isRecoverable = uiState.account.isRecoverable,
                        email = uiState.account.email,
                        providers = uiState.account.providers
                    )

                    if (uiState.account.isRecoverable) {
                        SignedInActions(
                            isBusy = uiState.isBusy,
                            onSignOut = { viewModel.signOut() }
                        )
                    } else {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isSignInMode) "Sign in to your account" else "Create your account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Spacer(Modifier.height(Space.xxs))
                            Text(
                                text = if (isSignInMode) {
                                    "Already have an account? Sign in to load its progress on this device."
                                } else {
                                    "Your current progress stays exactly as it is and gets attached to the new account."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                            Spacer(Modifier.height(Space.md))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                enabled = !uiState.isBusy,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                shape = Shapes.tile,
                                colors = accountFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(Space.sm))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                enabled = !uiState.isBusy,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = Shapes.tile,
                                colors = accountFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(Space.md))
                            ClayButton(
                                label = if (isSignInMode) "Sign In" else "Create Account",
                                onClick = {
                                    if (isSignInMode) viewModel.signIn(email, password)
                                    else viewModel.createOrLinkAccount(email, password)
                                },
                                enabled = !uiState.isBusy,
                                tone = ClayButtonTone.Primary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (googleSignIn != null) {
                                Spacer(Modifier.height(Space.sm))
                                OutlinedButton(
                                    onClick = googleSignIn,
                                    enabled = !uiState.isBusy,
                                    shape = Shapes.tile,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google_g),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(Space.xs))
                                    Text("Continue with Google", color = Ink, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(Space.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isSignInMode = !isSignInMode }) {
                                    Text(
                                        text = if (isSignInMode) "Create an account" else "I already have one",
                                        color = Violet,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isSignInMode) {
                                    TextButton(onClick = { viewModel.sendPasswordReset(email) }) {
                                        Text("Forgot password?", color = Muted)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(Space.lg))
                    DeleteAccountSection(
                        isBusy = uiState.isBusy,
                        onDelete = { viewModel.deleteAccount() }
                    )

                    Spacer(Modifier.height(Space.navBarClearance))
                }

                SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    )
}

@Composable
private fun accountFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = Violet,
    unfocusedBorderColor = SurfaceSunken
)

@Composable
private fun AccountStatusCard(
    isRecoverable: Boolean,
    email: String?,
    providers: List<String>
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = Shapes.chip,
                color = if (isRecoverable) Green.copy(alpha = 0.15f) else Warning.copy(alpha = 0.2f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            id = if (isRecoverable) Iconsax.TickCircle else Iconsax.InfoCircle
                        ),
                        contentDescription = null,
                        tint = if (isRecoverable) Green else Warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = if (isRecoverable) "Progress protected" else "Guest progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    text = when {
                        isRecoverable && email != null -> "Signed in as $email"
                        isRecoverable && providers.contains("google.com") -> "Signed in with Google"
                        isRecoverable -> "Signed in"
                        else -> "Saved on this device only. Uninstalling loses it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun SignedInActions(isBusy: Boolean, onSignOut: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Sign out?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your progress is saved to your account. Sign back in on any device " +
                        "to pick up where you left off."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onSignOut()
                }) { Text("Sign out", fontWeight = FontWeight.Bold, color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            }
        )
    }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your progress syncs automatically",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
        Text(
            text = "XP, streaks, badges, level stars and word reviews are saved to your " +
                "account and restored whenever you sign in.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(Space.md))
        OutlinedButton(
            onClick = { confirming = true },
            enabled = !isBusy,
            shape = Shapes.tile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = Iconsax.Logout),
                contentDescription = null,
                tint = Red,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Space.xs))
            Text("Sign Out", color = Red, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Available whether the account is a guest or fully signed in — a guest's own local
 * progress and any partial cloud doc are just as real to delete. Kept visually
 * subordinate to everything above it: this is the one truly irreversible action
 * on the whole screen.
 */
@Composable
private fun DeleteAccountSection(isBusy: Boolean, onDelete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Delete your account?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes your XP, streaks, badges, and word progress " +
                        "from KasiGuru's servers, and can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text("Delete permanently", fontWeight = FontWeight.Bold, color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            }
        )
    }

    Text(
        text = "Danger zone",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Muted
    )
    Spacer(Modifier.height(Space.xs))
    TextButton(
        onClick = { confirming = true },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete my account and data", color = Red, fontWeight = FontWeight.Bold)
    }
}
