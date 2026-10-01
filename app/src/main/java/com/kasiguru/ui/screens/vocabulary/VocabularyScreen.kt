package com.kasiguru.ui.screens.vocabulary

import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.components.KasiGuruTextField
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.GroundIconButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.CategoryMetaData
import com.kasiguru.ui.theme.CategoryRegistry
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Info
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.WidthClass
import com.kasiguru.ui.theme.rememberWidthClass
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.TourRevealInLazyGrid
import com.kasiguru.ui.tour.tourAnchor
import com.kasiguru.util.audio.AudioPlayerManager
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

/**
 * The dictionary as a screen of its own, pushed - from a notification, a help link or the guided
 * tour. The Library tab shows the same [DictionaryContent] under its Words segment.
 */
@Composable
fun VocabularyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToWord: (Int) -> Unit = {},
    onNavigateToSubmitWord: () -> Unit = {},
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val isSyncing by viewModel.isSyncing.collectAsState()

    GroundScaffold(
        title = "Dictionary",
        onBack = onNavigateBack,
        pattern = GroundPattern.None,
        actions = { DictionaryRefreshAction(isSyncing = isSyncing, onRefresh = viewModel::refreshFromCloud) },
        content = {
            DictionaryContent(
                onNavigateToCategory = onNavigateToCategory,
                onNavigateToWord = onNavigateToWord,
                onNavigateToSubmitWord = onNavigateToSubmitWord,
                header = { GroundTitleBlock(title = "Dictionary") },
                viewModel = viewModel
            )
        }
    )
}

/** Pulls the latest corpus now. A spinner while it runs, so a second tap cannot stack a second sync. */
@Composable
fun DictionaryRefreshAction(isSyncing: Boolean, onRefresh: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    if (isSyncing) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(end = Space.sm)
                .size(20.dp),
            strokeWidth = 2.dp,
            color = Lime
        )
    } else {
        GroundIconButton(
            iconRes = Iconsax.Refresh,
            contentDescription = "Refresh the dictionary from the cloud",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRefresh()
            }
        )
    }
}

/** Status hues the category tiles cycle through, so neighbouring tiles differ without new colours. */
private val CategoryAccents: List<Color> @Composable get() = listOf(Lime, Info, Gold, Coral)

/**
 * The dictionary's body: one search field, the word of the day, Add a word, and the categories.
 *
 * Carries no bar or back button of its own, so it can sit under [VocabularyScreen]'s bar or inside
 * the Library tab below its Words/Stories toggle; [header] is drawn as the first full-width item and
 * scrolls away with the content.
 *
 * One search field does the work the old screen split between a category filter and a floating
 * word search: it looks through every entry and every category at once, and says so plainly - with
 * Jepjep curious - when nothing matches.
 */
@Composable
fun DictionaryContent(
    onNavigateToCategory: (String) -> Unit,
    onNavigateToWord: (Int) -> Unit,
    onNavigateToSubmitWord: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val search by viewModel.dictionarySearch.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager(context) }
    val columns = if (rememberWidthClass() == WidthClass.COMPACT) 2 else 3
    val gridState = rememberLazyGridState()

    DisposableEffect(Unit) {
        onDispose { audioPlayer.stopAudio() }
    }

    // The field's text survives a segment switch in Library; the view model's query must agree with it.
    LaunchedEffect(Unit) { viewModel.onDictionarySearchQueryChange(query) }

    // The registry's twelve, plus any category the corpus actually contains that the registry has
    // never heard of. Rendering the registry alone meant a word filed under "General" -- which is
    // what the admin portal offers and what the sync falls back to -- had no card at all and could
    // be reached only by typing its name into the search bar.
    val allCategoryMeta = remember(uiState.categories) {
        val known = CategoryRegistry.categories.map { it.name.lowercase() }.toSet()
        CategoryRegistry.categories +
            uiState.categories
                .filter { it.isNotBlank() && it.lowercase() !in known }
                .map { CategoryRegistry.getMeta(it) }
    }

    val trimmed = query.trim()
    val searching = trimmed.isNotEmpty()
    val matchingCategories = remember(trimmed, allCategoryMeta) {
        if (trimmed.isEmpty()) allCategoryMeta
        else allCategoryMeta.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.description.contains(trimmed, ignoreCase = true)
        }
    }
    val searchSettled = search.query == trimmed
    val wordResults = if (searchSettled) search.results else emptyList()

    // Word of the Day: seeded by the epoch day, so it is stable for everyone on a given day and
    // rotates the next, over a stable id-sorted list.
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

    // Index of each anchored item, for bringing it back on screen during the tour. Only meaningful
    // while not searching, which is the state the tour always arrives in.
    val base = if (header != null) 1 else 0
    TourRevealInLazyGrid(gridState) { anchor ->
        when (anchor) {
            TourAnchor.DictWordOfDay -> base + 2
            TourAnchor.DictSubmitBanner -> base + 3
            else -> null
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
        ),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") { header() }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "search") {
            SearchField(
                query = query,
                onQueryChange = {
                    query = it
                    viewModel.onDictionarySearchQueryChange(it)
                }
            )
        }

        if (!searching) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "progress") {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "${uiState.totalLearnedCount} of ${uiState.allVocabulary.size} words learned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                    Spacer(Modifier.height(Space.xs))
                    KasiGuruProgressBar(
                        progress = corpusProgress,
                        modifier = Modifier.fillMaxWidth(),
                        height = 6.dp
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "word-of-day") {
                if (uiState.isLoading && featuredWord == null) {
                    LoadingState(label = "Loading the dictionary")
                } else {
                    WordOfTheDayCard(
                        modifier = Modifier.tourAnchor(TourAnchor.DictWordOfDay),
                        kasiguranin = featuredWord?.kasiguranin ?: "singët",
                        translation = when {
                            featuredWord == null -> "langgam · ant"
                            else -> "${featuredWord.tagalog} · ${featuredWord.english}"
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (featuredWord != null) onNavigateToWord(featuredWord.id)
                        },
                        onPlayClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (featuredWord != null) {
                                audioPlayer.playWord(featuredWord)
                            } else {
                                audioPlayer.playAudio("singët", "")
                            }
                        }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "add-word") {
                AddWordRow(
                    modifier = Modifier.tourAnchor(TourAnchor.DictSubmitBanner),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToSubmitWord()
                    }
                )
            }
        }

        if (matchingCategories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "categories-heading") {
                SectionHeading(text = "Categories", modifier = Modifier.padding(top = Space.sm))
            }
            itemsIndexed(matchingCategories, key = { _, meta -> "category-${meta.name}" }) { _, meta ->
                val stats = uiState.categoryStats[meta.name] ?: CategoryProgressStats()
                CategoryTile(
                    meta = meta,
                    stats = stats,
                    accent = CategoryAccents[allCategoryMeta.indexOf(meta).coerceAtLeast(0) % CategoryAccents.size],
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToCategory(meta.name)
                    }
                )
            }
        }

        if (searching) {
            if (wordResults.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "words-heading") {
                    SectionHeading(text = "Words", modifier = Modifier.padding(top = Space.sm))
                }
                items(wordResults, key = { "word-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { word ->
                    WordResultRow(word = word, onClick = { onNavigateToWord(word.id) })
                }
            } else if (!searchSettled) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "searching") {
                    LoadingState(label = "Searching")
                }
            } else if (matchingCategories.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "no-results") {
                    EmptyState(
                        pose = JepjepPose.Curious,
                        title = "No words match \"$trimmed\"",
                        message = "Try the Tagalog or English meaning, or check the spelling. If the word " +
                            "really is missing, you can add it.",
                        actionLabel = "Add a word",
                        onAction = onNavigateToSubmitWord
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    KasiGuruTextField(
        value = query,
            label = { Text("Search words") },
        onValueChange = onQueryChange,
        placeholder = { Text("e.g. greetings, water, bahay") },
        leadingIcon = {
            Icon(painter = painterResource(id = Iconsax.Search), contentDescription = null, tint = Muted)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = Iconsax.CloseCircle),
                        contentDescription = "Clear search",
                        tint = Muted
                    )
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/**
 * The way into the community review queue. A quiet row, not a lime banner: the Words view is for
 * finding a word, and lime on this screen would compete with nothing but still claim to be the thing
 * to do.
 */
@Composable
private fun AddWordRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        onClick = onClick,
        contentPadding = PaddingValues(Space.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(44.dp).clip(Shapes.chip).background(Lime.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Add),
                    contentDescription = null,
                    tint = LimeText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(text = "Add a word", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = "Know a Kasiguranin word that is missing? Send it for review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            Spacer(Modifier.width(Space.sm))
            Icon(
                painter = painterResource(id = Iconsax.ArrowRight),
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WordOfTheDayCard(
    modifier: Modifier = Modifier,
    kasiguranin: String,
    translation: String,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.panel,
        border = BorderHairline,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = "Word of the day", style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = kasiguranin,
                    style = KasiguraninHeadword.copy(fontSize = 28.sp, lineHeight = 32.sp),
                    color = Ink
                )
                Text(text = translation, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            // Audio is Info, not lime: lime is for the thing to do, and this is something to hear.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Info)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.VolumeHigh),
                    contentDescription = "Play pronunciation",
                    tint = RewardInk,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** A category: its icon on a tinted tile, its name, and how much of it is learned, in words and a bar. */
@Composable
private fun CategoryTile(
    meta: CategoryMetaData,
    stats: CategoryProgressStats,
    accent: Color,
    onClick: () -> Unit
) {
    val fraction = if (stats.totalWords > 0) stats.learnedWords.toFloat() / stats.totalWords else 0f
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        onClick = onClick,
        contentPadding = PaddingValues(Space.md)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(Shapes.chip).background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (meta.customDrawableRes != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = meta.customDrawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = meta.iconRes),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            text = meta.name,
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = "${stats.totalWords} words · ${stats.learnedWords} learned",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(Space.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(Shapes.pill)
                .background(TrackNeutral)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(Shapes.pill)
                    .background(BrandLime)
            )
        }
    }
}

@Composable
private fun WordResultRow(word: VocabularyEntity, onClick: () -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.sm)
    ) {
        Text(text = word.kasiguranin, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(
            text = listOf(word.tagalog, word.english).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
