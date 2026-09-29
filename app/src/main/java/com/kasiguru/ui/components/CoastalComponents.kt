package com.kasiguru.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.motionTween

/**
 * Segmented progress: one pill per step, the ones reached so far filled in [BrandLime].
 *
 * Segments rather than one continuous bar because the steps are discrete things the learner does,
 * and counting them is the point ("three more"). Used by the onboarding frame; replaces the old dot
 * stepper, whose 7 dp dots were too small to read at a glance.
 *
 * Read by screen readers as one progress bar ("Step 3 of 12"), not as twelve unlabelled boxes.
 *
 * @param filled how many segments are filled, counting the current step.
 */
@Composable
fun SegmentedProgressBar(
    total: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    fillColor: Color = BrandLime,
    trackColor: Color = TrackNeutral,
    segmentHeight: Dp = 6.dp,
    gap: Dp = 4.dp
) {
    val clamped = filled.coerceIn(0, total.coerceAtLeast(0))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Step $clamped of $total"
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = clamped.toFloat(),
                range = 0f..total.coerceAtLeast(1).toFloat()
            )
        },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                targetValue = if (index < clamped) fillColor else trackColor,
                animationSpec = motionTween(Motion.Standard),
                label = "SegmentFill"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(segmentHeight)
                    .clip(Shapes.pill)
                    .background(color)
            )
        }
    }
}
