package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun StreakDialog(
    currentStreak: Int,
    longestStreak: Int,
    onDismiss: () -> Unit
) {
    val todayDayOfWeek = LocalDate.now().dayOfWeek.value // 1 = Mon, 7 = Sun
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flame Icon Circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(VocabCardStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.Flash),
                        contentDescription = "Streak Flame",
                        tint = TextHeadingBlack,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$currentStreak Day Streak! 🔥",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextHeadingBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You're on fire! Practice every day to build your Kasiguranin habit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtleGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Weekly 7-Day Tracker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    days.forEachIndexed { index, dayName ->
                        val dayNumber = index + 1
                        val isPastOrToday = dayNumber <= todayDayOfWeek
                        val isActiveStreak = isPastOrToday && (todayDayOfWeek - dayNumber < currentStreak)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayNumber == todayDayOfWeek) TextHeadingBlack else TextSubtleGray
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActiveStreak -> VocabCardStart
                                            dayNumber == todayDayOfWeek -> HeroCardStart
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActiveStreak) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Flash),
                                        contentDescription = null,
                                        tint = TextHeadingBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isPastOrToday) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.TickSquare),
                                        contentDescription = null,
                                        tint = TextSubtleGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Longest Streak Info Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StoriesCardStart.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "🏆 Personal Best: $longestStreak Days",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Keep It Up!",
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
