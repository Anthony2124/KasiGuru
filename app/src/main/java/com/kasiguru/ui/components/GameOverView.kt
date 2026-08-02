package com.kasiguru.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*

@Composable
fun GameOverView(
    score: Int,
    total: Int,
    xpEarned: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
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
                Text(
                    "+$xpEarned XP Earned!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSubtleGray,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Return to Games Hub", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
