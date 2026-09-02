package com.kasiguru.ui.screens.lesson

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.lesson.Exercise
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.GreenTint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint

/**
 * The two exercise shapes that need more than a list of answer buttons.
 *
 * Both are tap-driven, never drag-driven. WCAG 2.2 requires a single-pointer alternative to any
 * dragging the author invents, and a word bank you can only reorder by dragging fails that outright
 * — so tapping is not a simplification here, it is the accessible design.
 */

// ── Build the sentence ──────────────────────────────────────────────────────────

/**
 * Word chips tapped into order, with the sentence's meaning as the prompt.
 *
 * Placement is tracked by chip *index*, not by the word itself: real sentences repeat words —
 * "Namúgtong ang anák ng mángga" has one *ang*, but "Me tólay sa baláy" and its negation share
 * three — and keying on the string would make two identical chips indistinguishable, so removing one
 * would remove the wrong one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilder(
    exercise: Exercise.SentenceBuild,
    enabled: Boolean,
    onSentenceChanged: (String) -> Unit
) {
    val placed = remember(exercise) { mutableStateListOf<Int>() }

    // The joined sentence is the answer the lesson grades, so it is reported on every change rather
    // than read out of this composable at check time.
    LaunchedEffect(placed.size, exercise) {
        onSentenceChanged(placed.joinToString(" ") { exercise.options[it] })
    }

    Column(Modifier.fillMaxWidth()) {
        // ── The line being built ──
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(Shapes.tile)
                .background(SurfaceSunken)
                .padding(Space.md)
        ) {
            if (placed.isEmpty()) {
                Text(
                    text = "Tap the words below, in order",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    verticalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    placed.forEach { index ->
                        WordChip(
                            label = exercise.options[index],
                            placed = true,
                            enabled = enabled,
                            onClick = { placed.remove(index) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Space.md))

        // ── The bank ──
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            exercise.options.forEachIndexed { index, option ->
                val isPlaced = index in placed
                // The chip keeps its slot once used rather than disappearing, so the bank does not
                // reflow under the finger between taps and send the next tap to a different word.
                Box(Modifier.alpha(if (isPlaced) 0f else 1f)) {
                    WordChip(
                        label = option,
                        placed = false,
                        enabled = enabled && !isPlaced,
                        onClick = { if (!isPlaced) placed.add(index) }
                    )
                }
            }
        }
    }
}

/** One tappable word. Placed chips are filled; bank chips are outlined. */
@Composable
private fun WordChip(
    label: String,
    placed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(Shapes.pill)
            .background(if (placed) Violet else Surface)
            .border(
                width = if (placed) 0.dp else 1.5.dp,
                color = if (placed) Color.Transparent else VioletTint,
                shape = Shapes.pill
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W700,
            color = if (placed) Color.White else Ink
        )
    }
}

// ── Match the pairs ─────────────────────────────────────────────────────────────

/**
 * Two shuffled columns; tap a word, then its meaning.
 *
 * Self-grading, so it reports completion rather than an answer to check: the run cannot continue
 * until every pair is matched, and a wrong attempt is shown and cleared rather than banked. That is
 * the honest shape for a matching exercise — there is no state in which the learner has "finished it
 * wrongly", only one in which they have not finished.
 */
@Composable
fun MatchPairsBoard(
    exercise: Exercise.MatchPairs,
    enabled: Boolean,
    onSolved: () -> Unit
) {
    val words = remember(exercise) { exercise.pairs.map { it.first }.shuffled() }
    val meanings = remember(exercise) { exercise.pairs.map { it.second }.shuffled() }
    val matched = remember(exercise) { mutableStateListOf<String>() }
    var selectedWord by remember(exercise) { mutableStateOf<String?>(null) }
    var wrongMeaning by remember(exercise) { mutableStateOf<String?>(null) }

    LaunchedEffect(matched.size, exercise) {
        if (matched.size == exercise.pairs.size) onSolved()
    }

    // A wrong pairing shows itself, then clears, so the learner sees which two they tried.
    LaunchedEffect(wrongMeaning) {
        if (wrongMeaning != null) {
            kotlinx.coroutines.delay(600)
            wrongMeaning = null
            selectedWord = null
        }
    }

    // Both columns use the same fixed tile height. Left to themselves the two columns size their
    // rows independently, so a meaning that wraps to two lines pushes everything below it down and
    // the pairs stop lining up across the board -- which makes a grid of pairs read as two unrelated
    // lists.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            words.forEach { word ->
                val isMatched = word.kasiguranin in matched
                MatchTile(
                    label = word.kasiguranin,
                    isSelected = selectedWord == word.kasiguranin,
                    isMatched = isMatched,
                    isWrong = false,
                    enabled = enabled && !isMatched,
                    onClick = { selectedWord = word.kasiguranin }
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            meanings.forEach { meaning ->
                val owner = exercise.pairs.first { it.second == meaning }.first.kasiguranin
                val isMatched = owner in matched
                MatchTile(
                    label = meaning,
                    isSelected = false,
                    isMatched = isMatched,
                    isWrong = wrongMeaning == meaning,
                    enabled = enabled && !isMatched && selectedWord != null,
                    onClick = {
                        val picked = selectedWord ?: return@MatchTile
                        if (picked == owner) {
                            matched.add(picked)
                            selectedWord = null
                        } else {
                            wrongMeaning = meaning
                        }
                    }
                )
            }
        }
    }
}

/**
 * One tile in the matching board.
 *
 * Matched tiles carry a tick as well as the green fill, and the tile's spoken description says its
 * state in words. Colour alone would leave the whole exercise unreadable to a learner who cannot
 * separate the green from the violet.
 */
@Composable
private fun MatchTile(
    label: String,
    isSelected: Boolean,
    isMatched: Boolean,
    isWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isWrong) 0.96f else 1f, label = "MatchTileNudge")

    val background = when {
        isMatched -> GreenTint
        isSelected -> Violet
        else -> Surface
    }
    val borderColour = when {
        isMatched -> Green
        isSelected -> Violet
        isWrong -> Faint
        else -> VioletTint
    }
    val labelColour = when {
        isSelected -> Color.White
        isMatched -> Ink
        else -> Ink
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TILE_HEIGHT)
            .clip(Shapes.chip)
            .background(background)
            .border(if (isMatched || isSelected) 2.dp else 1.5.dp, borderColour, Shapes.chip)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.sm, vertical = Space.xs)
            .alpha(scale)
            .clearAndSetSemantics {
                contentDescription = when {
                    isMatched -> "$label, matched"
                    isSelected -> "$label, selected"
                    else -> label
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMatched) {
            Icon(
                painter = painterResource(id = Iconsax.TickCircle),
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Space.xxs))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isMatched || isSelected) FontWeight.W700 else FontWeight.W500,
            color = labelColour,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Height of every tile on the matching board.
 *
 * Fixed rather than intrinsic: two lines of a wrapped meaning fit, and every tile matching every
 * other is what lets the eye read the board as pairs rather than as two stacks. It is also a
 * comfortable target, well past the 48 dp floor.
 */
private val TILE_HEIGHT = 72.dp
