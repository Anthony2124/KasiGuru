package com.kasiguru.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun GameOverView(
    score: Int,
    total: Int,
    xpEarned: Int,
    starsEarned: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ConfettiView()

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Cup),
                    contentDescription = "Complete",
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Game Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextHeadingBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Score: $score / $total",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextHeadingBlack,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..3) {
                        val isEarned = i <= starsEarned
                        Icon(
                            painter = painterResource(id = Iconsax.StarBold),
                            contentDescription = null,
                            tint = if (isEarned) PlayGoldStart else Color(0xFFE0E0E0),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "+$xpEarned XP Earned!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSubtleGray,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Return to Games Hub", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
