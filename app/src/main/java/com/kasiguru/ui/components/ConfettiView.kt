package com.kasiguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.kasiguru.ui.theme.*
import kotlin.random.Random

private data class ConfettiParticle(
    val xRatio: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

@Composable
fun ConfettiView(
    modifier: Modifier = Modifier,
    particleCount: Int = 45
) {
    val colors = listOf(CanopyTop, Gold, CanopyTop, Coral, Gold, Gold)
    
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.5f,
                speed = Random.nextFloat() * 0.8f + 0.5f,
                size = Random.nextFloat() * 14f + 8f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confettiTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            val currentY = ((particle.initialY + progress * particle.speed) % 1.5f) * height
            if (currentY <= height) {
                val currentX = particle.xRatio * width + kotlin.math.sin(progress * 4f) * 20f
                drawRect(
                    color = particle.color,
                    topLeft = Offset(currentX, currentY),
                    size = Size(particle.size, particle.size * 0.6f)
                )
            }
        }
    }
}
