package com.kasiguru.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.kasiguru.data.repository.DailyStreakQuota
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import java.time.LocalDate

@Composable
fun StreakDialog(
    currentStreak: Int,
    longestStreak: Int,
    streakQuota: DailyStreakQuota = DailyStreakQuota(),
    onDismiss: () -> Unit
) {
    val todayDayOfWeek = LocalDate.now().dayOfWeek.value // 1 = Mon, 7 = Sun
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flame Icon Circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.FlashBold),
                        contentDescription = "Streak Flame",
                        tint = Coral,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "$currentStreak Day Streak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (streakQuota.isQuotaMet) "Your streak is active for today! Keep up the momentum!" else "Complete your daily goals below to activate today's streak!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Daily Streak Goals Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (streakQuota.isQuotaMet) GreenTint else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (streakQuota.isQuotaMet) Green.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Requirements",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (streakQuota.isQuotaMet) Green.copy(alpha = 0.15f) else Gold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (streakQuota.isQuotaMet) "Active Today 🔥" else "In Progress",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (streakQuota.isQuotaMet) Green else GoldDeep,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Requirement 1: Review Words
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (streakQuota.reviewCompleted) Green.copy(alpha = 0.15f) else Muted.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (streakQuota.reviewCompleted) Iconsax.TickCircleBold else Iconsax.Refresh),
                                        contentDescription = null,
                                        tint = if (streakQuota.reviewCompleted) Green else Muted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Complete Review Words",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    )
                                    Text(
                                        text = if (streakQuota.reviewCompleted) "Daily review completed" else "Finish flashcard review deck",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Muted
                                    )
                                }
                            }
                            Text(
                                text = if (streakQuota.reviewCompleted) "Done" else "Pending",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (streakQuota.reviewCompleted) Green else Muted
                            )
                        }

                        // Requirement 2: Mini Games
                        val gamesClamped = streakQuota.gamesPlayed.coerceAtMost(streakQuota.requiredGames)
                        val isGamesDone = streakQuota.gamesPlayed >= streakQuota.requiredGames
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isGamesDone) Green.copy(alpha = 0.15f) else CanopyTop.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (isGamesDone) Iconsax.TickCircleBold else Iconsax.PlayCircle),
                                        contentDescription = null,
                                        tint = if (isGamesDone) Green else CanopyTop,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Play 3 Mini Game Levels",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    )
                                    Text(
                                        text = if (isGamesDone) "3 of 3 levels played" else "$gamesClamped of ${streakQuota.requiredGames} played today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Muted
                                    )
                                }
                            }
                            Text(
                                text = "$gamesClamped/${streakQuota.requiredGames}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGamesDone) Green else CanopyTop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                                color = if (dayNumber == todayDayOfWeek) Ink else Muted
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActiveStreak -> Coral
                                            dayNumber == todayDayOfWeek -> CanopyTop
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
                                        modifier = Modifier.size(17.dp)
                                    )
                                } else if (isPastOrToday) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.TickSquare),
                                        contentDescription = null,
                                        tint = Muted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Longest Streak Info Pill
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
                            painter = painterResource(id = Iconsax.CupBold),
                            contentDescription = null,
                            tint = GoldDeep,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Longest Streak: $longestStreak Days",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldDeep
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
