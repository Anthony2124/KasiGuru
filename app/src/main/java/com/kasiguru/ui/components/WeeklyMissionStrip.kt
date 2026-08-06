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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.theme.*

/**
 * 7-day mission calendar strip component matching Inspo Panel 3 ("Mission").
 */
@Composable
fun WeeklyMissionStrip(
    currentStreak: Int,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit = {}
) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val activeDays = (currentStreak % 7).let { if (it == 0 && currentStreak > 0) 7 else it }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(PlayPurpleStart, PlayPurpleEnd)))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly Mission",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                        Text(
                            text = "7-Day Check-in Challenge",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite.copy(alpha = 0.85f)
                        )
                    }

                    Surface(
                        onClick = onCheckInClick,
                        shape = CircleShape,
                        color = PlayGoldStart,
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "✔ Check",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7-day tracker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dayNames.forEachIndexed { index, day ->
                        val dayNumber = index + 1
                        val isChecked = dayNumber <= activeDays

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChecked) PlayGoldStart else TextWhite.copy(alpha = 0.25f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Text(text = "✔", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextHeadingBlack)
                                } else {
                                    Text(text = "$dayNumber", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
