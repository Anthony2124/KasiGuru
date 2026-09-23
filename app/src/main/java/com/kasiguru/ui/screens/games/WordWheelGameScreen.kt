package com.kasiguru.ui.screens.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.wordwheel.BoardCell
import com.kasiguru.domain.wordwheel.WordWheelPuzzle
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.GameUnavailableState
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.ui.theme.VioletTint
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WordWheelGameScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNextLevel: ((Int) -> Unit)? = null,
    viewModel: WordWheelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exitGuard = rememberGameExitGuard(
        active = !uiState.isLoading && !uiState.isGameOver && !uiState.isUnavailable,
        onExit = onNavigateBack
    )

    GroundScaffold(
        title = "Word Wheel",
        subtitle = "Level ${uiState.level} · ${uiState.tier.name}",
        onBack = exitGuard,
        navIcon = Iconsax.CloseCircle,
        pattern = GroundPattern.None,
        compactTitle = true,
        content = {
            val puzzle = uiState.puzzle
            when {
                uiState.isLoading -> Loading()
                uiState.isUnavailable || puzzle == null -> GameUnavailableState(
                    accentColor = Violet,
                    onBack = onNavigateBack,
                    title = "Not enough words for this level yet",
                    message = "The dictionary on this phone can't build this wheel yet. " +
                        "Try again after the next dictionary update."
                )
                // Stars arrive once the result is saved; until then the level is over but not scored.
                uiState.isGameOver && uiState.starsEarned == 0 -> Loading()
                uiState.isGameOver -> GameOverView(
                    score = puzzle.slots.size,
                    total = puzzle.slots.size,
                    xpEarned = uiState.finalXp,
                    starsEarned = uiState.starsEarned,
                    onFinish = onNavigateBack,
                    onNextLevel = uiState.nextLevel?.let { next ->
                        onNavigateToNextLevel?.let { navigate -> { navigate(next) } }
                    }
                )
                else -> Playing(uiState, puzzle, viewModel)
            }
        }
    )
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Violet)
    }
}

@Composable
private fun Playing(uiState: WordWheelUiState, puzzle: WordWheelPuzzle, viewModel: WordWheelViewModel) {
    val haptic = LocalHapticFeedback.current
    val found = uiState.foundSlots.size
    val total = puzzle.slots.size

    // No vertical scroll: a scroll container would steal the vertical part of every swipe across the
    // wheel. The board takes whatever height is left and scales its tiles to fit instead.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wheelSize = min(260.dp, maxHeight * 0.36f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.gutter, vertical = Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                label = "Found $found of $total",
                progress = found / total.toFloat(),
                score = found,
                accentStart = Violet,
                accentEnd = VioletDeep,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.sm))
            Board(puzzle = puzzle, uiState = uiState)
            Spacer(Modifier.height(Space.sm))
            FeedbackLine(uiState)
            Spacer(Modifier.height(Space.xs))
            AttemptPill(uiState.attempt)
            Spacer(Modifier.height(Space.sm))
            LetterWheel(
                letters = puzzle.wheel,
                order = uiState.wheelOrder,
                selection = uiState.selection,
                size = wheelSize,
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onLetterTapped(it)
                },
                onDragOver = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onLetterDraggedOver(it)
                },
                onDragEnd = viewModel::submit
            )
            Spacer(Modifier.height(Space.sm))
            Controls(uiState, viewModel)
        }
    }
}

@Composable
private fun ColumnScope.Board(puzzle: WordWheelPuzzle, uiState: WordWheelUiState) {
    val shown = uiState.shownCells
    val foundCells = uiState.foundSlots.flatMap { puzzle.slots[it].cells }.toSet()
    val gap = 4.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clearAndSetSemantics {
                contentDescription = "Crossword, ${uiState.foundSlots.size} of ${puzzle.slots.size} words found"
            },
        contentAlignment = Alignment.Center
    ) {
        val tile: Dp = minOf(
            (maxWidth - gap * (puzzle.cols - 1)) / puzzle.cols,
            (maxHeight - gap * (puzzle.rows - 1)) / puzzle.rows,
            40.dp
        )
        val letterSize = with(LocalDensity.current) { (tile * 0.5f).toSp() }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until puzzle.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until puzzle.cols) {
                        val cell = BoardCell(row, col)
                        val letter = puzzle.letterAt[cell]
                        if (letter == null) {
                            Spacer(Modifier.size(tile))
                            continue
                        }
                        val isFound = cell in foundCells
                        val isShown = cell in shown
                        Box(
                            modifier = Modifier
                                .size(tile)
                                .clip(RoundedCornerShape(tile * 0.22f))
                                .background(
                                    when {
                                        isFound -> Violet
                                        isShown -> VioletTint
                                        else -> Surface
                                    }
                                )
                                .then(
                                    if (isShown) Modifier
                                    else Modifier.border(1.5.dp, TrackNeutral, RoundedCornerShape(tile * 0.22f))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isShown) {
                                Text(
                                    text = letter,
                                    fontSize = letterSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isFound) androidx.compose.ui.graphics.Color.White else Violet
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackLine(uiState: WordWheelUiState) {
    val (text, isMiss) = when (val f = uiState.feedback) {
        is WheelFeedback.Found -> (if (f.gloss.isBlank()) "Found ${f.word}!" else "Found ${f.word}: ${f.gloss}") to false
        is WheelFeedback.Bonus -> (if (f.gloss.isBlank()) "Bonus word ${f.word}!" else "Bonus word ${f.word}: ${f.gloss}") to false
        is WheelFeedback.AlreadyFound -> "You already found ${f.word}." to false
        is WheelFeedback.NotAWord -> "${f.attempt.lowercase()} isn't in the dictionary." to true
        WheelFeedback.TooShort -> "Words need at least 3 letters." to true
        is WheelFeedback.Revealed -> "Hint: one letter uncovered." to false
        null -> "Swipe across the letters, or tap them and press Check." to false
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            // Ink, not Red: red text on the Ground falls under 4.5:1, so a miss is marked by weight.
            fontWeight = if (isMiss) FontWeight.Bold else FontWeight.Normal,
            color = if (isMiss) Ink else Muted,
            modifier = Modifier.weight(1f)
        )
        if (uiState.bonusFound.isNotEmpty()) {
            Spacer(Modifier.width(Space.xs))
            Text(
                text = "Bonus ${uiState.bonusFound.size}",
                style = MaterialTheme.typography.labelMedium,
                color = RewardInk,
                modifier = Modifier
                    .clip(Shapes.pill)
                    .background(Gold)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AttemptPill(attempt: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(Shapes.pill)
            .background(if (attempt.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else Violet)
            .padding(horizontal = Space.md, vertical = Space.xs)
            .semantics { contentDescription = if (attempt.isEmpty()) "No letters picked" else "Spelling $attempt" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = attempt,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The letter wheel. One pointer handler for the whole wheel tells a tap from a swipe: movement past
 * touch slop is a swipe that picks up every letter it crosses and submits on release; anything less
 * is a tap on the letter under the finger. Each letter also carries a semantics click, so TalkBack
 * users spell by double-tapping letters and pressing Check.
 */
@Composable
private fun LetterWheel(
    letters: List<String>,
    order: List<Int>,
    selection: List<Int>,
    size: Dp,
    onTap: (Int) -> Unit,
    onDragOver: (Int) -> Unit,
    onDragEnd: () -> Unit
) {
    val letterSize = size * 0.24f
    val density = LocalDensity.current
    val centerPx = with(density) { (size / 2).toPx() }
    val radiusPx = with(density) { (size / 2 - letterSize / 2 - 10.dp).toPx() }
    val hitRadiusPx = with(density) { (letterSize / 2).toPx() } * 1.15f
    val positions: Map<Int, Offset> = remember(order, centerPx, radiusPx) {
        order.mapIndexed { slot, wheelIndex ->
            val angle = -Math.PI / 2 + 2 * Math.PI * slot / order.size
            wheelIndex to Offset(
                centerPx + radiusPx * cos(angle).toFloat(),
                centerPx + radiusPx * sin(angle).toFloat()
            )
        }.toMap()
    }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val tap by rememberUpdatedState(onTap)
    val dragOver by rememberUpdatedState(onDragOver)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val lineColor = Violet.copy(alpha = 0.45f)

    fun hit(point: Offset): Int? =
        positions.entries.firstOrNull { (it.value - point).getDistance() <= hitRadiusPx }?.key

    Box(
        modifier = Modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = Violet, spotColor = Violet)
            .clip(CircleShape)
            .background(Surface)
            .pointerInput(positions) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = hit(down.position)
                    var dragging = false
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!dragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                            start?.let(dragOver)
                        }
                        if (dragging) {
                            finger = change.position
                            hit(change.position)?.let(dragOver)
                            change.consume()
                        }
                    }
                    finger = null
                    if (dragging) dragEnd() else start?.let(tap)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val points = selection.mapNotNull(positions::get) + listOfNotNull(finger)
            val stroke = with(density) { 10.dp.toPx() }
            points.zipWithNext().forEach { (a, b) ->
                drawLine(lineColor, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
        val letterPx = with(density) { letterSize.toPx() }
        val letterSp = with(density) { (letterSize * 0.46f).toSp() }
        for ((index, center) in positions) {
            val isSelected = index in selection
            Box(
                modifier = Modifier
                    .offset { IntOffset((center.x - letterPx / 2).toInt(), (center.y - letterPx / 2).toInt()) }
                    .size(letterSize)
                    .clip(CircleShape)
                    .background(if (isSelected) Violet else VioletTint)
                    .semantics {
                        contentDescription = "Letter ${letters[index]}"
                        role = Role.Button
                        selected = isSelected
                        onClick { tap(index); true }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letters[index],
                    fontSize = letterSp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else Ink
                )
            }
        }
    }
}

@Composable
private fun Controls(uiState: WordWheelUiState, viewModel: WordWheelViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        ControlIcon(Iconsax.Refresh, "Shuffle letters", onClick = viewModel::shuffle)
        ControlIcon(
            iconRes = Iconsax.Flash,
            label = if (uiState.hintsUsed == 0) "Hint: uncover a letter" else "Hint, ${uiState.hintsUsed} used",
            onClick = viewModel::hint
        )
        ControlIcon(Iconsax.CloseCircle, "Clear letters", onClick = viewModel::clearSelection, enabled = uiState.selection.isNotEmpty())
        ClayButton(
            label = "Check",
            onClick = viewModel::submit,
            enabled = uiState.selection.isNotEmpty(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlIcon(iconRes: Int, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) VioletTint else TrackNeutral)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = if (enabled) Violet else Muted,
            modifier = Modifier.size(22.dp)
        )
    }
}
