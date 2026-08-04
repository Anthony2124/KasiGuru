package com.kasiguru.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.util.audio.AudioPlayerManager

@Composable
fun WordVerificationDialog(
    targetWord: VocabularyEntity,
    allWords: List<VocabularyEntity>,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }

    // Generate 4 choices (1 correct + 3 distractor choices)
    val options = remember(targetWord, allWords) {
        val correctAnswer = if (targetWord.english.isNotEmpty()) targetWord.english else targetWord.tagalog
        val distractors = allWords
            .filter { it.id != targetWord.id }
            .map { if (it.english.isNotEmpty()) it.english else it.tagalog }
            .distinct()
            .shuffled()
            .take(3)

        (distractors + correctAnswer).shuffled()
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayerManager.stopAudio()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCorrect == true) {
                    ConfettiView()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Title
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HeroCardStart.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "PROVE YOU KNOW IT! 🎯",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Kasiguranin Word Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target Word:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSubtleGray
                                )
                                Text(
                                    text = targetWord.kasiguranin,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TextHeadingBlack
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    audioPlayerManager.playAudio(targetWord.kasiguranin, targetWord.audioFileName)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(VocabCardStart)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.VolumeHigh),
                                    contentDescription = "Listen",
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select the correct translation below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Multiple Choice Options
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEach { option ->
                            val isChoiceSelected = selectedOption == option
                            val isChoiceCorrect = isChoiceSelected && (option == targetWord.english || option == targetWord.tagalog)
                            val isChoiceWrong = isChoiceSelected && !isChoiceCorrect

                            val optionBg = when {
                                isChoiceCorrect -> Success.copy(alpha = 0.25f)
                                isChoiceWrong -> Error.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.background
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = selectedOption == null) {
                                        selectedOption = option
                                        val correct = (option == targetWord.english || option == targetWord.tagalog)
                                        isCorrect = correct

                                        if (correct) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = optionBg,
                                border = if (isChoiceSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, if (isChoiceCorrect) Success else Error)
                                } else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextHeadingBlack
                                    )

                                    if (isChoiceCorrect) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.TickCircle),
                                            contentDescription = "Correct",
                                            tint = Success,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Result Footer
                    if (isCorrect == true) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Success.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 CORRECT! +10 XP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Success
                                )
                                Text(
                                    text = "Word successfully verified and marked as Learned!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextHeadingBlack,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        onSuccess()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (isCorrect == false) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Error.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "❌ INCORRECT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Error
                                )
                                Text(
                                    text = "Keep practicing! Try listening to the audio clip to memorize it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextHeadingBlack,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = TextHeadingBlack),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Try Again Later", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = TextSubtleGray)
                        }
                    }
                }
            }
        }
    }
}
