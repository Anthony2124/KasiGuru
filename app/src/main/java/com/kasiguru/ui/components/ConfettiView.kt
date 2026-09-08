package com.kasiguru.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.kasiguru.ui.theme.CanopyTop
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.LocalReducedMotion
import kotlin.random.Random

private data class ConfettiParticle(
    val xRatio: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val drift: Float
)

/**
 * A single celebratory burst of confetti — mounted at a win moment and left in place.
 *
 * It runs **once**: particles fall from above the top edge to below the bottom edge over one pass and
 * then the canvas is empty, so a caller that keeps this composed (an overlay that never unmounts, a
 * wizard step the learner lingers on) does not get an endless snow of squares. It was previously an
 * `infiniteRepeatable`, which is how onboarding ended up raining confetti from the warm-up quiz all
 * the way to "Start learning".
 *
 * Honours reduced motion as a contract: with animations removed it renders nothing at all, and nothing
 * downstream may depend on the burst having played.
 */
@Composable
fun ConfettiView(
    modifier: Modifier = Modifier,
    particleCount: Int = 45,
    durationMillis: Int = 1600
) {
    if (LocalReducedMotion.current) return

    val colors = listOf(CanopyTop, Gold, CanopyTop, Coral, Gold, Gold)

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.4f - 0.05f,
                speed = Random.nextFloat() * 0.7f + 0.8f,
                size = Random.nextFloat() * 14f + 8f,
                color = colors[Random.nextInt(colors.size)],
                drift = Random.nextFloat() * 40f - 20f
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis, easing = LinearEasing))
    }

    val p = progress.value
    if (p >= 1f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val fallSpan = height * 1.5f

        particles.forEach { particle ->
            val y = (particle.initialY * height) + p * particle.speed * fallSpan
            if (y in 0f..height) {
                val x = particle.xRatio * width +
                    kotlin.math.sin(p * 6f + particle.xRatio * 6f) * particle.drift
                drawRect(
                    color = particle.color,
                    topLeft = Offset(x, y),
                    size = Size(particle.size, particle.size * 0.6f)
                )
            }
        }
    }
}
