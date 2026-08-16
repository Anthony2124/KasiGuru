package com.kasiguru.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignInMode by remember { mutableStateOf(false) }

    val googleSignIn = rememberGoogleSignIn(onIdToken = { viewModel.linkGoogle(it) })

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowLeft),
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isSignInMode) "Sign in to your account" else "Create your account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSignInMode) {
                                "Already have an account? Sign in to load its progress on this device."
                            } else {
                                "Your current progress stays exactly as it is and gets attached to the new account."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtleGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))

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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (isSignInMode) viewModel.signIn(email, password)
                                else viewModel.createOrLinkAccount(email, password)
                            },
                            enabled = !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (uiState.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = TextHeadingBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignInMode) "Sign In" else "Create Account",
                                    color = TextHeadingBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (googleSignIn != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = googleSignIn,
                                enabled = !uiState.isBusy,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Global),
                                    contentDescription = null,
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Continue with Google",
                                    color = TextHeadingBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isSignInMode = !isSignInMode }) {
                                Text(
                                    text = if (isSignInMode) "Create an account" else "I already have one",
                                    color = HeroCardStart,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isSignInMode) {
                                TextButton(onClick = { viewModel.sendPasswordReset(email) }) {
                                    Text("Forgot password?", color = TextSubtleGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountStatusCard(
    isRecoverable: Boolean,
    email: String?,
    providers: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isRecoverable) Success.copy(alpha = 0.15f) else Warning.copy(alpha = 0.2f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            id = if (isRecoverable) Iconsax.TickCircle else Iconsax.InfoCircle
                        ),
                        contentDescription = null,
                        tint = if (isRecoverable) Success else Warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = if (isRecoverable) "Progress protected" else "Guest progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
                Text(
                    text = when {
                        isRecoverable && email != null -> "Signed in as $email"
                        isRecoverable && providers.contains("google.com") -> "Signed in with Google"
                        isRecoverable -> "Signed in"
                        else -> "Saved on this device only. Uninstalling loses it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtleGray
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
                }) { Text("Sign out", fontWeight = FontWeight.Bold, color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Your progress syncs automatically",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHeadingBlack
            )
            Text(
                text = "XP, streaks, badges, level stars and word reviews are saved to your " +
                    "account and restored whenever you sign in.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSubtleGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { confirming = true },
                enabled = !isBusy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Logout),
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = Error, fontWeight = FontWeight.Bold)
            }
        }
    }
}
