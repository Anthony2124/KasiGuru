package com.kasiguru.ui.components.brand

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.R
import com.kasiguru.ui.theme.LocalReducedMotion

/**
 * Jepjep waking up and waving hello: Adrian's animated `jepjep_splash.svg`, played natively.
 *
 * The SVG's layers live in `res/drawable/jepjep_hello.xml` as named groups with the SVG's own
 * pivots. This replays its CSS keyframes on those groups: a 0.95 s pop-in, then a 2.4 s hop with a
 * leg kick and head tilt, hands waving every 0.6 s, a blink every 4.2 s and twinkling sparkles.
 * Reduced motion shows the resting pose with the sparkles lit, as the SVG does.
 */
@Composable
fun JepjepHello(
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    contentDescription: String? = "Jepjep waving hello"
) {
    val reducedMotion = LocalReducedMotion.current
    val seconds = remember { mutableFloatStateOf(0f) }
    if (!reducedMotion) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) {
                withFrameMillis { now -> seconds.floatValue = (now - start) / 1000f }
            }
        }
    }

    val vector = ImageVector.vectorResource(R.drawable.jepjep_hello)
    val configs = remember(reducedMotion) { helloConfigs(seconds, animate = !reducedMotion) }
    val painter = rememberVectorPainter(
        defaultWidth = vector.defaultWidth,
        defaultHeight = vector.defaultHeight,
        viewportWidth = vector.viewportWidth,
        viewportHeight = vector.viewportHeight,
        name = vector.name,
        tintColor = vector.tintColor,
        tintBlendMode = vector.tintBlendMode,
        autoMirror = vector.autoMirror
    ) { _, _ -> RenderVectorGroup(group = vector.root, configs = configs) }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                alpha = if (reducedMotion) 1f else Enter.opacity.at(seconds.floatValue / 0.95f)
            }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// The SVG's keyframes
// ─────────────────────────────────────────────────────────────────────────────

private val Ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** One CSS keyframe: at [at] (0–1) the value is [value]; [easing] runs to the next keyframe. */
private class Key(val at: Float, val value: Float, val easing: Easing = Ease)

private class Track(vararg val keys: Key) {
    fun at(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        if (p <= keys.first().at) return keys.first().value
        for (i in 0 until keys.size - 1) {
            val a = keys[i]
            val b = keys[i + 1]
            if (p <= b.at) {
                val t = if (b.at == a.at) 1f else (p - a.at) / (b.at - a.at)
                return a.value + (b.value - a.value) * a.easing.transform(t)
            }
        }
        return keys.last().value
    }
}

/** Progress through a looping CSS animation, holding the first frame during its delay. */
private fun loop(seconds: Float, delay: Float, duration: Float): Float =
    if (seconds < delay) 0f else ((seconds - delay) % duration) / duration

/** `animation-direction: alternate`: forwards, then backwards. */
private fun alternate(seconds: Float, delay: Float, duration: Float): Float {
    if (seconds < delay) return 0f
    val cycles = (seconds - delay) / duration
    val within = cycles % 1f
    return if (cycles.toInt() % 2 == 0) within else 1f - within
}

private object Enter {
    private val inOut = CubicBezierEasing(0.2f, 0.7f, 0.35f, 1f)
    private val fall = CubicBezierEasing(0.5f, 0f, 0.8f, 0.5f)
    val opacity = Track(Key(0f, 0f, inOut), Key(0.42f, 1f), Key(1f, 1f))
    val translateY = Track(Key(0f, 140f, inOut), Key(0.42f, -24f, fall), Key(0.62f, 0f, EaseOut), Key(1f, 0f))
    val scaleX = Track(
        Key(0f, 0.45f, inOut), Key(0.42f, 0.97f, fall), Key(0.62f, 1.07f, EaseOut),
        Key(0.80f, 0.98f, EaseInOut), Key(1f, 1f)
    )
    val scaleY = Track(
        Key(0f, 0.45f, inOut), Key(0.42f, 1.04f, fall), Key(0.62f, 0.92f, EaseOut),
        Key(0.80f, 1.025f, EaseInOut), Key(1f, 1f)
    )
}

private object Hop {
    private val rise = CubicBezierEasing(0.3f, 0.6f, 0.4f, 1f)
    private val land = CubicBezierEasing(0.5f, 0f, 0.8f, 0.4f)
    val translateY = Track(
        Key(0f, 0f, EaseOut), Key(0.10f, 0f, rise), Key(0.30f, -40f, land),
        Key(0.44f, 0f, EaseOut), Key(0.54f, 0f, EaseInOut), Key(0.64f, 0f), Key(1f, 0f)
    )
    val scaleX = Track(
        Key(0f, 1f, EaseOut), Key(0.10f, 1.035f, rise), Key(0.30f, 0.985f, land),
        Key(0.44f, 1.04f, EaseOut), Key(0.54f, 0.99f, EaseInOut), Key(0.64f, 1f), Key(1f, 1f)
    )
    val scaleY = Track(
        Key(0f, 1f, EaseOut), Key(0.10f, 0.965f, rise), Key(0.30f, 1.02f, land),
        Key(0.44f, 0.955f, EaseOut), Key(0.54f, 1.012f, EaseInOut), Key(0.64f, 1f), Key(1f, 1f)
    )
    val shadow = Track(Key(0f, 1f), Key(0.10f, 1.04f), Key(0.30f, 0.78f), Key(0.44f, 1.06f), Key(0.64f, 1f), Key(1f, 1f))
    val kick = Track(
        Key(0f, 0f), Key(0.08f, 0f, EaseOut), Key(0.28f, 8f, EaseInOut), Key(0.46f, -1f, EaseInOut),
        Key(0.60f, 0f), Key(1f, 0f)
    )
    val tilt = Track(Key(0f, 0f, EaseInOut), Key(0.30f, -3f, EaseInOut), Key(0.62f, 2.5f, EaseInOut), Key(1f, 0f))
}

private val BlinkTop = Track(Key(0f, -190f), Key(0.90f, -190f), Key(0.94f, 0f), Key(0.95f, 0f), Key(1f, -190f))
private val BlinkBottom = Track(Key(0f, 130f), Key(0.90f, 130f), Key(0.94f, 0f), Key(0.95f, 0f), Key(1f, 130f))
private val TwinkleScale = Track(
    Key(0f, 0f, EaseInOut), Key(0.20f, 1.1f, EaseInOut), Key(0.35f, 0.85f, EaseInOut), Key(0.55f, 0f), Key(1f, 0f)
)
private val TwinkleRotation = Track(
    Key(0f, 0f, EaseInOut), Key(0.20f, 45f, EaseInOut), Key(0.35f, 70f, EaseInOut), Key(0.55f, 0f), Key(1f, 0f)
)
/**
 * One eye's lid shapes from the SVG: a box whose lower edge is a curve from ([left], [edge]) to
 * ([right], [edge]) bowing to [bow], reaching up to [top] and down to [floor].
 */
private class Eye(
    val side: String,
    private val left: Float,
    private val right: Float,
    private val top: Float,
    private val edge: Float,
    private val bow: Float,
    private val floor: Float
) {
    private val mid = (left + right) / 2

    fun top(dy: Float): List<PathNode> = PathBuilder().apply {
        moveTo(left, top + dy); horizontalLineTo(right); verticalLineTo(edge + dy)
        quadTo(mid, bow + dy, left, edge + dy); close()
    }.nodes

    fun line(dy: Float): List<PathNode> = PathBuilder().apply {
        moveTo(left, edge + dy); quadTo(mid, bow + dy, right, edge + dy)
    }.nodes

    fun bottom(dy: Float): List<PathNode> = PathBuilder().apply {
        moveTo(left, edge + dy); quadTo(mid, bow + dy, right, edge + dy)
        verticalLineTo(floor + dy); horizontalLineTo(left); close()
    }.nodes
}

private val Eyes = listOf(
    Eye("L", left = 273.0f, right = 475.4f, top = 159.1f, edge = 294.6f, bow = 320.6f, floor = 405.1f),
    Eye("R", left = 689.0f, right = 887.0f, top = 114.1f, edge = 251.9f, bow = 277.9f, floor = 363.9f)
)

private val SparkDelays = floatArrayOf(1.0f, 1.55f, 2.25f, 1.85f, 2.7f, 2.05f, 3.05f)

/** A group or path whose properties are read from the clock each frame. */
private class Animated(private val values: (Float) -> Map<VectorProperty<*>, Any>, private val clock: State<Float>) :
    VectorConfig {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(property: VectorProperty<T>, defaultValue: T): T =
        (values(clock.value)[property] as T?) ?: defaultValue
}

private fun helloConfigs(clock: State<Float>, animate: Boolean): Map<String, VectorConfig> {
    if (!animate) return emptyMap()
    val configs = mutableMapOf<String, VectorConfig>()
    fun group(name: String, values: (Float) -> Map<VectorProperty<*>, Any>) {
        configs[name] = Animated(values, clock)
    }
    group("frog") { s ->
        val p = s / 0.95f
        mapOf(
            VectorProperty.TranslateY to Enter.translateY.at(p),
            VectorProperty.ScaleX to Enter.scaleX.at(p),
            VectorProperty.ScaleY to Enter.scaleY.at(p)
        )
    }
    group("hop") { s ->
        val p = loop(s, 0.95f, 2.4f)
        mapOf(
            VectorProperty.TranslateY to Hop.translateY.at(p),
            VectorProperty.ScaleX to Hop.scaleX.at(p),
            VectorProperty.ScaleY to Hop.scaleY.at(p)
        )
    }
    group("shadow") { s ->
        val scale = Hop.shadow.at(loop(s, 0.95f, 2.4f))
        mapOf(VectorProperty.ScaleX to scale, VectorProperty.ScaleY to scale)
    }
    group("legL") { s -> mapOf(VectorProperty.Rotation to Hop.kick.at(loop(s, 0.95f, 2.4f))) }
    group("head") { s -> mapOf(VectorProperty.Rotation to Hop.tilt.at(loop(s, 1.0f, 2.4f))) }
    group("handL") { s ->
        mapOf(VectorProperty.Rotation to 8f + (-11f - 8f) * EaseInOut.transform(alternate(s, 0.7f, 0.6f)))
    }
    group("handR") { s ->
        mapOf(VectorProperty.Rotation to -8f + (11f + 8f) * EaseInOut.transform(alternate(s, 0.7f, 0.6f)))
    }
    // The lids slide inside the eye's clip. Compose's vector parser can't nest a moving group in a
    // clip group, so the lid paths themselves are redrawn at their new height.
    for (eye in Eyes) {
        group("lidTop${eye.side}") { s -> mapOf(VectorProperty.PathData to eye.top(BlinkTop.at(loop(s, 1.6f, 4.2f)))) }
        group("lidTopLine${eye.side}") { s ->
            mapOf(VectorProperty.PathData to eye.line(BlinkTop.at(loop(s, 1.6f, 4.2f))))
        }
        group("lidBot${eye.side}") { s ->
            mapOf(VectorProperty.PathData to eye.bottom(BlinkBottom.at(loop(s, 1.6f, 4.2f))))
        }
    }
    SparkDelays.forEachIndexed { i, delay ->
        group("spark$i") { s ->
            val p = loop(s, delay, 2.6f)
            val scale = TwinkleScale.at(p)
            mapOf(
                VectorProperty.ScaleX to scale,
                VectorProperty.ScaleY to scale,
                VectorProperty.Rotation to TwinkleRotation.at(p)
            )
        }
    }
    return configs
}
