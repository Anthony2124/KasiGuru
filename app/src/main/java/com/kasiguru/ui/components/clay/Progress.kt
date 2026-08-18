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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.OnCanopyDecor
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint

/**
 * Circular progress ring with a free content slot at its centre.
 *
 * @param onCanopy when true the track uses the canopy decoration tone instead of the light-surface
 *   track. Getting this wrong makes the ring invisible, which is exactly the bug the old progress bar
 *   shipped with.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    strokeWidth: Dp = 9.dp,
    color: Color = Gold,
    onCanopy: Boolean = false,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ProgressRing"
    )
    val trackColor = if (onCanopy) OnCanopyDecor else TrackNeutral

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val radius = (this.size.minDimension - stroke) / 2f
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(color = trackColor, radius = radius, center = centre, style = Stroke(stroke))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(centre.x - radius, centre.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** One day in [WeekStrip]. */
enum class DayState {
    /** Practised — the streak held. */
    Done,

    /** Today, not yet practised. */
    Today,

    /** Today, already practised. */
    TodayDone,

    /** A past day the learner missed. */
    Missed,

    /** Still to come this week. */
    Upcoming
}

data class DayMark(val label: String, val dayOfMonth: Int, val state: DayState)

/**
 * The seven-day strip from the reference set, carrying the learning streak.
 *
 * A streak is the single strongest retention mechanic the app already has data for, and today it is
 * shown only as a number in a badge. Laying the week out makes the gap visible — you can see the day
 * you missed, which is what makes the streak feel worth protecting.
 *
 * Colour alone never carries the state: done days also show a filled dot, and missed days are drawn
 * hollow, so the strip stays readable without colour vision.
 */
@Composable
fun WeekStrip(
    days: List<DayMark>,
    modifier: Modifier = Modifier,
    onCanopy: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        days.forEach { day ->
            val isCurrent = day.state == DayState.Today || day.state == DayState.TodayDone
            val practised = day.state == DayState.Done || day.state == DayState.TodayDone

            val cellColor = when {
                isCurrent && onCanopy -> Surface
                isCurrent -> Violet
                onCanopy -> OnCanopyDecor
                else -> VioletTint
            }
            val numberColor = when {
                isCurrent && onCanopy -> Violet
                isCurrent -> Surface
                onCanopy -> OnCanopy
                else -> Ink
            }
            val labelColor = if (onCanopy) OnCanopy else Muted

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = buildString {
                            append(day.label)
                            append(" ")
                            append(day.dayOfMonth)
                            append(
                                when (day.state) {
                                    DayState.Done, DayState.TodayDone -> ", practised"
                                    DayState.Missed -> ", missed"
                                    DayState.Today -> ", today, not yet practised"
                                    DayState.Upcoming -> ", upcoming"
                                }
                            )
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xxs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(Shapes.chip)
                        .background(cellColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = numberColor
                        )
                        // The dot repeats the state without relying on colour.
                        if (practised) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .size(4.dp)
                                    .clip(Shapes.pill)
                                    .background(if (isCurrent && onCanopy) Violet else Gold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One column in [StatStrip]. */
data class Stat(val label: String, val value: String)

/**
 * The dividered stat row from the reference: points, rank, level side by side.
 *
 * Sits inside the canopy, so its text is pure white — faded white fails AA against the canopy at
 * every alpha, and the divider is the only element allowed to use the decoration tone.
 */
@Composable
fun StatStrip(
    stats: List<Stat>,
    modifier: Modifier = Modifier,
    onCanopy: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { index, stat ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (onCanopy) OnCanopy else Ink
                )
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (onCanopy) OnCanopy else Muted,
                    textAlign = TextAlign.Center
                )
            }
            if (index != stats.lastIndex) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = Space.xs)
                        .background(if (onCanopy) OnCanopyDecor else TrackNeutral)
                )
            }
        }
    }
}


/**
 * Seven-day XP bar chart.
 *
 * Bars are labelled by day and the tallest is highlighted, so the chart answers "how am I doing this
 * week" at a glance rather than requiring the reader to compare heights precisely. Values are drawn
 * from real XP history; an all-zero week renders flat bars rather than an empty frame.
 */
@Composable
fun XpBarChart(
    values: List<Int>,
    dayLabels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = Coral,
    height: Dp = 96.dp
) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            val fraction = value.toFloat() / maxValue
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, delayMillis = index * 40, easing = FastOutSlowInEasing),
                label = "XpBar"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clearAndSetSemantics {
                        contentDescription = "${dayLabels.getOrNull(index) ?: ""}: $value XP"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // A zero day still shows a visible stub, so the axis reads as a week rather
                        // than as missing data.
                        .height(((height - 18.dp) * animatedFraction).coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (value == maxValue && value > 0) barColor else barColor.copy(alpha = 0.45f))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dayLabels.getOrNull(index).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Faint
                )
            }
        }
    }
}
