package com.kasiguru.ui.screens.lesson

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.Violet

/**
 * The moment the lesson pays off.
 *
 * The app previously had no completion state at all — a finished game returned you to a hub. XP
 * counts up rather than appearing, because watching a number climb is the reward; a static total is
 * just information.
 *
 * @param artwork optional celebration illustration. Adrian supplies this; the screen is composed so
 *   that its absence reads as restraint rather than as a hole.
 */
@Composable
fun LessonCompleteScreen(
    xpAwarded: Int,
    accuracy: Float,
    words: List<VocabularyEntity>,
    onContinue: () -> Unit,
    artwork: (@Composable () -> Unit)? = null
) {
    val isPerfect = accuracy >= 1f

    // Hold at zero for a beat so the count-up is seen starting, not caught mid-flight.
    var countStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { countStarted = true }
    val countedXp by animateIntAsState(
        targetValue = if (countStarted) xpAwarded else 0,
        animationSpec = tween(durationMillis = 900, delayMillis = 250, easing = FastOutSlowInEasing),
        label = "XpCount"
    )

    Box(Modifier.fillMaxSize().background(Ground)) {
        ConfettiView()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Space.xxl))

            if (artwork != null) {
                artwork()
                Spacer(Modifier.height(Space.lg))
            } else {
                ClayCircle(size = 96.dp, face = Gold, lipColor = GoldDeep) {
                    Icon(
                        painter = painterResource(id = Iconsax.CupBold),
                        contentDescription = null,
                        tint = RewardInk,
                        modifier = Modifier.size(46.dp).align(Alignment.Center)
                    )
                }
                Spacer(Modifier.height(Space.lg))
            }

            Text(
                text = if (isPerfect) "Perfect lesson" else "Lesson complete",
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = if (isPerfect) {
                    "Every answer right on the first try."
                } else {
                    "${(accuracy * 100).toInt()}% correct on your first attempt."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Space.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                RewardTile(
                    value = "+$countedXp",
                    label = "XP earned",
                    iconRes = Iconsax.Flash,
                    modifier = Modifier.weight(1f)
                )
                RewardTile(
                    value = "${words.size}",
                    label = if (words.size == 1) "word covered" else "words covered",
                    iconRes = Iconsax.Book,
                    modifier = Modifier.weight(1f)
                )
            }

            if (words.isNotEmpty()) {
                Spacer(Modifier.height(Space.lg))
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "In this lesson",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.xs))
                    words.forEach { word ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = word.kasiguranin,
                                style = MaterialTheme.typography.titleMedium,
                                color = Violet
                            )
                            Spacer(Modifier.width(Space.xs))
                            Text(
                                text = word.tagalog.ifBlank { word.english },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.xxl))
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface)
                .navigationBarsPadding()
                .padding(Space.gutter)
        ) {
            ClayButton(
                label = "Continue",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RewardTile(
    value: String,
    label: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(Shapes.chip)
                    .background(Green.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(Space.xs))
            Column {
                Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Ink)
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
    }
}
