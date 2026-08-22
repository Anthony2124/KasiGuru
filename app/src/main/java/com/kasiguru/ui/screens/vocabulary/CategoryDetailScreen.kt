package com.kasiguru.ui.screens.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.WordVerificationDialog
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.clay.FloatingSearchBar
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.theme.CategoryRegistry
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Violet
import com.kasiguru.util.audio.AudioPlayerManager

/**
 * One category's word list. A pushed subscreen — no bottom bar, so the canopy carries its own back
 * button — over a plain search-and-list sheet, since the corpus itself is the point here.
 */
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    onNavigateBack: () -> Unit,
    onNavigateToWord: (Int) -> Unit = {},
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var verifyingWord by remember { mutableStateOf<VocabularyEntity?>(null) }
    var unlearningWord by remember { mutableStateOf<VocabularyEntity?>(null) }
    var dictionaryQuery by remember { mutableStateOf("") }
    val dictionaryResults by viewModel.dictionarySearchResults.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    val meta = remember(categoryName) { CategoryRegistry.getMeta(categoryName) }

    LaunchedEffect(categoryName) { viewModel.selectCategory(categoryName) }
    DisposableEffect(Unit) { onDispose { audioPlayerManager.stopAudio() } }

    val categoryWords = remember(searchQuery, uiState.filteredVocabulary) {
        val baseList = uiState.allVocabulary.filter { it.category.equals(categoryName, ignoreCase = true) }
        if (searchQuery.isBlank()) baseList else baseList.filter {
            it.kasiguranin.contains(searchQuery, ignoreCase = true) ||
                it.tagalog.contains(searchQuery, ignoreCase = true) ||
                it.english.contains(searchQuery, ignoreCase = true)
        }
    }
    val totalWords = categoryWords.size
    val learnedCount = categoryWords.count { it.isLearned }

    verifyingWord?.let { word ->
        WordVerificationDialog(
            targetWord = word,
            allWords = uiState.allVocabulary,
            onSuccess = { viewModel.markWordAsLearned(word.id); verifyingWord = null },
            onDismiss = { verifyingWord = null }
        )
    }
    unlearningWord?.let { word ->
        AlertDialog(
            onDismissRequest = { unlearningWord = null },
            title = { Text("Reset learned word?") },
            text = { Text("Reset \"${word.kasiguranin}\" back to unlearned so you can review it again?") },
            confirmButton = {
                TextButton(onClick = { viewModel.unmarkWordAsLearned(word.id); unlearningWord = null }) {
                    Text("Reset word", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { unlearningWord = null }) { Text("Cancel") } }
        )
    }

    GroundScaffold(
        title = meta.name,
        subtitle = meta.description,
        onBack = onNavigateBack,
        pattern = GroundPattern.Grid,
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item {
                    GroundTitleBlock(
                        title = meta.name,
                        subtitle = meta.description,
                        // The learned count was a canopy GlassChip; a progress bar says the same thing
                        // and shows how far along it is, which the chip could only state as a fraction.
                        lead = {
                            KasiGuruProgressBar(
                                progress = if (totalWords == 0) 0f else learnedCount.toFloat() / totalWords,
                                modifier = Modifier.fillMaxWidth(),
                                height = 6.dp,
                                gradientColors = listOf(Violet, VioletDeep)
                            )
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                text = "$learnedCount of $totalWords learned",
                                style = MaterialTheme.typography.labelMedium,
                                color = Muted
                            )
                        }
                    )
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search in ${meta.name}…") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Search), contentDescription = null, tint = Violet)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(id = Iconsax.CloseCircle),
                                    contentDescription = "Clear search",
                                    tint = Faint,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.tile,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = Violet,
                            unfocusedBorderColor = SurfaceSunken
                        ),
                        singleLine = true
                    )
                }
                item {
                    Text(
                        text = "$totalWords words",
                        style = MaterialTheme.typography.labelLarge,
                        color = Muted,
                        modifier = Modifier.padding(top = Space.xs)
                    )
                }
                items(categoryWords, key = { it.id }) { vocab ->
                    CategoryWordCard(
                        vocab = vocab,
                        audioPlayerManager = audioPlayerManager,
                        onMarkLearned = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (vocab.isLearned) unlearningWord = vocab else verifyingWord = vocab
                        }
                    )
                }
            }

            // Overlays the list rather than pushing it. Distinct from the field above: that one
            // filters this category's already-loaded list, this one queries every category so a
            // word from elsewhere in the dictionary can be reached without leaving this screen.
            FloatingSearchBar(
                query = dictionaryQuery,
                onQueryChange = {
                    dictionaryQuery = it
                    viewModel.onDictionarySearchQueryChange(it)
                },
                results = dictionaryResults,
                onResultClick = { word ->
                    dictionaryQuery = ""
                    viewModel.onDictionarySearchQueryChange("")
                    onNavigateToWord(word.id)
                },
                placeholder = "Search the whole dictionary…",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = Space.gutter)
                    .padding(top = 64.dp)
            )
        }
    )
}

@Composable
private fun CategoryWordCard(
    vocab: VocabularyEntity,
    audioPlayerManager: AudioPlayerManager,
    onMarkLearned: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isExpanded = !isExpanded
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = vocab.kasiguranin, style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(text = "${vocab.english} · ${vocab.tagalog}", style = MaterialTheme.typography.bodySmall, color = Faint)
            }
            // Both icons previously had only a 22dp clickable — under the 48dp minimum touch target
            // and sitting inside a card that's itself clickable (to expand), which made mis-taps
            // between "play audio," "mark learned," and "expand the card" easy. Each now gets its own
            // properly-sized tap target instead of just its visible glyph.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        audioPlayerManager.playAudio(vocab.kasiguranin, vocab.audioFileName)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.VolumeHigh),
                    contentDescription = "Listen",
                    tint = Violet,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier.size(44.dp).clickable(onClick = onMarkLearned),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.TickCircle),
                    contentDescription = if (vocab.isLearned) "Learned" else "Mark as learned",
                    tint = if (vocab.isLearned) Green else Faint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = Space.sm)) {
                HorizontalDivider(color = SurfaceSunken)
                Spacer(Modifier.height(Space.sm))

                if (vocab.phoneticGlottal || vocab.phoneticVowelLength || vocab.ipaNotation.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        if (vocab.phoneticGlottal) TagChip(label = "Glottal stop ʔ")
                        if (vocab.phoneticVowelLength) TagChip(label = "Long vowel ː")
                        if (vocab.ipaNotation.isNotEmpty()) TagChip(label = "[${vocab.ipaNotation}]")
                    }
                    Spacer(Modifier.height(Space.sm))
                }

                if (vocab.neutralForm.isNotEmpty()) {
                    Text(text = "Verb aspect inflections", style = MaterialTheme.typography.labelLarge, color = Violet)
                    Spacer(Modifier.height(Space.xs))
                    AspectRow("Neutral (infinitive)", vocab.neutralForm)
                    AspectRow("Imperfective (present)", vocab.imperfectiveForm)
                    AspectRow("Perfective (past)", vocab.perfectiveForm)
                    AspectRow("Contemplative (future)", vocab.contemplativeForm)
                    Spacer(Modifier.height(Space.sm))
                }

                if (vocab.exampleSentence.isNotEmpty()) {
                    Text(text = "Example sentence", style = MaterialTheme.typography.labelLarge, color = Ink)
                    Text(
                        text = "\"${vocab.exampleSentence}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Muted
                    )
                    if (vocab.exampleTranslation.isNotEmpty()) {
                        Text(text = vocab.exampleTranslation, style = MaterialTheme.typography.bodySmall, color = Faint)
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Faint)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Ink)
    }
}
