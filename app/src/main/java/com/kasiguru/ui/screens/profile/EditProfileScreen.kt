package com.kasiguru.ui.screens.profile

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
import androidx.compose.material3.OutlinedTextField
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
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.clay.CanopyBackButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Violet
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
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    val progress = uiState.userProgress ?: return
    var fullName by remember { mutableStateOf(progress.fullName) }
    var age by remember { mutableStateOf(progress.age?.toString() ?: "") }
    var address by remember { mutableStateOf(progress.address) }
    val scope = rememberCoroutineScope()

    val hasUnsavedChanges = fullName != progress.fullName ||
        age != (progress.age?.toString() ?: "") ||
        address != progress.address
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
                    Text("Discard", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep editing") }
            }
        )
    }

    CanopyScaffold(
        canopyHeight = 128.dp,
        canopyContent = {
            CanopyBackButton(onClick = attemptBack)
            Spacer(Modifier.height(Space.sm))
            Text(text = "Edit profile", style = MaterialTheme.typography.headlineMedium, color = OnCanopy)
            Text(text = "Keep your details up to date", style = MaterialTheme.typography.bodyMedium, color = OnCanopy)
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Space.gutter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(Space.lg))
                CasiguranAvatarPortrait(
                    resident = CasiguranResident.TEACHER,
                    size = 88.dp,
                    level = progress.level
                )
                Spacer(Modifier.height(Space.lg))

                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.tile,
                            colors = fieldColors(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = age,
                            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) age = it },
                            label = { Text("Age") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.tile,
                            colors = fieldColors(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.tile,
                            colors = fieldColors(),
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
                                iconId = 1
                            )
                            // Only leave once the write actually finished — a failure now surfaces
                            // as the inline message below instead of navigating away silently.
                            if (saved) onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.error?.let { message ->
                    Spacer(Modifier.height(Space.sm))
                    Text(text = message, style = MaterialTheme.typography.bodySmall, color = Red)
                }
                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = Violet,
    unfocusedBorderColor = SurfaceSunken
)
