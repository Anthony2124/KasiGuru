package com.kasiguru.ui.screens.report

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(
    prefilledCategory: String? = null,
    prefilledWord: String? = null,
    prefilledScreenContext: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: ReportIssueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(prefilledCategory, prefilledWord, prefilledScreenContext) {
        viewModel.initPrefilled(prefilledCategory, prefilledWord, prefilledScreenContext)
    }

    // Photo picker launcher (Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri)
        }
    }

    // Fallback document/content picker
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri)
        }
    }

    val openPhotoPicker = {
        try {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            getContentLauncher.launch("image/*")
        }
    }

    val hasUnsavedChanges = !uiState.isSuccess && (
        uiState.title.isNotBlank() ||
            uiState.description.isNotBlank() ||
            uiState.targetWord.isNotBlank() ||
            uiState.photoUri != null
    )
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val attemptBack: () -> Unit = { if (hasUnsavedChanges) showDiscardConfirm = true else onNavigateBack() }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardConfirm = true }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard report?") },
            text = { Text("What you've entered hasn't been submitted yet.") },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Space.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Green.copy(alpha = 0.15f)),
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
                    text = "Report Submitted!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "Thank you for helping us improve KasiGuru! Our development and language moderation team will review this issue promptly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                if (!uiState.submittedReportId.isNullOrBlank()) {
                    Spacer(Modifier.height(Space.sm))
                    Surface(shape = Shapes.pill, color = Violet.copy(alpha = 0.1f)) {
                        Text(
                            text = "Reference ID: #${uiState.submittedReportId?.take(8)}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Violet
                        )
                    }
                }
                Spacer(Modifier.height(Space.lg))

                ClayButton(
                    label = "Submit Another Report",
                    onClick = { viewModel.resetSuccess() },
                    tone = ClayButtonTone.Primary,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(Modifier.height(Space.sm))

                TextButton(onClick = onNavigateBack) {
                    Text("Return", fontWeight = FontWeight.Bold, color = Muted, fontSize = 14.sp)
                }
            }
        }
        return
    }

    GroundScaffold(
        title = "Report an Issue",
        subtitle = "Report bugs, incorrect words, or translation errors with screenshot evidence.",
        onBack = attemptBack,
        pattern = GroundPattern.Grid,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter)
                    .padding(bottom = Space.navBarClearance),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                GroundTitleBlock(
                    title = "Report an Issue",
                    subtitle = "Found a bug, glitch, or wrong word? Help us fix it."
                )

                if (uiState.errorMessage != null) {
                    Surface(color = RedTint, shape = Shapes.tile, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.xs)
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.InfoCircle),
                                contentDescription = null,
                                tint = Red,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = uiState.errorMessage!!,
                                color = Red,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 1. Issue Category
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = "What kind of issue are you reporting?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(Space.sm))

                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        viewModel.categories.forEach { cat ->
                            val isSelected = uiState.category == cat
                            val categoryIcon = when (cat) {
                                "Bug / System Issue" -> Iconsax.Setting
                                "Wrong Word / Translation" -> Iconsax.Book
                                "Grammar / Literature" -> Iconsax.Document
                                "Audio Issue" -> Iconsax.VolumeHigh
                                else -> Iconsax.InfoCircle
                            }

                            Surface(
                                shape = Shapes.tile,
                                color = if (isSelected) Violet.copy(alpha = 0.12f) else Surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onCategorySelected(cat) }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Violet else SurfaceSunken,
                                        shape = Shapes.tile
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = categoryIcon),
                                            contentDescription = null,
                                            tint = if (isSelected) Violet else Muted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = cat,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Violet else Ink,
                                            fontSize = 14.sp
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.onCategorySelected(cat) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Violet)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Report Details
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "2. Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    if (uiState.category == "Wrong Word / Translation") {
                        OutlinedTextField(
                            value = uiState.targetWord,
                            onValueChange = { viewModel.onTargetWordChanged(it) },
                            label = { Text("Kasiguranin Word *") },
                            placeholder = { Text("e.g. apak, bëkas, talapid") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = Iconsax.Book),
                                    contentDescription = null,
                                    tint = Violet,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = Shapes.tile,
                            colors = reportFieldColors()
                        )
                        Spacer(Modifier.height(Space.sm))
                    }

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        label = { Text("Summary Title *") },
                        placeholder = {
                            Text(
                                if (uiState.category == "Wrong Word / Translation") {
                                    "e.g. Incorrect Tagalog definition for 'apak'"
                                } else {
                                    "e.g. Audio doesn't play in Recall Game"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = Iconsax.Edit),
                                contentDescription = null,
                                tint = Violet,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = reportFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        label = { Text("Detailed Description *") },
                        placeholder = {
                            Text(
                                if (uiState.category == "Wrong Word / Translation") {
                                    "Describe what is wrong with the current word or translation, and what the correct Kasiguranin meaning or spelling should be."
                                } else {
                                    "Describe what happened, the steps to reproduce it, or where in the app you encountered this problem."
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        shape = Shapes.tile,
                        colors = reportFieldColors()
                    )
                }

                // 3. Photo Evidence
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "3. Photo Evidence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Ink
                            )
                            Text(
                                text = "Attach a screenshot showing the bug or typo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }

                        if (uiState.photoUri != null) {
                            TextButton(onClick = { viewModel.onRemovePhoto() }) {
                                Text("Remove", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.sm))

                    if (uiState.photoUri == null) {
                        Surface(
                            shape = Shapes.tile,
                            color = Violet.copy(alpha = 0.05f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openPhotoPicker() }
                                .border(
                                    width = 1.5.dp,
                                    color = Violet.copy(alpha = 0.4f),
                                    shape = Shapes.tile
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Violet.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.AddCircle),
                                        contentDescription = "Add photo",
                                        tint = Violet,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "Add Screenshot / Photo Evidence",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Violet
                                )
                                Text(
                                    text = "Tap to choose an image from your gallery",
                                    fontSize = 12.sp,
                                    color = Muted
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Space.xs)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(Shapes.tile)
                                    .background(SurfaceSunken)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uiState.photoUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Attached screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (uiState.isCompressingPhoto) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓ Photo attached successfully",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Green,
                                    fontWeight = FontWeight.SemiBold
                                )

                                TextButton(onClick = { openPhotoPicker() }) {
                                    Text("Change Photo", color = Violet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 4. Reporter Info & Diagnostics
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "4. Submitter & Device Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = "Device specs are attached automatically to help diagnose errors.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.reporterName,
                        onValueChange = { viewModel.onReporterNameChanged(it) },
                        label = { Text("Your Name (Optional)") },
                        placeholder = { Text("Anonymous learner") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = Iconsax.Profile),
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = reportFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    Surface(
                        shape = Shapes.tile,
                        color = SurfaceSunken.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.InfoCircle),
                                    contentDescription = null,
                                    tint = Muted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Auto-attached diagnostics:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                            }
                            Text(
                                text = "App: v${uiState.appVersion} • Device: ${uiState.deviceInfo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Submit Button
                ClayButton(
                    label = if (uiState.isSubmitting) "Submitting Report…" else "Submit Report",
                    onClick = { viewModel.submitReport() },
                    enabled = !uiState.isSubmitting && !uiState.isCompressingPhoto,
                    tone = ClayButtonTone.Primary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

@Composable
private fun reportFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = Violet,
    unfocusedBorderColor = SurfaceSunken
)
