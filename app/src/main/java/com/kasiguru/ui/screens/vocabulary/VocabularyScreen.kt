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
import com.kasiguru.ui.components.clay.CanopyScaffold
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
    onNavigateToSubmitWord: () -> Unit = {},
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
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

    val featuredWord = remember(uiState.allVocabulary) {
        uiState.allVocabulary.firstOrNull { it.kasiguranin.isNotBlank() && it.tagalog.isNotBlank() }
    }

    val corpusProgress = if (uiState.allVocabulary.isNotEmpty()) {
        uiState.totalLearnedCount.toFloat() / uiState.allVocabulary.size.toFloat()
    } else 0f

    CanopyScaffold(
        canopyHeight = 122.dp,
        canopyContent = {
            Spacer(Modifier.height(Space.sm))
            Text(text = "Dictionary", style = MaterialTheme.typography.headlineMedium, color = OnCanopy)
            Text(
                text = "${uiState.totalLearnedCount} of ${uiState.allVocabulary.size} words learned",
                style = MaterialTheme.typography.bodyMedium,
                color = OnCanopy
            )
            Spacer(Modifier.height(Space.sm))
            KasiGuruProgressBar(
                progress = corpusProgress,
                height = 5.dp,
                gradientColors = listOf(Color.White, Color.White.copy(alpha = 0.7f)),
                animated = true
            )
        },
        sheetContent = {
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

                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(Space.xs))
                    ContributeWordRow(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToSubmitWord()
                    })
                }
            }
        }
    )
}

/**
 * The corpus is community-grown (PRODUCT.md: contributors submit words into a moderated review
 * queue), and the old Home dashboard's banner for this was the only way in — dropped when Home was
 * retired. Dictionary is the entry point's natural home now: it's where a learner notices a gap.
 */
@Composable
private fun ContributeWordRow(onClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, onClick = onClick, elevation = 2.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(Shapes.chip).background(Violet.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = Iconsax.Add), contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(text = "Know a Kasiguranin word?", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(text = "Submit it to the community review queue", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Icon(painter = painterResource(id = Iconsax.ArrowRight), contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
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
