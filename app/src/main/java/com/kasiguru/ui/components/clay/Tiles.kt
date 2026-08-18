package com.kasiguru.ui.components.clay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLocked
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.PathTrackIdle
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.Violet

/**
 * A skill card with its completion percentage, after the reference's Reading / Listening / Speaking
 * grid. Used on Learn for the four things a KasiGuru learner can actually practise.
 *
 * The percentage is stated as text as well as drawn as a bar, so the progress does not depend on
 * reading a bar length.
 *
 * @param artwork optional illustration slot. Adrian supplies the artwork; with none present the tile
 *   falls back to a tinted disc so the layout never looks broken.
 */
@Composable
fun SkillTile(
    title: String,
    percent: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: (@Composable BoxScope.() -> Unit)? = null
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "SkillProgress"
    )

    SoftCard(
        modifier = modifier,
        shape = Shapes.tile,
        onClick = onClick,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(Shapes.chip)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            artwork?.invoke(this)
        }
        Spacer(Modifier.height(Space.sm))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$percent% complete",
            style = MaterialTheme.typography.labelSmall,
            color = Muted
        )
        Spacer(Modifier.height(Space.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(Shapes.pill)
                .background(TrackNeutral)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(5.dp)
                    .clip(Shapes.pill)
                    .background(accent)
            )
        }
    }
}

/** Where an activity sits in today's plan. */
enum class ActivityState { Done, Current, Upcoming, Locked }

/**
 * One activity in Today's Path: a lesson, a review deck, a story, a game.
 *
 * The leading tile is colour-coded by activity kind, matching the reference's lesson rows. State is
 * carried by more than colour — done rows dim their title and show a filled node, the current row is
 * the only one with a raised card, and locked rows state the requirement in words.
 */
@Composable
fun ActivityRow(
    title: String,
    subtitle: String,
    accent: Color,
    state: ActivityState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable BoxScope.() -> Unit)? = null
) {
    val isLocked = state == ActivityState.Locked
    val isDone = state == ActivityState.Done

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.tile,
        color = Surface,
        // Only the current activity is raised. A page where every row floats equally is the card-soup
        // failure in miniature.
        elevation = if (state == ActivityState.Current) 10.dp else 2.dp,
        onClick = if (isLocked) null else onClick,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(Shapes.chip)
                    .background(if (isLocked) NodeLocked else accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                leading?.invoke(this)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isLocked -> NodeLockedInk
                        isDone -> Muted
                        else -> Ink
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLocked) NodeLockedInk else Muted
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(Space.xs))
                trailing()
            }
        }
    }
}

/**
 * Wraps an activity in the dotted timeline rail, giving Today's Path its sequence.
 *
 * The rail is what turns a list of cards into a plan: it says these things happen in this order, and
 * it is the reason Learn does not read as another stack of tiles.
 */
@Composable
fun TimelineItem(
    state: ActivityState,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    railWidth: Dp = 28.dp,
    content: @Composable () -> Unit
) {
    // Canvas's draw block runs outside composition, so every themed colour it needs must be resolved
    // here first and captured as a plain value.
    val green = Green
    val pathTrackIdle = PathTrackIdle
    val violet = Violet
    val surface = Surface
    val nodeLocked = NodeLocked

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier.width(railWidth).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxHeight().width(railWidth)) {
                val centreX = size.width / 2f
                val nodeY = 30.dp.toPx()
                val nodeRadius = 6.dp.toPx()

                if (!isLast) {
                    drawLine(
                        color = if (state == ActivityState.Done) green.copy(alpha = 0.45f) else pathTrackIdle,
                        start = Offset(centreX, nodeY + nodeRadius + 4.dp.toPx()),
                        end = Offset(centreX, size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 5.dp.toPx())
                        )
                    )
                }

                when (state) {
                    ActivityState.Done -> drawCircle(green, nodeRadius, Offset(centreX, nodeY))
                    ActivityState.Current -> {
                        drawCircle(violet.copy(alpha = 0.22f), nodeRadius * 2f, Offset(centreX, nodeY))
                        drawCircle(violet, nodeRadius, Offset(centreX, nodeY))
                    }
                    else -> {
                        drawCircle(surface, nodeRadius, Offset(centreX, nodeY))
                        drawCircle(
                            color = if (state == ActivityState.Locked) nodeLocked else pathTrackIdle,
                            radius = nodeRadius,
                            center = Offset(centreX, nodeY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(Space.xs))
        Box(Modifier.weight(1f).padding(bottom = Space.sm)) { content() }
    }
}

/**
 * Section heading inside the sheet.
 *
 * Deliberately plain: no eyebrow label above it, no icon beside it. The old screens stacked an
 * uppercase chip over every heading, which added a line of noise to each section without adding
 * information.
 */
@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineSmall, color = Ink)
        action?.invoke()
    }
}

/** Muted caption used under a heading when a section genuinely needs one line of explanation. */
@Composable
fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Faint,
        modifier = modifier
    )
}
