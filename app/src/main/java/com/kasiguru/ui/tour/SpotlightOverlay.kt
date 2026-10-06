package com.kasiguru.ui.tour

import com.kasiguru.ui.theme.LimeText
import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.kasiguru.ui.theme.LocalReducedMotion
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.GestureHint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.motionTween
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** How opaque the dim is. Measured, not guessed: the app behind stays legible as context. */
private const val ScrimAlpha = 0.72f

/** The dim while a new screen arrives: light enough to see where the tour has gone. */
private const val ArrivingScrimAlpha = 0.4f

/*
 * One rhythm for every stop on the screen already showing:
 *
 *   caption fades out -> hole travels -> caption fades in, StepBeatMs after Next was tapped.
 *
 * Before, a stop on the same screen showed its words 200ms after the tap and one on another screen
 * 700ms after the destination arrived; the hole flew on a spring, so a long jump finished faster
 * than a short one; and the old caption vanished in a single frame while the new one rushed in over
 * 180ms. Each stop felt quick, and no two stops felt alike.
 *
 * A stop behind a tab switch takes ArrivalBeatMs instead. On StepBeatMs its caption landed the
 * moment the 700ms crossfade ended, so the core chapter's four tab stops (Learn, Practice, Library,
 * Me) covered each new screen before the learner had seen it, and read as rushed.
 */

/** The old caption leaving. Short: it is only clearing the stage. */
private const val CaptionOutMs = 200

/** The hole's travel, fixed so every move takes the same time however far it goes. */
private const val HoleMoveMs = 450

/** The new caption arriving. */
private const val CaptionInMs = 300

/** From Next to the new caption, for a stop on the same screen: the caption leaving plus the hole's travel. */
internal const val StepBeatMs = 750L

/**
 * From Next to the new caption, for a stop on another screen: navigation-compose's default 700ms
 * crossfade, then about half a second with the dim lifted, so the learner sees where the tour has
 * gone before the words cover it.
 */
internal const val ArrivalBeatMs = 1_250L

/** How long a stop holds its caption back after Next. */
internal fun stepBeatMs(newScreen: Boolean): Long = if (newScreen) ArrivalBeatMs else StepBeatMs

/** Longest wait for a destination or its anchor. A missing anchor must never strand the caption. */
private const val ArrivalTimeoutMs = 1_500L

/**
 * Whether a stop needs a new screen to arrive before it can be explained.
 *
 * @param alreadyThere whether the stop's destination is the one showing as the stop begins. A
 *   chapter's first stop is usually somewhere else entirely - Settings, the dictionary - and has no
 *   previous stop to compare with.
 */
internal fun opensNewScreen(previous: TourTarget?, next: TourTarget, alreadyThere: Boolean): Boolean =
    !alreadyThere || (previous != null && previous != next)

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
    // What is drawn lags what was asked for by one caption-fade, so the old words leave in place
    // instead of being swapped for the new ones mid-fade (and the counter does not jump early).
    var shownStop by remember { mutableStateOf(stop) }
    var shownIndex by remember { mutableIntStateOf(stepIndex) }
    val isLast = shownIndex == stepCount - 1

    // Back is handled here rather than left to fall through. At stop 1 the back stack holds only
    // Home - onboarding was popped inclusively on the way in - so falling through would pop it and
    // drop the learner at the launcher with this overlay still drawn over the app.
    BackHandler(enabled = true) { onBack() }

    val padPx = with(density) { shownStop.pad.toPx() }
    val cornerPx = with(density) { shownStop.corner.toPx() }
    val gapPx = with(density) { Space.md.toPx() }
    val safeTopPx = WindowInsets.statusBars.getTop(density) + gapPx
    val bottomBlockedPx = with(density) { bottomBlocked.toPx() }
    val navBarPx = WindowInsets.navigationBars.getBottom(density)

    // A stop with no anchor explains a screen rather than a control: dim, no hole, caption centred.
    val hole = shownStop.anchor
        ?.takeIf { anchorVisible }
        ?.let { anchors.boundsOf(it) }
        ?.inflate(padPx)

    // Keep the last known rectangle so a move between two measured anchors animates rather than
    // cutting. While no anchor is measured nothing is drawn, so a stale rect can never be painted
    // over a screen it does not belong to.
    var lastHole by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(hole) { if (hole != null) lastHole = hole }

    val animatedHoleState = animateValueAsState(
        targetValue = hole ?: lastHole ?: Rect.Zero,
        typeConverter = Rect.VectorConverter,
        animationSpec = if (LocalReducedMotion.current) snap() else tween(HoleMoveMs, easing = FastOutSlowInEasing),
        label = "tourHole"
    )
    val animatedHole by animatedHoleState
    val drawnHole = if (hole != null) animatedHole else null
    val holeTarget by rememberUpdatedState(hole)

    val reduced = LocalReducedMotion.current
    // Starts hidden: the first stop waits for its anchor like every other, rather than drawing one
    // frame of caption before the effect below has run.
    val captionArrival = remember { Animatable(0f) }
    // 0 is the full dim; 1 is lifted to ArrivingScrimAlpha while a new screen arrives.
    val scrimLift = remember { Animatable(0f) }
    // The caption's buttons only answer once it is showing, so a double tap cannot skip a stop.
    var captionReady by remember { mutableStateOf(false) }
    var shownTarget by remember { mutableStateOf<TourTarget?>(null) }
    val destinationShowing by rememberUpdatedState(anchorVisible)
    LaunchedEffect(stop) {
        val startedAt = SystemClock.uptimeMillis()
        val newScreen = opensNewScreen(shownTarget, stop.target, alreadyThere = destinationShowing)
        shownTarget = stop.target
        captionReady = false
        if (newScreen && !reduced) launch { scrimLift.animateTo(1f, tween(Motion.Standard, easing = Motion.EaseOut)) }

        // The old caption leaves where it stands, then the hole is pointed at the new target.
        if (reduced || captionArrival.value == 0f) captionArrival.snapTo(0f)
        else captionArrival.animateTo(0f, tween(CaptionOutMs, easing = Motion.EaseIn))
        shownStop = stop
        shownIndex = stepIndex

        // The caption describes what is behind it, so that has to be there first: the destination,
        // and the anchor measured - which also keeps the card from appearing centred and then
        // jumping beside a hole that lands a frame later.
        withTimeoutOrNull(ArrivalTimeoutMs) {
            snapshotFlow { destinationShowing && (stop.anchor == null || anchors.boundsOf(stop.anchor) != null) }
                .first { it }
        }
        if (!reduced) {
            // ...and the hole has finished travelling, including after a reveal scroll moved it.
            withTimeoutOrNull(HoleMoveMs + 300L) {
                snapshotFlow { holeTarget.let { it == null || it.isNear(animatedHoleState.value) } }.first { it }
            }
            val elapsed = SystemClock.uptimeMillis() - startedAt
            val beat = stepBeatMs(newScreen)
            if (elapsed < beat) delay(beat - elapsed)
        }

        captionReady = true
        if (reduced) {
            scrimLift.snapTo(0f)
            captionArrival.snapTo(1f)
        } else {
            coroutineScope {
                launch { scrimLift.animateTo(0f, tween(CaptionInMs, easing = Motion.EaseOut)) }
                captionArrival.animateTo(1f, tween(CaptionInMs, easing = Motion.EaseOut))
            }
        }
    }

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
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            // Read here rather than in composition, so the lift animates as redraws alone.
            val scrimAlpha = ScrimAlpha + (ArrivingScrimAlpha - ScrimAlpha) * scrimLift.value
            drawRect(color = Scrim.copy(alpha = scrimAlpha))
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
            stop = shownStop,
            chapterTitle = chapterTitle,
            stepIndex = shownIndex,
            stepCount = stepCount,
            isLast = isLast,
            onBack = { if (captionReady) onBack() },
            onSkip = { if (captionReady) onSkip() },
            onNext = { if (captionReady) onNext() },
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
                    val usable = (safeBottomPx - safeTopPx).coerceAtLeast(0f)
                    val maxH = (if (room < 140.dp.toPx()) usable else room).coerceIn(0f, usable).roundToInt()

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
                .graphicsLayer { alpha = captionArrival.value; translationY = (1f - captionArrival.value) * 12.dp.toPx() }
        )
    }
}

/** Within a pixel on every edge: the hole has landed. */
private fun Rect.isNear(other: Rect): Boolean =
    abs(left - other.left) < 1f && abs(top - other.top) < 1f &&
        abs(right - other.right) < 1f && abs(bottom - other.bottom) < 1f

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
                    color = Muted,
                    modifier = Modifier.weight(1f)
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
                            color = LimeText
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.sm))

            // Jepjep beside the words, so the title and its explanation read as one thing he says.
            Row(verticalAlignment = Alignment.Top) {
                Jepjep(JepjepPose.Peeking, height = 64.dp)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
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
                    stop.gesture?.let {
                        GestureHint(swipe = it == TourGesture.SWIPE, modifier = Modifier.padding(top = Space.sm))
                    }
                }
            }
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
                        color = LimeText
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
