package com.kasiguru.ui.screens.profile

import com.kasiguru.ui.theme.RedText
import com.kasiguru.ui.components.KasiGuruTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepAvatarPicker
import com.kasiguru.ui.components.brand.JepjepAvatarPortrait
import com.kasiguru.ui.components.ErrorDialog
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Lime
import kotlinx.coroutines.launch

/**
 * Edit profile: a short canopy with a back button, over a single form card. A pushed subscreen from
 * both Profile's edit pencil and its avatar tap.
 */
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Lime)
        }
        return
    }

    val progress = uiState.userProgress ?: return
    var fullName by remember { mutableStateOf(progress.fullName) }
    var age by remember { mutableStateOf(progress.age?.toString() ?: "") }
    var address by remember { mutableStateOf(progress.address) }
    var avatar by remember { mutableStateOf(JepjepAvatar.fromId(progress.profileIconId)) }
    val scope = rememberCoroutineScope()

    val hasUnsavedChanges = fullName != progress.fullName ||
        age != (progress.age?.toString() ?: "") ||
        address != progress.address ||
        avatar.id != progress.profileIconId
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val attemptBack: () -> Unit = { if (hasUnsavedChanges) showDiscardConfirm = true else onNavigateBack() }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardConfirm = true }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits haven't been saved yet.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onNavigateBack() }) {
                    Text("Discard", color = RedText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep editing") }
            }
        )
    }

    uiState.error?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = { viewModel.clearError() }
        )
    }

    GroundScaffold(
        title = "Edit profile",
        onBack = onNavigateBack,
        pattern = GroundPattern.Orbs,
        compactTitle = true,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Space.gutter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(Space.lg))
                JepjepAvatarPortrait(
                    avatar = avatar,
                    size = 96.dp,
                    level = progress.level
                )
                Spacer(Modifier.height(Space.lg))

                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        Text("Your avatar", style = MaterialTheme.typography.titleMedium, color = com.kasiguru.ui.theme.Ink)
                        JepjepAvatarPicker(selected = avatar, onSelect = { avatar = it }, portraitSize = 64.dp)
                    }
                }
                Spacer(Modifier.height(Space.md))

                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        KasiGuruTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        KasiGuruTextField(
                            value = age,
                            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) age = it },
                            label = { Text("Age") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        KasiGuruTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(Modifier.height(Space.lg))

                ClayButton(
                    label = if (uiState.isSaving) "Saving…" else "Save changes",
                    enabled = !uiState.isSaving,
                    onClick = {
                        scope.launch {
                            val saved = viewModel.updateProfile(
                                fullName = fullName.trim(),
                                age = age.trim().toIntOrNull(),
                                address = address.trim(),
                                iconId = avatar.id
                            )
                            // Only leave once the write actually finished — a failure now surfaces
                            // as the inline message below instead of navigating away silently.
                            if (saved) onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = Lime,
    unfocusedBorderColor = SurfaceSunken
)
