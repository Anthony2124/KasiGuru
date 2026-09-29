package com.kasiguru.ui.components.clay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kasiguru.ui.theme.GlowCore
import com.kasiguru.ui.theme.GlowMid
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.LocalReducedMotion

/**
 * The glow: a soft radial light behind Jepjep on near-black, measured from Adrian's updated
 * onboarding screen. [GlowCore] at the centre, [GlowMid] half a screen-width out, [Ground] one
 * screen-width out and everywhere beyond.
 *
 * It must read as light, not as a shape: no rings, no hard edge. It belongs on onboarding and on the
 * moments that reward something (lesson complete, level up, badge unlock, streak). Everyday screens
 * stay flat night so the Kasiguranin words stay the loudest thing on them.
 *
 * Drawn, not shipped as a bitmap, so it costs no APK bytes and scales to any screen. If it ever shows
 * banding on a real phone, a 9:16 WebP with light noise is the fallback.
 */
fun DrawScope.drawGlow(center: Offset, radius: Float, scale: Float = 1f) {
    drawRect(Ground)
    drawRect(
        brush = Brush.radialGradient(
            0f to GlowCore,
            0.5f to GlowMid,
            1f to Ground,
            center = center,
            radius = (radius * scale).coerceAtLeast(1f)
        )
    )
}

/**
 * Paints the glow behind this element. [centerX] and [centerY] are fractions of the element's size:
 * put the centre on Jepjep, which is the middle of the screen when he is centred.
 */
fun Modifier.glowBackground(centerX: Float = 0.5f, centerY: Float = 0.45f, scale: Float = 1f): Modifier =
    this.drawBehind {
        drawGlow(
            center = Offset(size.width * centerX, size.height * centerY),
            radius = size.width,
            scale = scale
        )
    }

/**
 * A full-bleed glow backdrop with content on top.
 *
 * @param breathe lets the glow swell very slightly (1.0 to 1.05 over four seconds) on reward
 *   screens. It is static whenever the system asks for reduced motion, and nothing depends on it.
 */
@Composable
fun GlowBackdrop(
    modifier: Modifier = Modifier,
    centerX: Float = 0.5f,
    centerY: Float = 0.45f,
    breathe: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val animate = breathe && !LocalReducedMotion.current
    val scale = if (animate) {
        val transition = rememberInfiniteTransition(label = "GlowBreath")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
            label = "GlowScale"
        ).value
    } else 1f

    Box(
        modifier = modifier
            .background(Ground)
            .glowBackground(centerX, centerY, scale),
        content = content
    )
}
