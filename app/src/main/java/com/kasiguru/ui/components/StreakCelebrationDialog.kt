package com.kasiguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

/**
 * Full celebratory dialog presented immediately when a learner completes their daily streak requirements.
 * Features an animated flame pulse, confetti burst, haptics, and reward highlights.
 */
@Composable
fun StreakCelebrationDialog(
    streakDays: Int,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Flame pulsing & glow animations
    val infiniteTransition = rememberInfiniteTransition(label = "StreakCelebration")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            border = BorderStroke(
                1.5.dp,
                StreakEmber.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ConfettiView()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Tag Badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StreakEmber.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.FlashBold),
                                contentDescription = null,
                                tint = StreakEmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "STREAK ACTIVATED 🔥",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = StreakEmber,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Animated Glowing Flame Circle
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .scale(flameScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Pulsing Glow
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            StreakEmber.copy(alpha = glowAlpha),
                                            PlayGoldStart.copy(alpha = glowAlpha * 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        // Inner Flame Disc
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            StreakEmber,
                                            PlayGoldStart
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.FlashBold),
                                contentDescription = "Flame",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$streakDays Day Streak!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "You completed all daily goals! Your learning momentum is on fire today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoastMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Completed Goals Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GreenTint,
                        border = BorderStroke(1.dp, Green.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.TickCircleBold),
                                    contentDescription = null,
                                    tint = Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Completed Review Words",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CoastInk
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.TickCircleBold),
                                    contentDescription = null,
                                    tint = Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Played 3 Mini-Game Levels",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CoastInk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reward Chip
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Gold.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.FlashBold),
                                contentDescription = null,
                                tint = GoldDeep,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+25 XP Daily Streak Bonus",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldDeep
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ClayButton(
                        label = "Keep the Flame Lit! 🔥",
                        onClick = onDismiss,
                        tone = ClayButtonTone.Reward,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
