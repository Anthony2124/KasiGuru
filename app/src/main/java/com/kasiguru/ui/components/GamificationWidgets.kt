package com.kasiguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

/**
 * Animated XP badge with a pulsing glow effect.
 */
@Composable
fun XpBadge(
    xp: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = false
) {
    val scale by if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "xpPulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "xpScale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = XpGoldDark.copy(alpha = 0.2f),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = Iconsax.StarBold),
                contentDescription = null,
                tint = XpGold,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$xp XP",
                style = MaterialTheme.typography.labelLarge,
                color = XpGold
            )
        }
    }
}

/**
 * Animated streak counter with a fire emoji.
 */
@Composable
fun StreakCounter(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val isActive = streak > 0

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) StreakOrange.copy(alpha = 0.15f) else DarkSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isActive) "🔥" else "❄️",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "$streak day${if (streak != 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) StreakOrange else TextGray
            )
        }
    }
}

/**
 * Badge display with lock/unlock state and optional scale animation.
 */
@Composable
fun AnimatedBadge(
    emoji: String,
    name: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    animateUnlock: Boolean = false
) {
    val scale by if (animateUnlock && isUnlocked) {
        val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "badgeScale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Column(
        modifier = modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = if (isUnlocked) {
                        Brush.radialGradient(
                            colors = listOf(
                                SecondaryContainer.copy(alpha = 0.5f),
                                SecondaryDark.copy(alpha = 0.2f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                DarkSurfaceVariant,
                                DarkSurface
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isUnlocked) emoji else "🔒",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isUnlocked) TextWhite else TextGray
        )
    }
}

/**
 * Translucent rounded pill stat chip (Play theme design system).
 */
@Composable
fun PlayStatChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    backgroundColor: androidx.compose.ui.graphics.Color = PlayChipTranslucent,
    contentColor: androidx.compose.ui.graphics.Color = TextDark
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

