package com.kasiguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*

/**
 * One review card in three states: the closed cover (side 0), the word (1) and the meaning (2).
 *
 * The instruction lives on the card itself, at its foot, and changes with the side: "Tap to open" on
 * the cover, "Tap to flip" on the word, "Tap to see the word" on the meaning. The old hint sat below
 * the card in a script face and described both steps at once, so it had to be read before it could
 * be followed. The cover swings open on a left hinge; word and meaning turn over around the vertical
 * axis like a real card. With reduced motion both become instant cuts.
 *
 * The paper keeps dark ink in both application themes.
 */
@Composable
fun FlashCard(
    word: VocabularyEntity,
    number: Int,
    side: Int,
    onFlip: () -> Unit,
    onAudio: () -> Unit,
    modifier: Modifier = Modifier,
    total: Int = 0
) {
    val reduced = LocalReducedMotion.current
    val coverAngle by animateFloatAsState(
        if (side == 0) 0f else -110f,
        tween(if (reduced) 0 else 450), label = "book cover"
    )
    // 0 = word facing up, 180 = meaning facing up.
    val turn by animateFloatAsState(
        if (side == 2) 180f else 0f,
        tween(if (reduced) 0 else 380, easing = FastOutSlowInEasing), label = "card turn"
    )
    val shape = RoundedCornerShape(28.dp)
    val tapLabel = when (side) {
        0 -> "Open the card"
        1 -> "Flip to the meaning"
        else -> "Flip back to the word"
    }
    Box(modifier) {
        // The next card, fanned out behind: a closed cover, dimmer than the one in hand.
        Box(
            Modifier.matchParentSize().graphicsLayer { rotationZ = 6f; translationY = 6.dp.toPx() }
                .clip(shape).background(OliveDeep).border(2.dp, Lime.copy(alpha = .35f), shape)
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = turn
                    cameraDistance = 24f * density
                }
                .clip(shape)
                .background(Cream)
                .border(3.dp, Lime, shape)
                // The flip has its own sound.
                .noTapSound()
                .clickable(onClickLabel = tapLabel, role = Role.Button, onClick = onFlip)
        ) {
            val showingAnswer = turn > 90f
            // The back face is drawn mirrored so it reads the right way round once turned.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = if (showingAnswer) 180f else 0f }) {
                PaperFace(
                    word = word,
                    number = number,
                    total = total,
                    answer = showingAnswer,
                    onAudio = onAudio
                )
            }

            if (side == 0 || (!reduced && coverAngle > -109f)) {
                Cover(
                    word = word,
                    number = number,
                    total = total,
                    shape = shape,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        rotationY = coverAngle
                        transformOrigin = TransformOrigin(0f, .5f)
                        cameraDistance = 24f * density
                    }
                )
            }
        }
    }
}

/** The closed cover: the category, which card this is, and what to do with it. */
@Composable
private fun Cover(word: VocabularyEntity, number: Int, total: Int, shape: RoundedCornerShape, modifier: Modifier) {
    Box(modifier.clip(shape).background(Olive)) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(
                Lime,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * .55f, -size.height * .08f),
                size = androidx.compose.ui.geometry.Size(size.width * .8f, size.height * .45f)
            )
            drawOval(
                Lime.copy(alpha = .28f),
                topLeft = androidx.compose.ui.geometry.Offset(-size.width * .4f, size.height * .72f),
                size = androidx.compose.ui.geometry.Size(size.width * 1.2f, size.height * .5f)
            )
        }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (total > 0) "Card $number of $total" else "Card $number",
                style = MaterialTheme.typography.labelLarge,
                color = Cream,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painterResource(categoryDoodle(word.category)), null,
                tint = Cream, modifier = Modifier.size(84.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                word.category,
                style = MaterialTheme.typography.titleLarge,
                color = Cream,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            TapPill(text = "Tap to open", fill = Cream, ink = RewardInk)
        }
    }
}

/** The paper inside: the word on one side, its meaning on the other. */
@Composable
private fun PaperFace(
    word: VocabularyEntity,
    number: Int,
    total: Int,
    answer: Boolean,
    onAudio: () -> Unit
) {
    val ink = RewardInk
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        // ── Top: the category and the card number ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ink.copy(alpha = .08f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(categoryDoodle(word.category)), null,
                    tint = ink, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    word.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = number.toString().padStart(2, '0') +
                    if (total > 0) " / " + total.toString().padStart(2, '0') else "",
                style = MaterialTheme.typography.labelMedium,
                color = ink.copy(alpha = .7f)
            )
        }

        // ── Middle: the word, or its meaning ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (answer) {
                Text(
                    "MEANING",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = .7f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    word.meaningEnglish.ifBlank { word.english },
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                    textAlign = TextAlign.Center
                )
                if (word.tagalog.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.width(48.dp).height(2.dp).background(ink.copy(alpha = .15f)))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "TAGALOG",
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.copy(alpha = .7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        word.tagalog,
                        style = MaterialTheme.typography.titleMedium,
                        color = ink,
                        textAlign = TextAlign.Center
                    )
                }
                if (word.exampleSentence.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ink.copy(alpha = .06f))
                            .padding(12.dp)
                    ) {
                        Text(word.exampleSentence, style = MaterialTheme.typography.bodyMedium, color = ink)
                        if (word.exampleTranslation.isNotBlank()) {
                            Text(
                                word.exampleTranslation,
                                style = MaterialTheme.typography.bodySmall,
                                color = ink.copy(alpha = .75f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    word.kasiguranin,
                    style = MaterialTheme.typography.displaySmall,
                    color = ink,
                    textAlign = TextAlign.Center
                )
                if (word.ipaNotation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("[${word.ipaNotation}]", style = MaterialTheme.typography.bodyLarge, color = ink.copy(alpha = .75f))
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ink)
                        .clickable(onClickLabel = "Listen to the word", role = Role.Button, onClick = onAudio),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(Iconsax.VolumeHigh), "Listen to the word", tint = Cream, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── Foot: what a tap does now ──
        TapPill(
            text = if (answer) "Tap to see the word" else "Tap to flip",
            fill = ink.copy(alpha = .08f),
            ink = ink,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/** The card's own instruction: a small pill at its foot with a tap mark. Decorative to TalkBack; the card's click label says it. */
@Composable
private fun TapPill(text: String, fill: Color, ink: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A fingertip: a dot inside a ring.
        Canvas(Modifier.size(14.dp)) {
            drawCircle(ink, radius = size.minDimension * .18f)
            drawCircle(ink, radius = size.minDimension * .45f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}
