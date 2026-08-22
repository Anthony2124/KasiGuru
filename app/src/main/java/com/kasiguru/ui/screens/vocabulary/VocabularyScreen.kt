package com.kasiguru.ui.screens.vocabulary

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.components.clay.FloatingSearchBar
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.CategoryMetaData
import com.kasiguru.ui.theme.CategoryRegistry
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.util.audio.AudioPlayerManager

/**
 * Dictionary: a short canopy stating the corpus and how much of it is learned, over a sheet of
 * search, the word of the day, and the twelve category cards.
 *
 * The canopy is deliberately short (DESIGN.md: "short on Dictionary") — this screen's job is to get a
 * learner into a category fast, not to hold their attention the way Learn or Progress does.
 */
@Composable
fun VocabularyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToWord: (Int) -> Unit = {},
    onNavigateToSubmitWord: () -> Unit = {},
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var dictionaryQuery by remember { mutableStateOf("") }
    val dictionaryResults by viewModel.dictionarySearchResults.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager(context) }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.stopAudio() }
    }

    val filterOptions = listOf("All", "Essentials", "Food", "Animals", "Body", "Numbers", "House", "Nature")

    val displayedCategories = remember(searchQuery, selectedFilterCategory, CategoryRegistry.categories) {
        CategoryRegistry.categories.filter { meta ->
            val matchesSearch = searchQuery.isBlank() ||
                meta.name.contains(searchQuery, ignoreCase = true) ||
                meta.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedFilterCategory == "All" ||
                meta.name.contains(selectedFilterCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    // Word of the Day was the alphabetically-first eligible word, recomputed only when the
    // vocabulary list reference changed - the same word for every user, forever, until the
    // dictionary itself changed. Seeding the pick by the epoch day gives a word that is stable
    // for everyone on a given day and rotates the next, over a stable id-sorted list so the
    // pick doesn't shift if Room's category/name ordering ever does.
    val featuredWord = remember(uiState.allVocabulary) {
        val eligible = uiState.allVocabulary
            .filter { it.kasiguranin.isNotBlank() && it.tagalog.isNotBlank() }
            .sortedBy { it.id }
        if (eligible.isEmpty()) null else {
            val today = java.time.LocalDate.now().toEpochDay()
            eligible[(today % eligible.size).toInt()]
        }
    }

    val corpusProgress = if (uiState.allVocabulary.isNotEmpty()) {
        uiState.totalLearnedCount.toFloat() / uiState.allVocabulary.size.toFloat()
    } else 0f

    GroundScaffold(
        title = "Dictionary",
        subtitle = "${uiState.totalLearnedCount} of ${uiState.allVocabulary.size} words learned",
        pattern = GroundPattern.Grid,
        content = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                ),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item(span = { GridItemSpan(2) }) {
                    GroundTitleBlock(
                        title = "Dictionary",
                        subtitle = "${uiState.totalLearnedCount} of ${uiState.allVocabulary.size} words learned",
                        lead = {
                            // The white gradient existed only to survive the violet canopy.
                            KasiGuruProgressBar(
                                progress = corpusProgress,
                                modifier = Modifier.fillMaxWidth(),
                                height = 6.dp,
                                gradientColors = listOf(Violet, VioletDeep)
                            )
                        }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    SubmitWordBanner(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToSubmitWord()
                    })
                    Spacer(Modifier.height(Space.xs))
                }

                item(span = { GridItemSpan(2) }) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search categories…") },
                        leadingIcon = {
                            Icon(painter = painterResource(id = Iconsax.Search), contentDescription = null, tint = Violet)
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
                    Spacer(Modifier.height(Space.sm))
                }

                item(span = { GridItemSpan(2) }) {
                    WordOfTheDayCard(
                        kasiguranin = featuredWord?.kasiguranin ?: "singët",
                        translation = if (featuredWord != null) {
                            "${featuredWord.tagalog} · ${featuredWord.english}"
                        } else "langgam · ant",
                        onPlayClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (featuredWord != null) {
                                audioPlayer.playAudio(featuredWord.kasiguranin, featuredWord.audioFileName)
                            } else {
                                audioPlayer.playAudio("singët", "")
                            }
                        }
                    )
                    Spacer(Modifier.height(Space.md))
                }

                item(span = { GridItemSpan(2) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(filterOptions) { filterName ->
                            val isSelected = selectedFilterCategory == filterName
                            Box(
                                modifier = Modifier
                                    .clip(Shapes.pill)
                                    .background(if (isSelected) Violet else SurfaceSunken)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedFilterCategory = filterName
                                    }
                                    .padding(horizontal = Space.md, vertical = Space.xs)
                            ) {
                                Text(
                                    text = filterName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White else Muted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.md))
                }

                item(span = { GridItemSpan(2) }) {
                    SectionHeading(text = "Categories")
                    Spacer(Modifier.height(Space.sm))
                }

                items(displayedCategories, key = { it.name }) { meta ->
                    val stats = uiState.categoryStats[meta.name] ?: CategoryProgressStats()
                    CategoryCard(
                        meta = meta,
                        stats = stats,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToCategory(meta.name)
                        }
                    )
                }

            }

            // Overlays the grid rather than pushing it - the grid stays scrollable underneath.
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = Space.gutter)
                    .padding(top = 64.dp)
            )
        }
    )
}

/**
 * The way into the community review queue, and the loudest thing on the Dictionary.
 *
 * The corpus is community-grown (PRODUCT.md: contributors submit words into a moderated queue), but
 * this used to be the very last item in the category grid - a 40dp icon and two lines of small text,
 * below twelve cards nobody scrolls past. It now opens the screen.
 *
 * Clay and violet on purpose: on a page made of a search field and a grid of categories this is the
 * only *action*, and DESIGN.md reserves clay for things you press. White text is safe here without
 * new measurement - Violet carries white at 6.00 in the measured table - and the icon disc can be
 * translucent because, unlike anything else on this screen, it has a genuinely vivid backdrop.
 */
@Composable
private fun SubmitWordBanner(onClick: () -> Unit) {
    ClaySurface(
        face = Violet,
        lipColor = VioletDeep,
        shape = Shapes.panel,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(44.dp).clip(Shapes.chip).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Add),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Submit a word",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "Add a Kasiguranin word you know to the community review queue",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(Space.sm))
            Icon(
                painter = painterResource(id = Iconsax.ArrowRight),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
@Composable
private fun WordOfTheDayCard(kasiguranin: String, translation: String, onPlayClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.panel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = "Word of the day", style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(2.dp))
                Text(text = kasiguranin, style = KasiguraninHeadword.copy(fontSize = 26.sp, lineHeight = 30.sp), color = Violet)
                Text(text = translation, style = MaterialTheme.typography.bodySmall, color = Faint)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Violet, VioletDeep)))
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.VolumeHigh),
                    contentDescription = "Play pronunciation",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(meta: CategoryMetaData, stats: CategoryProgressStats, onClick: () -> Unit) {
    val onGradient = if (meta.onGradientIsInk) RewardInk else Color.White
    SoftCard(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = Shapes.panel,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(meta.startColor, meta.endColor)))
                .padding(Space.md)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(onGradient.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (meta.customDrawableRes != null) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = meta.customDrawableRes),
                                contentDescription = meta.name,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = meta.iconRes),
                                contentDescription = null,
                                tint = onGradient,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.clip(Shapes.pill).background(onGradient.copy(alpha = 0.20f))
                            .padding(horizontal = Space.xs, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${stats.totalWords} words",
                            style = MaterialTheme.typography.labelSmall,
                            color = onGradient
                        )
                    }
                }
                Column {
                    Text(
                        text = meta.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = onGradient,
                        maxLines = 2
                    )
                    Text(
                        text = "${stats.learnedWords} learned",
                        style = MaterialTheme.typography.labelSmall,
                        color = onGradient.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
