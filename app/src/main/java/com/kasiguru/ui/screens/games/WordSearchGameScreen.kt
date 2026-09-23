package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.wordsearch.GridCell
import com.kasiguru.domain.wordsearch.PlacedWord
import com.kasiguru.domain.wordsearch.WordSearchPuzzle
import com.kasiguru.ui.components.AudioPlayButton
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.GameUnavailableState
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.CoralDeep
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.ui.theme.VioletTint
import com.kasiguru.util.audio.AudioPlayerManager

/** A found word's fill and the letter colour that reads on it. */
private data class WordHue(val fill: Color, val letter: Color)

/**
 * One hue per found word, in the order found. Gold and Coral carry Ink, never white (DESIGN.md's
 * third colour rule); Red is left out because in this app it means "wrong".
 */
@Composable
private fun wordHues(): List<WordHue> = listOf(
    WordHue(Violet, Color.White),
    WordHue(Coral, RewardInk),
    WordHue(Green, Color.White),
    WordHue(Gold, RewardInk),
    WordHue(VioletDeep, Color.White),
    WordHue(CoralDeep, RewardInk),
    WordHue(GoldDeep, RewardInk),
    WordHue(Ink, Surface)
)

@Composable
fun WordSearchGameScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNextLevel: ((Int) -> Unit)? = null,
    viewModel: WordSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayerManager.stopAudio() }
    }

    val exitGuard = rememberGameExitGuard(
        active = !uiState.isLoading && !uiState.isGameOver && !uiState.isUnavailable,
        onExit = onNavigateBack
    )

    GroundScaffold(
        title = uiState.category.ifBlank { "Word Search" },
        subtitle = "Level ${uiState.level} · ${uiState.tier.name}",
        onBack = exitGuard,
        navIcon = Iconsax.CloseCircle,
        pattern = GroundPattern.None,
        compactTitle = true,
        content = {
            val puzzle = uiState.puzzle
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                uiState.isUnavailable || puzzle == null -> GameUnavailableState(
                    accentColor = Violet,
                    onBack = onNavigateBack,
                    title = "Not enough words for this level yet",
                    message = "This category needs more short words before this grid can be built. " +
                        "Try another category, or an earlier level."
                )
                uiState.isGameOver -> GameOverView(
                    score = puzzle.words.size,
                    total = puzzle.words.size,
                    xpEarned = uiState.finalXp,
                    starsEarned = uiState.starsEarned,
                    onFinish = onNavigateBack,
                    onNextLevel = uiState.nextLevel?.let { next ->
                        onNavigateToNextLevel?.let { navigate -> { navigate(next) } }
                    }
                )
                else -> PlayingState(
                    uiState = uiState,
                    puzzle = puzzle,
                    onCellTapped = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onCellTapped(it)
                    },
                    onPlayWord = { id -> uiState.entries[id]?.let(audioPlayerManager::playWord) }
                )
            }
        }
    )
}

@Composable
private fun PlayingState(
    uiState: WordSearchUiState,
    puzzle: WordSearchPuzzle,
    onCellTapped: (GridCell) -> Unit,
    onPlayWord: (Int) -> Unit
) {
    val hues = wordHues()
    // Which found word owns each cell, so the grid and the list share one colour per word.
    val cellOwner: Map<GridCell, Int> = remember(uiState.foundIds, puzzle) {
        buildMap {
            uiState.foundIds.forEachIndexed { order, id ->
                puzzle.words.first { it.id == id }.cells.forEach { put(it, order) }
            }
        }
    }
    val found = uiState.foundIds.size
    val total = puzzle.words.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter, vertical = Space.md)
    ) {
        GameHeader(
            label = "Found $found of $total",
            progress = found / total.toFloat(),
            score = found,
            accentStart = Violet,
            accentEnd = VioletDeep
        )
        Spacer(Modifier.height(Space.sm))
        StatusLine(uiState)
        Spacer(Modifier.height(Space.sm))

        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.panel,
            contentPadding = PaddingValues(Space.sm)
        ) {
            LetterGrid(
                puzzle = puzzle,
                selection = uiState.selectionStart,
                cellOwner = cellOwner,
                hues = hues,
                onCellTapped = onCellTapped
            )
        }

        Spacer(Modifier.height(Space.lg))
        Text("Find these words", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(Space.xs))
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            puzzle.words.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    pair.forEach { word ->
                        val order = uiState.foundIds.indexOf(word.id)
                        WordChip(
                            word = word,
                            meaning = uiState.entries[word.id]?.let(::tagalogGloss).orEmpty(),
                            hue = hues.getOrNull(order % hues.size).takeIf { order >= 0 },
                            onPlay = { onPlayWord(word.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusLine(uiState: WordSearchUiState) {
    val (text, isMiss) = when {
        uiState.lastTapMissed -> "That line isn't one of the words. Try again." to true
        uiState.selectionStart != null -> "Now tap the word's last letter." to false
        else -> "Tap a word's first letter, then its last letter." to false
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // Announced as it changes, so a TalkBack user hears whether a line was a word.
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Icon(
            painter = painterResource(id = if (isMiss) Iconsax.CloseCircle else Iconsax.InfoCircle),
            contentDescription = null,
            tint = if (isMiss) Red else Muted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Space.xs))
        // Ink, not Red: red text on the lavender Ground falls under 4.5:1. The icon carries the colour.
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isMiss) FontWeight.Bold else FontWeight.Normal,
            color = if (isMiss) Ink else Muted
        )
    }
}

@Composable
private fun LetterGrid(
    puzzle: WordSearchPuzzle,
    selection: GridCell?,
    cellOwner: Map<GridCell, Int>,
    hues: List<WordHue>,
    onCellTapped: (GridCell) -> Unit
) {
    val gap = when {
        puzzle.size <= 6 -> 6.dp
        puzzle.size <= 8 -> 5.dp
        else -> 4.dp
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tile = (maxWidth - gap * (puzzle.size - 1)) / puzzle.size
        // Letter size follows the tile, not the system font scale: the grid is a fixed number of
        // cells across, and a scaled-up letter would overflow its tile rather than get easier to read.
        // Dp.toSp divides the font scale back out, so the drawn letter stays 46% of the tile.
        val letterSize = with(LocalDensity.current) { (tile * 0.46f).toSp() }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until puzzle.size) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until puzzle.size) {
                        val cell = GridCell(row, col)
                        val owner = cellOwner[cell]
                        val isSelected = cell == selection
                        val hue = owner?.let { hues[it % hues.size] }
                        val letter = puzzle.letterAt(cell)
                        Box(
                            modifier = Modifier
                                .size(tile)
                                .clip(RoundedCornerShape(tile * 0.28f))
                                .background(
                                    when {
                                        isSelected -> VioletTint
                                        hue != null -> hue.fill
                                        else -> TrackNeutral
                                    }
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, Violet, RoundedCornerShape(tile * 0.28f))
                                    else Modifier
                                )
                                .clickable(role = Role.Button) { onCellTapped(cell) }
                                .semantics {
                                    contentDescription = "Row ${row + 1}, column ${col + 1}, $letter"
                                    stateDescription = when {
                                        isSelected -> "First letter selected"
                                        owner != null -> "Part of a found word"
                                        else -> ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = letterSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = hue?.letter ?: Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordChip(
    word: PlacedWord,
    meaning: String,
    hue: WordHue?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFound = hue != null
    SoftCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            stateDescription = if (isFound) "Found" else "Not found yet"
        },
        shape = Shapes.chip,
        elevation = if (isFound) 0.dp else 2.dp,
        contentPadding = PaddingValues(start = Space.sm, top = Space.xs, bottom = Space.xs, end = Space.xxs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(hue?.fill ?: TrackNeutral)
            )
            Spacer(Modifier.width(Space.xs))
            Column(Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    textDecoration = if (isFound) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meaning.isNotBlank()) {
                    Text(
                        text = meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Hearing the word is the reward for finding it, so the speaker appears once it is found.
            if (isFound) {
                AudioPlayButton(onClick = onPlay, size = 40.dp, contentDescription = "Play ${word.word}")
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
