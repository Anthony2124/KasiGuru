package com.kasiguru.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.SegmentedProgressBar
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Olive
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.RedTint
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.motionTween

/** Text and controls stop growing here, so a tablet reads like a phone column rather than a banner. */
internal val ReadableWidth: Dp = 480.dp

/** Answer pills are taller than the 48 dp minimum: they are the main thing tapped on their steps. */
private val OptionMinHeight: Dp = 52.dp

/**
 * The frame every step after the wake-up shares: the segmented progress bar and Skip at the top,
 * the step's own content in the middle, and a text action plus the one lime button pinned at the
 * bottom, clear of the gesture bar and the keyboard.
 *
 * The middle is the step's business, including scrolling: at 360 dp and a large font size most
 * steps are taller than the space left between the two bars, and the buttons must not move.
 *
 * @param filled segments to fill, counting the current step.
 * @param showSkip hidden steps keep Skip's space, so the progress bar never changes length.
 */
@Composable
internal fun OnboardingFrame(
    filled: Int,
    total: Int,
    showSkip: Boolean,
    onSkip: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // ── Top: progress + Skip ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Space.gutter, end = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedProgressBar(
                total = total,
                filled = filled,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Space.xs))
            FrameTextButton(
                label = "Skip",
                onClick = onSkip,
                visible = showSkip,
                color = Muted
            )
        }

        // ── Middle: the step ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content
        )

        // ── Bottom: Back (or the step's own text action) + the one lime button ──
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = ReadableWidth)
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .padding(top = Space.xs, bottom = Space.sm)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FrameTextButton(label = secondaryLabel, onClick = onSecondary)
            Spacer(Modifier.width(Space.sm))
            ClayButton(
                label = primaryLabel,
                onClick = onPrimary,
                enabled = primaryEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A text-only action with a full 48 dp target: Skip, Back, Not now. When [visible] is false it still
 * takes its space, so what sits beside it does not shift, but it can't be tapped or focused.
 */
@Composable
private fun FrameTextButton(
    label: String,
    onClick: () -> Unit,
    visible: Boolean = true,
    color: Color = Ink
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = Touch.minTarget, minHeight = Touch.minTarget)
            .alpha(if (visible) 1f else 0f)
            .clip(Shapes.pill)
            .then(
                if (visible) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier.clearAndSetSemantics { }
            )
            .padding(horizontal = Space.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Where Jepjep is, so the glow can sit behind him
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Jepjep's centre on each step, in root coordinates. Measured rather than guessed, because where he
 * ends up depends on the screen height, the font size and how far the step is scrolled; the glow
 * backdrop reads the current step's entry and centres itself there.
 */
@Stable
internal class JepjepSpots {
    val centers = mutableStateMapOf<OnboardingStep, Offset>()

    fun report(step: OnboardingStep, center: Offset) {
        val old = centers[step]
        // Sub-pixel jitter from scrolling or a transition is not worth a recomposition.
        if (old == null || (old - center).getDistance() > 1f) centers[step] = center
    }

    /** Attach to the Jepjep on [step]. Transforms applied after it (a jump, a breath) don't move the glow. */
    fun anchor(step: OnboardingStep): Modifier = Modifier.onGloballyPositioned { coordinates ->
        val topLeft = coordinates.positionInRoot()
        report(
            step,
            Offset(
                topLeft.x + coordinates.size.width / 2f,
                topLeft.y + coordinates.size.height / 2f
            )
        )
    }
}

/**
 * What a framed step's content needs from the frame.
 *
 * @param artHeight Jepjep's height on this screen: about a third of the space between the bars, so
 *   the illustration shrinks on short phones instead of pushing the controls off screen.
 * @param viewportHeight the full height of that space.
 * @param jepjepAnchor attach to this step's Jepjep so the glow follows him.
 */
@Immutable
internal class StepContext(
    val artHeight: Dp,
    val viewportHeight: Dp,
    val jepjepAnchor: Modifier
)

// ─────────────────────────────────────────────────────────────────────────────
// Text
// ─────────────────────────────────────────────────────────────────────────────

/** Full width up to [ReadableWidth], inside the page gutter. For every block of text and controls. */
internal fun Modifier.readable(): Modifier =
    this.widthIn(max = ReadableWidth).fillMaxWidth().padding(horizontal = Space.gutter)

/**
 * [text] with [words] in [BrandLime]: the one highlighted word or phrase in each heading. Matches the
 * last occurrence, so a learner named "Nice" still gets their name, not the greeting, highlighted.
 */
internal fun highlighted(text: String, vararg words: String): AnnotatedString = buildAnnotatedString {
    append(text)
    words.forEach { word ->
        if (word.isEmpty()) return@forEach
        val start = text.lastIndexOf(word)
        if (start >= 0) addStyle(SpanStyle(color = BrandLime), start, start + word.length)
    }
}

/** A step heading: Fredoka display, white, one highlighted word, announced as a heading. */
@Composable
internal fun StepTitle(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.displaySmall
) {
    Text(
        text = text,
        style = style,
        color = Ink,
        textAlign = textAlign,
        modifier = modifier.semantics { heading() }
    )
}

/** The line or two under a heading. */
@Composable
internal fun StepBody(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Muted,
        textAlign = textAlign,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Options
// ─────────────────────────────────────────────────────────────────────────────

/** How an answer pill looks. Every state but [Idle] carries an icon, so colour is never the only signal. */
internal enum class PillState { Idle, Selected, Correct, Wrong }

/**
 * A single-choice answer pill, at least 52 dp tall. Selected is olive with a 2 dp lime border and a
 * tick; a wrong answer turns red with a cross.
 */
@Composable
internal fun OptionPill(
    label: String,
    state: PillState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start
) {
    val fill by animateColorAsState(
        targetValue = when (state) {
            PillState.Idle -> Surface
            PillState.Selected, PillState.Correct -> Olive
            PillState.Wrong -> RedTint
        },
        animationSpec = motionTween(Motion.Quick),
        label = "OptionFill"
    )
    val borderColor = when (state) {
        PillState.Idle -> BorderHairline
        PillState.Selected, PillState.Correct -> Lime
        PillState.Wrong -> Red
    }
    val icon: Pair<Int, String?>? = when (state) {
        PillState.Idle -> null
        PillState.Selected -> Iconsax.TickCircle to null
        PillState.Correct -> Iconsax.TickCircle to "Correct"
        PillState.Wrong -> Iconsax.CloseCircle to "Not this one"
    }

    Row(
        modifier = modifier
            .heightIn(min = OptionMinHeight)
            .clip(Shapes.pill)
            .background(fill)
            .border(if (state == PillState.Idle) 1.dp else 2.dp, borderColor, Shapes.pill)
            .selectable(
                selected = state != PillState.Idle,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            textAlign = textAlign,
            modifier = Modifier.weight(1f)
        )
        if (icon != null) {
            Spacer(Modifier.width(Space.xs))
            OptionIcon(
                res = icon.first,
                contentDescription = icon.second,
                tint = if (state == PillState.Wrong) Red else Lime
            )
        }
    }
}

@Composable
internal fun OptionIcon(@DrawableRes res: Int, contentDescription: String?, tint: Color) {
    Icon(
        painter = painterResource(id = res),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Illustration helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fades the bottom of this element to transparent, so an illustration cut off by its box melts into
 * the glow instead of ending on a hard line, as it does in Adrian's designs.
 */
internal fun Modifier.fadeOutBottom(solidUntil: Float = 0.72f): Modifier = this
    // Offscreen so DstIn masks this element's own pixels, not everything already drawn beneath it.
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black,
                solidUntil to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
