package com.kasiguru.ui.components.clay

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Clay
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.VioletShadow

/**
 * The clay primitive every dimensional object in KasiGuru is built from.
 *
 * Compose has no inset-shadow API, so claymorphism's "double shadow" cannot be faked with a single
 * drop shadow. This composes three real layers instead:
 *
 *  1. **The lip** — a solid shape in [lipColor] sitting [Clay.lip] below the face. This is what makes
 *     a podium block, a badge or the FAB read as a solid object rather than a flat circle.
 *  2. **The lit top** — a white gradient across the upper [Clay.litFraction] of the face, standing in
 *     for the inner highlight that CSS would get from `box-shadow: inset`.
 *  3. **The cast shadow** — carried by the lip, violet-tinted rather than neutral grey, with both
 *     offset and blur.
 *
 * Pressing compresses the object: the lip shrinks toward [Clay.lipPressed] and the face travels down
 * by the same amount, so it squashes against the surface. The full lip height is reserved in the
 * layout at all times, so nothing reflows while it animates.
 *
 * @param face the top surface colour.
 * @param lipColor the base beneath it. Use a clearly darker tone of [face] — a mere alpha shift reads
 *   as a shadow rather than as a side, and the object stops looking solid.
 * @param lit set false for large flat panels where the highlight would band across a wide fill.
 * @param artwork optional illustration slot drawn inside the face, behind [content]. Adrian supplies
 *   this artwork; every clay object must look finished with it absent.
 */
@Composable
fun ClaySurface(
    face: Color,
    lipColor: Color,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = Shapes.tile,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    lit: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(Space.md),
    artwork: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val compressed = isPressed && enabled && onClick != null

    val durationMs = if (compressed) Clay.pressDownMs else Clay.pressUpMs

    val lipHeight by animateDpAsState(
        targetValue = if (compressed) Clay.lipPressed else Clay.lip,
        animationSpec = tween(durationMs),
        label = "ClayLip"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (compressed) Clay.shadowPressed else Clay.shadow,
        animationSpec = tween(durationMs),
        label = "ClayShadow"
    )
    // As the lip compresses the face travels down by the difference, keeping the base planted.
    val faceDrop: Dp = Clay.lip - lipHeight

    Box(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        // ── 1. Lip: the solid base, and the source of the cast shadow ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = lipHeight)
                .shadow(
                    elevation = if (enabled) shadowElevation else 0.dp,
                    shape = shape,
                    ambientColor = VioletShadow,
                    spotColor = VioletShadow
                )
                .clip(shape)
                .background(lipColor)
        )

        // ── 2. Face ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Clay.lip)
                .offset(y = faceDrop)
                .clip(shape)
                .background(face)
                .then(
                    if (lit) Modifier.background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = Clay.litAlpha),
                            Clay.litFraction to Color.Transparent
                        )
                    ) else Modifier
                )
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = interactionSource,
                        // The compression *is* the press feedback; a ripple on top of it reads as
                        // two competing responses to one touch.
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    ) else Modifier
                )
                .padding(contentPadding)
        ) {
            artwork?.invoke(this)
            content()
        }
    }
}

/**
 * Circular clay object: FABs, badge medallions, podium caps.
 *
 * Built from its own square layers rather than by handing [ClaySurface] a [CircleShape]. That shortcut
 * looks right until you try it: the surface is `size` wide but `size + lip` tall, and a 50% rounded
 * corner on a non-square box is an *ellipse*, so the FAB rendered as a squashed blob.
 */
@Composable
fun ClayCircle(
    size: Dp,
    face: Color,
    lipColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val compressed = isPressed && enabled && onClick != null
    val durationMs = if (compressed) Clay.pressDownMs else Clay.pressUpMs

    val lipHeight by animateDpAsState(
        targetValue = if (compressed) Clay.lipPressed else Clay.lip,
        animationSpec = tween(durationMs),
        label = "ClayCircleLip"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (compressed) Clay.shadowPressed else Clay.shadow,
        animationSpec = tween(durationMs),
        label = "ClayCircleShadow"
    )

    Box(
        modifier = modifier
            .size(width = size, height = size + Clay.lip)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        // Lip: a full circle sitting at the bottom of the box.
        Box(
            Modifier
                .size(size)
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = if (enabled) shadowElevation else 0.dp,
                    shape = CircleShape,
                    ambientColor = VioletShadow,
                    spotColor = VioletShadow
                )
                .clip(CircleShape)
                .background(lipColor)
        )
        // Face: the same circle at the top, travelling down as the lip compresses.
        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.TopCenter)
                .offset(y = Clay.lip - lipHeight)
                .clip(CircleShape)
                .background(face)
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = Clay.litAlpha),
                        Clay.litFraction to Color.Transparent
                    )
                )
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
