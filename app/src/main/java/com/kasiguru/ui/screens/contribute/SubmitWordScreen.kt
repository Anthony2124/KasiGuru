package com.kasiguru.ui.screens.contribute

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.domain.contribute.DuplicateLevel
import com.kasiguru.domain.contribute.DuplicateMatch
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitWordScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubmitWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    val hasUnsavedChanges = !uiState.isSuccess && listOf(
        uiState.kasiguranin, uiState.tagalog, uiState.english, uiState.rootForm,
        uiState.partOfSpeech, uiState.exampleSentence, uiState.pastTense,
        uiState.presentTense, uiState.futureTense, uiState.contributorName
    ).any { it.isNotBlank() }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val attemptBack: () -> Unit = { if (hasUnsavedChanges) showDiscardConfirm = true else onNavigateBack() }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardConfirm = true }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this word?") },
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

    if (uiState.showDuplicateConfirm) {
        val alreadyRecorded = uiState.duplicateMatches
            .any { it.level == DuplicateLevel.SameSense }
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateConfirm() },
            title = { Text(if (alreadyRecorded) "This entry already exists" else "This word already exists") },
            text = {
                Text(
                    if (alreadyRecorded) {
                        "KasiGuru already carries this word with this meaning. Submitting it again " +
                            "gives the reviewers a duplicate to reject. Check the entry shown on the " +
                            "form first."
                    } else {
                        "KasiGuru already carries this word with a different meaning. If you are " +
                            "recording a separate sense of it, please continue — that is a word the " +
                            "dictionary is missing."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.submitWord(confirmedDuplicate = true) }) {
                    Text("Submit anyway", color = Amber)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicateConfirm() }) { Text("Go back") }
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
                    text = "Submission Sent for Review!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "Thank you for helping preserve the Kasiguranin heritage. Your contribution is queued for verification by cultural language experts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(Space.lg))

                ClayButton(
                    label = "Submit Another Word",
                    onClick = { viewModel.resetSuccess() },
                    tone = ClayButtonTone.Primary
                )

                Spacer(Modifier.height(Space.sm))

                TextButton(onClick = onNavigateBack) {
                    Text("Return to Dashboard", fontWeight = FontWeight.Bold, color = Muted, fontSize = 14.sp)
                }
            }
        }
        return
    }

    GroundScaffold(
        title = "Help Expand Kasiguranin",
        subtitle = "Share native words, phrases, or local expressions to be verified and published to KasiGuru.",
        onBack = attemptBack,
        pattern = GroundPattern.Orbs,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                GroundTitleBlock(
                    title = "Help Expand Kasiguranin",
                    subtitle = "Share native words, phrases, or local expressions to be verified and published to KasiGuru."
                )
                Spacer(Modifier.height(Space.xs))

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

                // Section 1: Core Translations
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Core Translations",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.kasiguranin,
                        onValueChange = { viewModel.onKasiguraninChanged(it) },
                        label = { Text("Kasiguranin Word *") },
                        placeholder = { Text("e.g. apak, singët, lukag") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Book), contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth().tourAnchor(TourAnchor.SubmitWordField),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.tagalog,
                        onValueChange = { viewModel.onTagalogChanged(it) },
                        label = { Text("Tagalog Translation") },
                        placeholder = { Text("e.g. daras, langgam, gising") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Global), contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.english,
                        onValueChange = { viewModel.onEnglishChanged(it) },
                        label = { Text("English Definition / Translation") },
                        placeholder = { Text("e.g. adze, ant, awake") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Global), contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                    Spacer(Modifier.height(Space.xs))

                    Text(
                        text = "* Kasiguranin word is required, plus at least one of Tagalog or English so learners can find it.",
                        fontSize = 11.sp,
                        color = Muted,
                        lineHeight = 15.sp
                    )

                    if (uiState.duplicateMatches.isNotEmpty()) {
                        Spacer(Modifier.height(Space.sm))
                        DuplicateNotice(matches = uiState.duplicateMatches)
                    }
                }

                // Section 2: Category & Usage Details
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "2. Category & Usage Details",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    ExposedDropdownMenuBox(
                        expanded = expandedCategoryDropdown,
                        onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                Icon(painter = painterResource(id = Iconsax.Element4Outline), contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = Shapes.tile,
                            colors = submitFieldColors()
                        )
                        ExposedDropdownMenu(expanded = expandedCategoryDropdown, onDismissRequest = { expandedCategoryDropdown = false }) {
                            viewModel.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.onCategoryChanged(category)
                                        expandedCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.sm))

                    var expandedPartOfSpeechDropdown by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedPartOfSpeechDropdown,
                        onExpandedChange = { expandedPartOfSpeechDropdown = !expandedPartOfSpeechDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.partOfSpeech.ifEmpty { "(Select Part of Speech)" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Part of Speech") },
                            leadingIcon = {
                                Icon(painter = painterResource(id = Iconsax.HashtagDown), contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPartOfSpeechDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = Shapes.tile,
                            colors = submitFieldColors()
                        )
                        ExposedDropdownMenu(expanded = expandedPartOfSpeechDropdown, onDismissRequest = { expandedPartOfSpeechDropdown = false }) {
                            viewModel.partsOfSpeech.forEach { pos ->
                                DropdownMenuItem(
                                    text = { Text(pos, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.onPartOfSpeechChanged(pos)
                                        expandedPartOfSpeechDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.rootForm,
                        onValueChange = { viewModel.onRootFormChanged(it) },
                        label = { Text("Root Word (Optional)") },
                        placeholder = { Text("e.g. apak") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Edit), contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.exampleSentence,
                        onValueChange = { viewModel.onExampleSentenceChanged(it) },
                        label = { Text("Example Sentence (Optional)") },
                        placeholder = { Text("How is this word used in a sentence?") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Document), contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                }

                // Section 3: Verb Tenses (Optional)
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "3. Verb Tenses (Optional)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.pastTense,
                        onValueChange = { viewModel.onPastTenseChanged(it) },
                        label = { Text("Past Tense") },
                        placeholder = { Text("e.g. naglakaw") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.presentTense,
                        onValueChange = { viewModel.onPresentTenseChanged(it) },
                        label = { Text("Present Tense") },
                        placeholder = { Text("e.g. nagalakaw") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.futureTense,
                        onValueChange = { viewModel.onFutureTenseChanged(it) },
                        label = { Text("Future Tense") },
                        placeholder = { Text("e.g. magalakaw") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                }

                // Section 4: Contributor Credit
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "4. Contributor Credit",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    OutlinedTextField(
                        value = uiState.contributorName,
                        onValueChange = { viewModel.onContributorNameChanged(it) },
                        label = { Text("Your Name / Credit (Optional)") },
                        placeholder = { Text("Leave blank to submit anonymously") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Profile), contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = Shapes.tile,
                        colors = submitFieldColors()
                    )
                }

                ClayButton(
                    label = if (uiState.isLoading) "Submitting…" else "Submit Entry for Verification",
                    onClick = { viewModel.submitWord() },
                    enabled = !uiState.isLoading,
                    tone = ClayButtonTone.Primary,
                    modifier = Modifier.fillMaxWidth().tourAnchor(TourAnchor.SubmitButton),
                    leading = if (uiState.isLoading) {
                        { CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) }
                    } else {
                        {
                            Icon(
                                painter = painterResource(id = Iconsax.AddCircle),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

@Composable
private fun submitFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = Violet,
    unfocusedBorderColor = SurfaceSunken
)

/**
 * Warns that the dictionary may already carry the word being contributed.
 *
 * Sits inside the Core Translations card rather than between the fields so that it cannot shove
 * the Tagalog and English inputs down the screen mid-sentence, and in [Amber] rather than [Red]
 * because it is not an error: the contributor may well be recording a homonym, and the form is
 * built to let them.
 *
 * Shows the matching entries themselves rather than just a count. "This word already exists" tells
 * a contributor nothing they can act on — seeing that the recorded sense of `baga` is *ember* while
 * theirs is *lungs* is what lets them decide in a glance whether to carry on.
 */
@Composable
private fun DuplicateNotice(matches: List<DuplicateMatch>) {
    val exact = matches.filter { it.level != DuplicateLevel.SimilarSpelling }
    val similar = matches.filter { it.level == DuplicateLevel.SimilarSpelling }
    val sameSense = matches.any { it.level == DuplicateLevel.SameSense }

    val heading = when {
        sameSense -> "This entry is already in the dictionary"
        exact.isNotEmpty() -> "This word is already in the dictionary"
        else -> "Similar words are already in the dictionary"
    }
    val guidance = when {
        sameSense -> "KasiGuru already records this word with this meaning, so this would be a duplicate."
        exact.isNotEmpty() -> "If you are recording a different meaning of the same word, please carry on — that is an entry KasiGuru is missing."
        else -> "Only the spelling is close. Carry on if yours is a different word."
    }

    Surface(color = AmberTint, shape = Shapes.tile, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Space.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.InfoCircle),
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = heading,
                    color = Amber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            exact.forEach { match ->
                Spacer(Modifier.height(Space.xs))
                ExistingEntryRow(match)
            }

            if (similar.isNotEmpty()) {
                Spacer(Modifier.height(Space.sm))
                // Not an eyebrow: with no heading above them, this line is the only thing telling
                // the two groups of matches apart. Kept as information, in sentence case at a
                // readable size rather than shouted in 10sp caps.
                Text(
                    text = if (exact.isEmpty()) "Close spellings" else "Also close in spelling",
                    color = Amber,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                similar.forEach { match ->
                    Spacer(Modifier.height(Space.xs))
                    ExistingEntryRow(match)
                }
            }

            Spacer(Modifier.height(Space.xs))
            Text(
                text = guidance,
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/** One existing dictionary entry: the headword, what it means, and where it is filed. */
@Composable
private fun ExistingEntryRow(match: DuplicateMatch) {
    val entry = match.entry
    // Both glosses, because a contributor who wrote only Tagalog cannot judge an English-only line.
    val meaning = listOf(entry.english, entry.tagalog).filter { it.isNotBlank() }.joinToString(" · ")
    Surface(color = Surface, shape = Shapes.tile, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs)) {
            Text(
                text = entry.kasiguranin,
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
            if (meaning.isNotBlank()) {
                Text(text = meaning, color = Ink, fontSize = 12.sp, lineHeight = 16.sp)
            }
            if (entry.category.isNotBlank()) {
                Text(text = entry.category, color = Muted, fontSize = 10.sp)
            }
        }
    }
}
