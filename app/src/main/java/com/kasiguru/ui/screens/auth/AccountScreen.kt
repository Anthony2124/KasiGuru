package com.kasiguru.ui.screens.auth

import com.kasiguru.ui.components.KasiGuruTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.kasiguru.ui.components.tapSounds

/**
 * Turns a guest (anonymous) session into a permanent account, or signs an
 * existing account back in after a reinstall / on another device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    onAuthSuccess: (() -> Unit)? = null,
    /**
     * After signing in to an existing account, or signing out / deleting the account. Every screen
     * behind this one was showing the previous person, so the caller starts over with a cleared
     * back stack instead of returning to them. Falls back to [onAuthSuccess] / staying put.
     */
    onAccountSwitched: (() -> Unit)? = null,
    onSignedOut: (() -> Unit)? = null,
    initialSignInMode: Boolean = true,
    /**
     * Reached from onboarding's "I already have an account": someone who already has an account and
     * only wants back in. They get a plain welcome-back sign-in and nothing else - no guest status,
     * no "about you" form, no create-account tab, no delete button.
     */
    returningLearner: Boolean = false,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignInMode by remember { mutableStateOf(initialSignInMode) }
    var passwordVisible by remember { mutableStateOf(false) }

    // The one-time "About you" step shown before sign-in when name, age or address is missing.
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    // A returning learner on a new device (or after signing out) has these on their account
    // already; signing in restores them, so they may go straight to sign-in for this visit.
    var skippedDetails by remember { mutableStateOf(false) }

    val googleSignIn = rememberGoogleSignIn(onIdToken = { viewModel.linkGoogle(it) })

    val hasUnsavedChanges = !uiState.account.isRecoverable && (
        email.isNotBlank() || password.isNotBlank() ||
            (uiState.hasPersonalDetails == false &&
                (fullName.isNotBlank() || age.isNotBlank() || address.isNotBlank()))
        )
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val attemptBack: () -> Unit = { if (hasUnsavedChanges) showDiscardConfirm = true else onNavigateBack() }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardConfirm = true }

    if (showDiscardConfirm) {
        AlertDialog(modifier = Modifier.tapSounds(), 
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this?", fontWeight = FontWeight.Bold) },
            text = { Text("What you've typed hasn't been submitted yet.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onNavigateBack() }) {
                    Text("Discard", fontWeight = FontWeight.Bold, color = RedText)
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
            val switched = uiState.didSwitchAccount
            if (switched && onAccountSwitched != null) onAccountSwitched() else onAuthSuccess?.invoke()
        }
    }

    LaunchedEffect(uiState.didSignOut) {
        if (uiState.didSignOut && onSignedOut != null) {
            viewModel.consumeMessages()
            onSignedOut()
        }
    }

    LaunchedEffect(uiState.didDeleteAccount) {
        if (uiState.didDeleteAccount) {
            if (onSignedOut != null) {
                viewModel.consumeMessages()
                onSignedOut()
            } else {
                snackbarHostState.showSnackbar("Account deleted. You're starting fresh as a guest.")
                viewModel.consumeMessages()
            }
        }
    }

    uiState.signOutSaveFailed?.let { reason ->
        AlertDialog(modifier = Modifier.tapSounds(),
            onDismissRequest = { viewModel.dismissSignOutFailure() },
            title = { Text("Progress not saved", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "$reason\n\nIf you sign out anyway, anything you did on this device since it " +
                        "last synced will be lost. Everything already saved stays on your account."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.signOut() }) {
                    Text("Try again", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.signOut(force = true) }) {
                    Text("Sign out anyway", color = RedText)
                }
            }
        )
    }

    // A returning learner asked to sign in; the "this account already exists" question is the
    // answer they came for, so it is accepted for them rather than asked.
    LaunchedEffect(uiState.pendingSignIn, returningLearner) {
        if (returningLearner && uiState.pendingSignIn != null) viewModel.confirmSignIn()
    }

    if (!returningLearner) uiState.pendingSignIn?.let {
        AlertDialog(modifier = Modifier.tapSounds(), 
            onDismissRequest = { viewModel.cancelSignIn() },
            title = { Text("Account already exists", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "An account already uses those details. Signing in opens that account " +
                        "exactly as it is saved — its name, details and progress stay the same. " +
                        "The guest progress on this device is replaced by the account's."
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
            if (returningLearner) {
                Box(Modifier.fillMaxSize()) {
                    ReturningSignIn(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        isBusy = uiState.isBusy,
                        onSignIn = { viewModel.signIn(email.trim(), password) },
                        onGoogle = googleSignIn,
                        onForgotPassword = { viewModel.sendPasswordReset(email.trim()) },
                        onStartNew = onNavigateBack
                    )
                    SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
                }
                return@GroundScaffold
            }
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
                    } else if (uiState.hasPersonalDetails == null) {
                        // Still reading the progress row; show nothing rather than the wrong card.
                    } else if (uiState.hasPersonalDetails == false && !skippedDetails && !isSignInMode) {
                        // Only creating an account asks for these. Signing in restores the ones the
                        // account already has, so a returning learner goes straight to the form.
                        PersonalDetailsCard(
                            isBusy = uiState.isBusy,
                            fullName = fullName,
                            onFullNameChange = { fullName = it },
                            age = age,
                            onAgeChange = { age = it },
                            address = address,
                            onAddressChange = { address = it },
                            onContinue = { viewModel.savePersonalDetails(fullName, age, address) },
                            onSkipToSignIn = {
                                skippedDetails = true
                                isSignInMode = true
                            }
                        )
                    } else {
                        // Mode Switcher: Log In vs Create Account
                        AuthSegmentedSwitcher(
                            isSignInMode = isSignInMode,
                            onModeChange = { isSignInMode = it }
                        )

                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            // Only when the details step was just completed on this visit.
                            if (fullName.isNotBlank() && !skippedDetails) {
                                Text(
                                    text = "Step 2 of 2 · Sign in",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LimeText
                                )
                                Spacer(Modifier.height(Space.xxs))
                            }
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

                            KasiGuruTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email address") },
                                singleLine = true,
                                enabled = !uiState.isBusy,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Sms),
                                        contentDescription = null,
                                        tint = LimeText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(Space.sm))
                            KasiGuruTextField(
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
                                        tint = LimeText,
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
                                keyboardActions = KeyboardActions(onDone = {
                                    if (isSignInMode) viewModel.signIn(email.trim(), password)
                                    else viewModel.createOrLinkAccount(email.trim(), password)
                                }),
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
                                            color = LimeText,
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
                                        color = LimeText,
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
                    .background(if (isSignInMode) Lime else Color.Transparent)
                    .clickable { onModeChange(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSignInMode) OnLime else Muted
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(Shapes.pill)
                    .background(if (!isSignInMode) Lime else Color.Transparent)
                    .clickable { onModeChange(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (!isSignInMode) OnLime else Muted
                )
            }
        }
    }
}

/**
 * Asked once, before the sign-in options: the learner's name, age and address. Saved to the local
 * progress row, from where they sync to the account like the rest of the profile and stay editable
 * in Edit profile. The full name becomes the leaderboard name; age and address are never published.
 */
@Composable
private fun PersonalDetailsCard(
    isBusy: Boolean,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkipToSignIn: () -> Unit
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Step 1 of 2 · About you",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = LimeText
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = "You only need to do this once. Next, you'll sign in with Google or email.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(Space.md))

        KasiGuruTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full name") },
            singleLine = true,
            enabled = !isBusy,
            leadingIcon = { DetailsFieldIcon(Iconsax.Profile) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Space.sm))
        KasiGuruTextField(
            value = age,
            onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) onAgeChange(it) },
            label = { Text("Age") },
            singleLine = true,
            enabled = !isBusy,
            leadingIcon = { DetailsFieldIcon(Iconsax.Calendar) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Space.sm))
        KasiGuruTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address") },
            placeholder = { Text("e.g. Barangay, Casiguran, Aurora") },
            enabled = !isBusy,
            leadingIcon = { DetailsFieldIcon(Iconsax.Location) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Space.md))
        ClayButton(
            label = "Continue",
            onClick = onContinue,
            enabled = !isBusy,
            tone = ClayButtonTone.Primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Space.xs))
        TextButton(
            onClick = onSkipToSignIn,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Already gave these before? Sign in to restore them",
                color = LimeText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DetailsFieldIcon(iconRes: Int) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = LimeText,
        modifier = Modifier.size(20.dp)
    )
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
                .background(Lime.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = LimeText,
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
    focusedBorderColor = Lime,
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
                        tint = if (isRecoverable) GreenText else Warning,
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
        AlertDialog(modifier = Modifier.tapSounds(), 
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
                }) { Text("Sign out", fontWeight = FontWeight.Bold, color = RedText) }
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
                tint = RedText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Space.xs))
            Text("Sign Out", color = RedText, fontWeight = FontWeight.Bold)
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
        AlertDialog(modifier = Modifier.tapSounds(), 
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
                }) { Text("Delete permanently", fontWeight = FontWeight.Bold, color = RedText) }
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
        Text("Delete my account and data", color = RedText, fontWeight = FontWeight.Bold)
    }
}

/** Loose on purpose: the server is the real check. This only stops an obvious typo being sent. */
private fun looksLikeEmail(text: String): Boolean {
    val t = text.trim()
    val at = t.indexOf('@')
    return at > 0 && t.indexOf('.', at) > at + 1 && !t.endsWith(".") && ' ' !in t
}

/**
 * "I already have an account", from onboarding: welcome back, one way in with Google, one with email,
 * and a way back for someone who tapped it by mistake.
 *
 * Built for the person it serves. They already know the app, so there is no pitch; they may be on a
 * new phone, so the copy says their progress comes with them; and every field says what is wrong
 * with it in place, so a typo is fixed where it was made instead of in a dialog.
 */
@Composable
private fun ReturningSignIn(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isBusy: Boolean,
    onSignIn: () -> Unit,
    onGoogle: (() -> Unit)?,
    onForgotPassword: () -> Unit,
    onStartNew: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var triedSubmit by remember { mutableStateOf(false) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val emailProblem = when {
        !triedSubmit && email.isBlank() -> null
        email.isBlank() -> "Enter the email you signed up with."
        !looksLikeEmail(email) -> "That doesn't look like an email address."
        else -> null
    }
    val passwordProblem = if (triedSubmit && password.isBlank()) "Enter your password." else null
    val submit: () -> Unit = {
        triedSubmit = true
        if (looksLikeEmail(email) && password.isNotBlank()) {
            focus.clearFocus()
            onSignIn()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Space.gutter)
            .padding(bottom = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.kasiguru.ui.components.brand.Jepjep(
            pose = com.kasiguru.ui.components.brand.JepjepPose.Waving,
            height = 132.dp
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = "Welcome back!",
            style = MaterialTheme.typography.displaySmall,
            color = Ink
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "Sign in to pick up where you left off. Your XP, streak, badges and words come with you.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(Space.lg))

        if (onGoogle != null) {
            OutlinedButton(
                onClick = onGoogle,
                enabled = !isBusy,
                shape = Shapes.pill,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface),
                border = BorderStroke(1.dp, BorderHairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
            AuthDivider(text = "or sign in with email")
            Spacer(Modifier.height(Space.md))
        }

        KasiGuruTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            placeholder = { Text("you@example.com") },
            singleLine = true,
            enabled = !isBusy,
            isError = emailProblem != null,
            supportingText = emailProblem?.let { { Text(it) } },
            leadingIcon = {
                Icon(painterResource(Iconsax.Sms), null, tint = LimeText, modifier = Modifier.size(20.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
            }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Space.sm))
        KasiGuruTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            enabled = !isBusy,
            isError = passwordProblem != null,
            supportingText = passwordProblem?.let { { Text(it) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(painterResource(Iconsax.Lock), null, tint = LimeText, modifier = Modifier.size(20.dp))
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword, enabled = !isBusy) {
                Text(
                    text = "Forgot password?",
                    color = LimeText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        ClayButton(
            label = if (isBusy) "Signing in…" else "Sign in",
            onClick = submit,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        )
        if (isBusy) {
            Spacer(Modifier.height(Space.sm))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Lime,
                trackColor = TrackNeutral
            )
        }

        Spacer(Modifier.height(Space.xl))
        HorizontalDivider(color = BorderHairline)
        Spacer(Modifier.height(Space.md))
        Text(
            text = "New to KasiGuru?",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        TextButton(onClick = onStartNew, enabled = !isBusy) {
            Text(
                text = "Start learning without an account",
                color = LimeText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
