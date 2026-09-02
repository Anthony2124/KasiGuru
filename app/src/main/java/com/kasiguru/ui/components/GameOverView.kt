package com.kasiguru.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

data class GameReviewItem(
    val prompt: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val subPrompt: String? = null
)

@Composable
fun GameOverView(
    score: Int,
    total: Int,
    xpEarned: Int,
    starsEarned: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    reviewItems: List<GameReviewItem> = emptyList(),
    onNextLevel: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ConfettiView()

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.CupBold),
                        contentDescription = "Complete",
                        tint = GoldDeep,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Challenge Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Score: $score / $total",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isEarned = i <= starsEarned
                        Icon(
                            painter = painterResource(id = Iconsax.StarBold),
                            contentDescription = null,
                            tint = if (isEarned) Gold else Color(0xFFE2E8F0),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Gold.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.FlashBold),
                            contentDescription = null,
                            tint = GoldDeep,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+$xpEarned XP Earned!",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldDeep,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (reviewItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Review Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        reviewItems.forEachIndexed { index, item ->
                            ReviewItemCard(index = index + 1, item = item)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (onNextLevel != null) {
                    ClayButton(
                        label = "Next Level",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNextLevel()
                        },
                        tone = ClayButtonTone.Primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFinish()
                        }
                    ) {
                        Text(
                            text = "Return to Games Hub",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Muted
                        )
                    }
                } else {
                    ClayButton(
                        label = "Return to Games Hub",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFinish()
                        },
                        tone = ClayButtonTone.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(index: Int, item: GameReviewItem) {
    val bg = if (item.isCorrect) GreenTint else RedTint
    val borderColor = if (item.isCorrect) Green.copy(alpha = 0.3f) else Red.copy(alpha = 0.3f)
    val badgeIcon = if (item.isCorrect) Iconsax.TickCircleBold else Iconsax.CloseCircleBold
    val badgeTint = if (item.isCorrect) Green else Red

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = badgeIcon),
                contentDescription = null,
                tint = badgeTint,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$index. ${item.prompt}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                if (!item.subPrompt.isNullOrBlank()) {
                    Text(
                        text = item.subPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (item.isCorrect) {
                    Text(
                        text = "Your answer: ${item.userAnswer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Your answer: ${item.userAnswer.ifBlank { "(No answer)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Red,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Correct answer: ${item.correctAnswer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
