package com.kasiguru.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val progress = uiState.userProgress ?: return

    var fullName by remember { mutableStateOf(progress.fullName) }
    var age by remember { mutableStateOf(progress.age?.toString() ?: "") }
    var address by remember { mutableStateOf(progress.address) }
    var selectedIconId by remember { mutableStateOf(progress.profileIconId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Avatar",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Icon Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                profileIcons.forEachIndexed { index, icon ->
                    val isSelected = index == selectedIconId
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Primary else DarkCard)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Secondary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedIconId = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Avatar $index",
                            tint = if (isSelected) TextWhite else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = TextMuted,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    focusedLabelColor = PrimaryLight,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        age = newValue
                    }
                },
                label = { Text("Age") },
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = TextMuted,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    focusedLabelColor = PrimaryLight,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = TextMuted,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    focusedLabelColor = PrimaryLight,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = ErrorLight,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.updateProfile(
                        fullName = fullName.trim(),
                        age = age.toIntOrNull(),
                        address = address.trim(),
                        iconId = selectedIconId
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
