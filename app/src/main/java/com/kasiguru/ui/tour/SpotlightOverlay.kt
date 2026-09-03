package com.kasiguru.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Scrim
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.motionTween
import kotlin.math.max
import kotlin.math.roundToInt

/** How opaque the dim is. Measured, not guessed: the app behind stays legible as context. */
private const val ScrimAlpha = 0.72f

/**
 * The guided tour's spotlight: a dim over the whole app with one element cut out of it, and a card
 * saying what that element is for.
 *
 * Mounted as the last child of the navigation root so it covers the nav host *and* the floating
 * bottom bar. It cannot be a `Dialog` the way the app's other overlays are - a dialog is a separate
 * window with its own dim, so it could neither show the live app through a hole nor share the
 * coordinate space the anchors report their bounds in.
 *
 * @param anchors the live anchor positions. Read here rather than at the call site so that a
 *   measurement recomposes this overlay alone, instead of the whole navigation graph - which would
 *   relayout the anchors, which would re-report their bounds.
 * @param anchorVisible whether the destination this stop describes is the one actually showing. When
 *   it is not, the screen dims without a hole, which is the honest thing to show for the frame or
 *   two a tab switch takes.
 * @param bottomBlocked how much of the bottom edge the floating navigation cluster occupies, so the
 *   caption is never placed underneath it.
 */
@Composable
fun SpotlightOverlay(
    stop: TourStop,
    chapterTitle: String,
    stepIndex: Int,
    stepCount: Int,
    anchors: TourAnchorRegistry,
    anchorVisible: Boolean,
    bottomBlocked: Dp,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isLast = stepIndex == stepCount - 1

    // Back is handled here rather than left to fall through. At stop 1 the back stack holds only
    // Learn - onboarding was popped inclusively on the way in - so falling through would pop it and
    // drop the learner at the launcher with this overlay still drawn over the app.
    BackHandler(enabled = true) { onBack() }

    val padPx = with(density) { stop.pad.toPx() }
    val cornerPx = with(density) { stop.corner.toPx() }
    val gapPx = with(density) { Space.md.toPx() }
    val safeTopPx = WindowInsets.statusBars.getTop(density) + gapPx
    val bottomBlockedPx = with(density) { bottomBlocked.toPx() }
    val navBarPx = WindowInsets.navigationBars.getBottom(density)

    // A stop with no anchor explains a screen rather than a control: dim, no hole, caption centred.
    val hole = stop.anchor
        ?.takeIf { anchorVisible }
        ?.let { anchors.boundsOf(it) }
        ?.inflate(padPx)

    // Keep the last known rectangle so a move between two measured anchors animates rather than
    // cutting. While no anchor is measured nothing is drawn, so a stale rect can never be painted
    // over a screen it does not belong to.
    var lastHole by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(hole) { if (hole != null) lastHole = hole }

    val animatedHole by animateValueAsState(
        targetValue = hole ?: lastHole ?: Rect.Zero,
        typeConverter = Rect.VectorConverter,
        animationSpec = motionTween(Motion.Standard),
        label = "tourHole"
    )
    val drawnHole = if (hole != null) animatedHole else null

    val scrimColor = Scrim.copy(alpha = ScrimAlpha)
    val ringColor = OnCanopy.copy(alpha = 0.9f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Without an offscreen layer, BlendMode.Clear clears the window's own alpha instead
                // of the scrim drawn a line above, and the "hole" renders as a solid black rectangle
                // on device while still looking correct in a software-canvas preview.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(Unit) {
                    // Swallow everything. The tour drives navigation across five tabs, so a stray
                    // tap on a live control would strand the overlay pointing at an anchor that no
                    // longer exists. Skip is one tap away on every stop, which is what pays for it.
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            drawRect(color = scrimColor)
            drawnHole?.let { rect ->
                drawRoundRect(
                    color = Color.Black,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = ringColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        CaptionCard(
            stop = stop,
            chapterTitle = chapterTitle,
            stepIndex = stepIndex,
            stepCount = stepCount,
            isLast = isLast,
            onBack = onBack,
            onSkip = onSkip,
            onNext = onNext,
            modifier = Modifier
                .layout { measurable, constraints ->
                    val containerH = constraints.maxHeight
                    val safeBottomPx = containerH - bottomBlockedPx - navBarPx - gapPx

                    // Bound the height before measuring, so the branch chosen below is one the card
                    // is already known to fit. Font scale 1.3 on a short screen is the case this
                    // exists for: a clipped Next button is worse than a card that scrolls.
                    val room = drawnHole?.let {
                        max(safeBottomPx - (it.bottom + gapPx), (it.top - gapPx) - safeTopPx)
                    } ?: (safeBottomPx - safeTopPx)
                    val maxH = room.coerceIn(0f, containerH.toFloat()).roundToInt()

                    val placeable = measurable.measure(
                        constraints.copy(minHeight = 0, maxHeight = maxH)
                    )
                    val cardH = placeable.height

                    val y = when {
                        drawnHole == null -> (containerH - cardH) / 2f
                        // Below first: a caption under the thing it describes matches reading order.
                        safeBottomPx - (drawnHole.bottom + gapPx) >= cardH ->
                            drawnHole.bottom + gapPx

                        (drawnHole.top - gapPx) - safeTopPx >= cardH ->
                            drawnHole.top - gapPx - cardH

                        // Neither side fits outright. The height cap above already sized the card to
                        // the roomier side, so pin it there.
                        (drawnHole.top - gapPx) - safeTopPx > safeBottomPx - (drawnHole.bottom + gapPx) ->
                            safeTopPx

                        else -> safeBottomPx - cardH
                    }

                    val clamped = y.coerceIn(safeTopPx, max(safeTopPx, safeBottomPx - cardH))
                    layout(constraints.maxWidth, containerH) {
                        placeable.place(0, clamped.roundToInt())
                    }
                }
                .padding(horizontal = Space.gutter)
        )
    }
}

@Composable
private fun CaptionCard(
    stop: TourStop,
    chapterTitle: String,
    stepIndex: Int,
    stepCount: Int,
    isLast: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        contentPadding = PaddingValues(Space.md)
    ) {
        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    // The chapter's name earns its place here: an optional chapter can be launched
                    // from the help page long after the core tour, and "4 of 6" alone does not say
                    // which of six things the learner is in the middle of.
                    text = "$chapterTitle  ·  ${stepIndex + 1} of $stepCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                if (!isLast) {
                    // A TextButton, not a ClayButton: ClayButton's inner row is fillMaxWidth, so in a
                    // SpaceBetween row it swallows all the remaining space and collides with the
                    // counter. Low-emphasis inline actions already use TextButton elsewhere (Profile's
                    // "See all"), and Skip should not compete with Next anyway.
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.defaultMinSize(minHeight = Touch.minTarget)
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = Violet
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.sm))

            Text(
                text = stop.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )

            Spacer(Modifier.height(Space.xxs))

            Text(
                text = stop.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }

        Spacer(Modifier.height(Space.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stepIndex > 0) {
                // Also a TextButton. ClayButtonTone.Quiet is a Surface face, which is exactly the
                // colour of the card it would sit on - a white button on a white card, readable only
                // by its lip. Quiet earns its keep over Ground, not over a SoftCard.
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minHeight = Touch.minTarget)
                ) {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge,
                        color = Violet
                    )
                }
                Spacer(Modifier.width(Space.sm))
            }
            ClayButton(
                label = if (isLast) "Start learning" else "Next",
                onClick = onNext,
                tone = ClayButtonTone.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
