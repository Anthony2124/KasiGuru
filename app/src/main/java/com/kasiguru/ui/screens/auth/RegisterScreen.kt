package com.kasiguru.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, GradientOceanEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start your Kasiguranin learning journey today",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (optional)") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error Message
            AnimatedVisibility(visible = uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = uiState.error ?: "",
                        color = ErrorLight,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = {
                    viewModel.registerUser(
                        username = username,
                        password = password,
                        confirmPassword = confirmPassword,
                        email = email
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape = RoundedCornerShape(16.dp),
                enabled = username.isNotBlank() && password.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = TextGray, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Sign In",
                    color = PrimaryLight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
            }
        }

        val showOtpDialog by viewModel.showOtpDialog.collectAsState()
        if (showOtpDialog) {
            var otpInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissOtpDialog() },
                containerColor = DarkSurface,
                title = { Text("Verify Email", color = TextWhite) },
                text = {
                    Column {
                        Text("We've sent a 6-digit verification code to your email. Please enter it below:", color = TextGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 6) otpInput = it },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Primary,
                                unfocusedIndicatorColor = TextMuted,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.verifyOtp(otpInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Text("Verify", color = TextWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissOtpDialog() }) {
                        Text("Cancel", color = TextGray)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun authTextFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Primary,
    unfocusedIndicatorColor = TextMuted,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
    focusedLabelColor = PrimaryLight,
    unfocusedLabelColor = TextMuted
)
