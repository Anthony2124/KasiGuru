package com.kasiguru.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.R
import com.kasiguru.ui.components.ErrorDialog
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
    onAuthSuccess: (() -> Unit)? = null,
    initialSignInMode: Boolean = true,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignInMode by remember { mutableStateOf(initialSignInMode) }
    var passwordVisible by remember { mutableStateOf(false) }

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

    uiState.error?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    LaunchedEffect(uiState.didSucceed) {
        if (uiState.didSucceed) {
            onAuthSuccess?.invoke()
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
        title = "Account & Sign In",
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
                        title = "Account & Sign In",
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
                        // Mode Switcher: Log In vs Create Account
                        AuthSegmentedSwitcher(
                            isSignInMode = isSignInMode,
                            onModeChange = { isSignInMode = it }
                        )

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
                                    "Sign in to restore your cloud progress, XP, streak, and badges."
                                } else {
                                    "Your current device progress will be securely attached to your new account."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                            Spacer(Modifier.height(Space.md))

                            if (googleSignIn != null) {
                                OutlinedButton(
                                    onClick = googleSignIn,
                                    enabled = !uiState.isBusy,
                                    shape = Shapes.pill,
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface),
                                    border = BorderStroke(1.dp, SurfaceSunken),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google_g),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(Space.sm))
                                    Text("Continue with Google", color = Ink, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(Space.md))
                                AuthDivider(text = "or with email")
                                Spacer(Modifier.height(Space.md))
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email address") },
                                singleLine = true,
                                enabled = !uiState.isBusy,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Sms),
                                        contentDescription = null,
                                        tint = Violet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
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
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Lock),
                                        contentDescription = null,
                                        tint = Violet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            tint = Muted
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = Shapes.tile,
                                colors = accountFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isSignInMode) {
                                Spacer(Modifier.height(Space.xxs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.sendPasswordReset(email) }) {
                                        Text(
                                            text = "Forgot password?",
                                            color = Violet,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(Space.sm))
                            }

                            Spacer(Modifier.height(Space.xs))
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

                            Spacer(Modifier.height(Space.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSignInMode) "Don't have an account?" else "Already have an account?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                                Spacer(Modifier.width(Space.xxs))
                                TextButton(onClick = { isSignInMode = !isSignInMode }) {
                                    Text(
                                        text = if (isSignInMode) "Create one" else "Sign in",
                                        color = Violet,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Benefits Card
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Why sync your KasiGuru account?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Spacer(Modifier.height(Space.sm))
                            AuthBenefitRow(
                                iconRes = Iconsax.TickCircle,
                                title = "Protect your hard work",
                                subtitle = "Your streak, XP, badges, and review progress are backed up safely."
                            )
                            Spacer(Modifier.height(Space.sm))
                            AuthBenefitRow(
                                iconRes = Iconsax.Global,
                                title = "Learn on any device",
                                subtitle = "Log in on a new phone and immediately resume where you left off."
                            )
                            Spacer(Modifier.height(Space.sm))
                            AuthBenefitRow(
                                iconRes = Iconsax.Cup,
                                title = "Compete on leaderboards",
                                subtitle = "Share your Kasiguranin learning rank with your community."
                            )
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
private fun AuthSegmentedSwitcher(
    isSignInMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Surface(
        shape = Shapes.pill,
        color = SurfaceSunken,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(Shapes.pill)
                    .background(if (isSignInMode) Violet else Color.Transparent)
                    .clickable { onModeChange(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSignInMode) Color.White else Muted
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(Shapes.pill)
                    .background(if (!isSignInMode) Violet else Color.Transparent)
                    .clickable { onModeChange(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (!isSignInMode) Color.White else Muted
                )
            }
        }
    }
}

@Composable
private fun AuthDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceSunken)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            modifier = Modifier.padding(horizontal = Space.sm)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceSunken)
    }
}

@Composable
private fun AuthBenefitRow(
    iconRes: Int,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Violet.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
    }
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
                        else -> "Saved on this device only. Sign in to protect your progress."
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
