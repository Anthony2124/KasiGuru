package com.kasiguru.ui.components.brand

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.R
import com.kasiguru.ui.theme.LocalReducedMotion

/**
 * Jepjep, KasiGuru's guide: a green frog in a cream barong, drawn by Adrian.
 *
 * Each pose is exported from Adrian's artwork as a WebP with a transparent background (the white
 * page keyed out from the edges inwards, so the near-white barong is untouched), 720 px on the long
 * side, in `res/drawable-nodpi`. Pick the pose by what the moment means, not by what looks nice:
 * the same pose should always mean the same thing.
 *
 * Never more than one Jepjep on screen.
 */
enum class JepjepPose(@DrawableRes val res: Int, val description: String) {
    /** Default hello. Welcome, a morning greeting on Home, Help. */
    Waving(R.drawable.jepjep_waving, "Jepjep waving"),
    /** Onboarding wake-up, streak lost, an empty inbox at night. */
    Sleeping(R.drawable.jepjep_sleeping, "Jepjep asleep"),
    /** The main reward pose: lesson complete, first word, level up, badge unlock. */
    Celebrating(R.drawable.jepjep_celebrating, "Jepjep celebrating"),
    /** Journey: the name step, the current node on the path, starting a new section. */
    WithBackpack(R.drawable.jepjep_with_backpack, "Jepjep with a backpack"),
    /** Coach marks and "something new" hints. Has a door edge on his left side. */
    Peeking(R.drawable.jepjep_peeking, "Jepjep peeking out"),
    /** Stories. */
    Reading(R.drawable.jepjep_reading, "Jepjep reading a book"),
    /** Listening exercises and audio tips. */
    Listening(R.drawable.jepjep_listening, "Jepjep listening"),
    /** Practice and games. */
    PlayingAGame(R.drawable.jepjep_playing_a_game, "Jepjep playing a game"),
    /** The teacher: first word, lesson intros, tips inside lessons. */
    Pointing(R.drawable.jepjep_pointing_a_lesson, "Jepjep pointing at a lesson"),
    /** Correct answers, a kept streak, an approved contribution. Thumbs up. */
    Encouraging(R.drawable.jepjep_encouraging_approve, "Jepjep giving a thumbs up"),
    /** Hints and recall. */
    Thinking(R.drawable.jepjep_thinking, "Jepjep thinking"),
    /** Word of the day, a search with no results. */
    Curious(R.drawable.jepjep_curious, "Jepjep looking curious"),
    /** A wrong answer, gently. Kinder than [Sad]; use it for mistakes. */
    Confused(R.drawable.jepjep_confused, "Jepjep looking puzzled"),
    /** Streak broken, leaving a lesson. Use sparingly. */
    Sad(R.drawable.jepjep_sad, "Jepjep looking sad"),
    /** Offline, errors, guest progress at risk. */
    Worried(R.drawable.jepjep_worried, "Jepjep looking worried"),
    /** Big wins: a perfect score, an unexpected level up. */
    Shocked(R.drawable.jepjep_shocked, "Jepjep amazed"),
    /** Calm moments: reminders, the Settings header, quiet empty states. Sits on a leaf. */
    Sitting(R.drawable.jepjep_sitting, "Jepjep sitting on a leaf"),
    /** Home's speech bubble, pronunciation tips. The drawn bubble is empty: put text beside it. */
    Speaking(R.drawable.jepjep_speaking, "Jepjep speaking")
}

/**
 * Draws Jepjep at a fixed [height]; the width follows the pose's own proportions.
 *
 * @param contentDescription null when the pose is decoration next to text that already says what
 *   is happening, which is almost always. Pass [JepjepPose.description] when he is the message.
 * @param breathe a 2% idle swell over three seconds, for Home and empty states. Off whenever the
 *   system asks for reduced motion.
 */
@Composable
fun Jepjep(
    pose: JepjepPose,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    contentDescription: String? = null,
    breathe: Boolean = false
) {
    val animate = breathe && !LocalReducedMotion.current
    val scale = if (animate) {
        rememberInfiniteTransition(label = "JepjepBreath").animateFloat(
            initialValue = 1f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "JepjepScale"
        ).value
    } else 1f

    Image(
        painter = painterResource(id = pose.res),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
    )
}
