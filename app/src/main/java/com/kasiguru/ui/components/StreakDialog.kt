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
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
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
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flame Icon Circle
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(StreakEmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.FlashBold),
                        contentDescription = "Streak Flame",
                        tint = StreakEmber,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$currentStreak Day Streak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastInk,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Keep up your daily language momentum to build long-term retention!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoastMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
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
                                color = if (dayNumber == todayDayOfWeek) CoastInk else CoastMuted
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActiveStreak -> StreakEmber
                                            dayNumber == todayDayOfWeek -> PlayPurpleStart
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActiveStreak) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Flash),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isPastOrToday) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.TickSquare),
                                        contentDescription = null,
                                        tint = CoastMuted,
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
                    shape = RoundedCornerShape(999.dp),
                    color = XpGold.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.CupBold),
                            contentDescription = null,
                            tint = XpGoldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Longest Streak: $longestStreak Days",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = XpGoldDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ClayButton(
                    label = "Keep it up!",
                    onClick = onDismiss,
                    tone = ClayButtonTone.Reward,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
