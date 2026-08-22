package com.kasiguru.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.ProfileEntity
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet

/**
 * "Who is using this device" - shown after Splash whenever more than one profile exists, and
 * reachable any time from Settings to add a family member or switch back.
 *
 * Selecting a profile only changes which one is marked active for display purposes right now -
 * see [com.kasiguru.data.local.entity.ProfileEntity]'s doc comment for why progress itself is
 * not yet split per profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    onProfileSelected: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileSelectionViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, resident ->
                viewModel.addProfile(name, resident.name)
                showAddDialog = false
            }
        )
    }

    GroundScaffold(
        title = "Who's learning?",
        onBack = onBack,
        pattern = GroundPattern.Orbs,
        content = {
            Column(modifier = Modifier.fillMaxSize().padding(Space.gutter)) {
                GroundTitleBlock(
                    title = "Who's learning?",
                    subtitle = "Pick a profile, or add one for someone else sharing this device."
                )
                Spacer(Modifier.height(Space.md))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileRow(
                            profile = profile,
                            onClick = { viewModel.selectProfile(profile.id, onProfileSelected) },
                            onDelete = if (profiles.size > 1) {
                                { viewModel.deleteProfile(profile.id) }
                            } else null
                        )
                    }
                    item {
                        Spacer(Modifier.height(Space.sm))
                        ClayButton(
                            label = "Add a profile",
                            onClick = { showAddDialog = true },
                            tone = ClayButtonTone.Quiet,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ProfileRow(profile: ProfileEntity, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    val resident = runCatching { CasiguranResident.valueOf(profile.residentName) }
        .getOrDefault(CasiguranResident.TEACHER)

    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CasiguranAvatarPortrait(resident = resident, size = 48.dp, showLevelRing = false)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(text = profile.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                if (profile.isActive) {
                    Text(text = "Currently active", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
            if (onDelete != null) {
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(id = Iconsax.CloseCircle),
                        contentDescription = "Remove ${profile.name}",
                        tint = Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, resident: CasiguranResident) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val choices = listOf(
        CasiguranResident.STUDENT,
        CasiguranResident.TEACHER,
        CasiguranResident.ELDER,
        CasiguranResident.SURFER,
        CasiguranResident.MUSICIAN,
        CasiguranResident.FARMER
    )
    var selected by remember { mutableStateOf(choices.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    choices.forEach { resident ->
                        CasiguranAvatarPortrait(
                            resident = resident,
                            size = 40.dp,
                            showLevelRing = selected == resident,
                            onClick = { selected = resident }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selected) }
            ) { Text("Add", color = Violet) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Red) }
        }
    )
}
