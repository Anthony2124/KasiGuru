package com.kasiguru.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * One rhythm for the whole app.
 *
 * Before this existed, every animation picked its own `tween(200)` or `tween(300)` in place, so
 * transitions that happened next to each other moved at unrelated speeds. Durations here are chosen by
 * how far a thing travels and how much it changes, not by taste at the call site.
 *
 * Exits run shorter than entrances. A screen leaving should feel like it is getting out of the way,
 * and matching the two makes every dismissal feel sluggish.
 */
object Motion {
    /** State flips that do not move: tint, selection, a hairline appearing. */
    const val Quick = 120

    /** The default. Content swaps, expansions, most enter transitions. */
    const val Standard = 240

    /** Whole-screen arrivals and celebratory reveals, where the distance is real. */
    const val Emphasized = 400

    /** Exits at roughly 65% of the matching entrance. */
    fun exit(enterMillis: Int): Int = (enterMillis * 0.65f).toInt()

    /** Arriving: decelerates into place. */
    val EaseOut: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Leaving: accelerates away. */
    val EaseIn: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Stagger between consecutive list items. */
    const val StaggerStep = 35

    /**
     * Cap on staggered items. Past roughly eight the last item's delay is long enough that the list
     * reads as loading rather than arriving, which is the opposite of what the stagger is for.
     */
    const val StaggerMax = 8

    /** Springy press and entrance feel, used wherever something should feel physical. */
    fun <T> springy(): AnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Whether the learner has asked the system to remove animations.
 *
 * Honouring this is not optional. Nothing in the app may depend on an animation having run: every
 * animated value must already be correct at its target when the duration collapses to zero.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Reads the system's animator duration scale. Developer options and the accessibility "Remove
 * animations" setting both write here, and a scale of zero is the platform's way of saying stop.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * A duration that collapses to zero when the learner has removed animations.
 *
 * Use this rather than reading [Motion] constants directly in an `animationSpec`, so reduced motion is
 * handled once here instead of being remembered at every call site.
 */
@Composable
@ReadOnlyComposable
fun motionDuration(millis: Int): Int = if (LocalReducedMotion.current) 0 else millis

/**
 * [tween] already respecting reduced motion.
 *
 * Returns [FiniteAnimationSpec] rather than [AnimationSpec] because the transition APIs that consume
 * it - `Crossfade`, `fadeIn`, `AnimatedVisibility` - require a spec that is known to end.
 */
@Composable
fun <T> motionTween(
    durationMillis: Int = Motion.Standard,
    delayMillis: Int = 0,
    easing: Easing = Motion.EaseOut
): FiniteAnimationSpec<T> = tween(
    durationMillis = motionDuration(durationMillis),
    delayMillis = motionDuration(delayMillis),
    easing = easing
)
