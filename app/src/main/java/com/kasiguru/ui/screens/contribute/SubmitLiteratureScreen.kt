package com.kasiguru.ui.screens.contribute

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

/**
 * Submitting a full story or poem, not just a single word - the extension of
 * [SubmitWordScreen]'s pending-review pattern to literature. Lands in
 * `literature_submissions` for admin review, same as a word submission lands in
 * `word_submissions`; nothing here writes to a live table directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitLiteratureScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubmitLiteratureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPdfSelected(context, uri)
        }
    }

    val openPdfPicker = {
        pdfPickerLauncher.launch("application/pdf")
    }

    val hasUnsavedChanges = !uiState.isSuccess && (
        uiState.title.isNotBlank() || uiState.titleKasiguranin.isNotBlank() ||
            uiState.pdfUri != null ||
            uiState.pages.any { it.kasiguranin.isNotBlank() || it.tagalog.isNotBlank() || it.english.isNotBlank() }
        )
    var showDiscardConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardConfirm = true }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this piece?") },
            text = { Text("What you've written hasn't been submitted yet.") },
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

    if (uiState.isSuccess) {
        Box(Modifier.fillMaxSize().background(Ground)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Space.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Green.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.TickCircle),
                        contentDescription = "Success",
                        tint = Green,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(Space.lg))
                Text(
                    text = "Submitted for review!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "A moderator will read it before it joins the stories collection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xl))
                ClayButton(label = "Done", onClick = onNavigateBack, modifier = Modifier.fillMaxWidth())
            }
        }
        return
    }

    GroundScaffold(
        title = "Submit a story or poem",
        onBack = { if (hasUnsavedChanges) showDiscardConfirm = true else onNavigateBack() },
        pattern = GroundPattern.None,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                GroundTitleBlock(
                    title = "Submit a story or poem",
                    subtitle = "Attach the piece as a PDF file, with a title and optional text transcription. " +
                        "It joins the pending queue for a moderator to review, the same as a submitted word."
                )

                uiState.errorMessage?.let { message ->
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = message, color = Red, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Title & Author details
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        OutlinedTextField(
                            value = uiState.titleKasiguranin,
                            onValueChange = viewModel::onTitleKasiguraninChanged,
                            label = { Text("Title, in Kasiguranin") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::onTitleChanged,
                            label = { Text("Title, translated") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.contributorName,
                            onValueChange = viewModel::onContributorNameChanged,
                            label = { Text("Your name (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // PDF Document Attachment (Required)
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Story / Manuscript Document *",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = "PDF file required (max 500 KB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                            if (uiState.pdfUri != null) {
                                TextButton(onClick = { viewModel.onRemovePdf() }) {
                                    Text("Remove", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        if (uiState.isReadingPdf) {
                            Surface(
                                shape = Shapes.tile,
                                color = Violet.copy(alpha = 0.05f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(Space.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Violet,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Reading and attaching PDF…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Violet
                                    )
                                }
                            }
                        } else if (uiState.pdfUri != null && uiState.pdfBase64.isNotBlank()) {
                            val formattedSize = if (uiState.pdfFileSize > 0) {
                                val kb = uiState.pdfFileSize / 1024
                                if (kb >= 1024) String.format("%.1f MB", kb / 1024.0) else "$kb KB"
                            } else ""

                            Surface(
                                shape = Shapes.tile,
                                color = Violet.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Violet.copy(alpha = 0.3f), Shapes.tile)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Space.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Violet.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.Document),
                                            contentDescription = "PDF Document",
                                            tint = Violet,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.pdfFileName.ifBlank { "Attached PDF" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Ink,
                                            maxLines = 1
                                        )
                                        if (formattedSize.isNotBlank()) {
                                            Text(
                                                text = formattedSize,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Muted
                                            )
                                        }
                                    }
                                    TextButton(onClick = openPdfPicker) {
                                        Text("Change", color = Violet, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = Shapes.tile,
                                color = Violet.copy(alpha = 0.04f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openPdfPicker() }
                                    .border(
                                        width = 1.5.dp,
                                        color = Violet.copy(alpha = 0.35f),
                                        shape = Shapes.tile
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Violet.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.Document),
                                            contentDescription = "Attach PDF",
                                            tint = Violet,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = "Attach Story / Poem (PDF) *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Violet
                                    )
                                    Text(
                                        text = "Tap to choose a .pdf document from your device",
                                        fontSize = 12.sp,
                                        color = Muted
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional page-by-page transcription
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page Transcription (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                uiState.pages.forEachIndexed { index, page ->
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Violet
                                )
                                if (uiState.pages.size > 1) {
                                    IconButton(onClick = { viewModel.removePage(index) }) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.CloseCircle),
                                            contentDescription = "Remove page ${index + 1}",
                                            tint = Muted
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = page.kasiguranin,
                                onValueChange = { viewModel.onPageChanged(index, page.copy(kasiguranin = it)) },
                                label = { Text("Kasiguranin") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            OutlinedTextField(
                                value = page.tagalog,
                                onValueChange = { viewModel.onPageChanged(index, page.copy(tagalog = it)) },
                                label = { Text("Tagalog (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            OutlinedTextField(
                                value = page.english,
                                onValueChange = { viewModel.onPageChanged(index, page.copy(english = it)) },
                                label = { Text("English (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }
                }

                ClayButton(
                    label = "Add another page",
                    onClick = viewModel::addPage,
                    tone = ClayButtonTone.Quiet,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(Space.sm))
                ClayButton(
                    label = if (uiState.isLoading) "Submitting…" else "Submit for review",
                    onClick = viewModel::submitLiterature,
                    enabled = !uiState.isLoading && !uiState.isReadingPdf,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

