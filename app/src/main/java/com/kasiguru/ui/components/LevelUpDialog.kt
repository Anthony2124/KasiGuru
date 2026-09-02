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
import com.kasiguru.util.gamification.LevelInfo

@Composable
fun LevelUpDialog(
    newLevelInfo: LevelInfo,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

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

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CanopyTop.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MascotOwlSlot(size = 54.dp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Level ${newLevelInfo.level}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        letterSpacing = (-0.3).sp
                    )

                    Text(
                        text = newLevelInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CanopyTop
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "You unlocked new mini-game modes & badges!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    ClayButton(
                        label = "Awesome!",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        tone = ClayButtonTone.Primary
                    )
                }
            }
        }
    }
}
