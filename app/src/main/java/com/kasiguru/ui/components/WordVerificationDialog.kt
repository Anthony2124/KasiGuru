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
import androidx.compose.ui.draw.alpha
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
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCorrect == true) {
                    ConfettiView()
                }

                // A visible, discoverable way out — previously only the backdrop tap or system back
                // could cancel this quiz, unlike every other decision dialog in the app.
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.CloseCircle),
                        contentDescription = "Cancel",
                        tint = CoastMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Title
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PlayPurpleStart.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "VERIFICATION QUIZ",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlayPurpleStart,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Kasiguranin Word Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    color = CoastMuted
                                )
                                Text(
                                    text = targetWord.kasiguranin,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CoastInk,
                                    letterSpacing = (-0.3).sp
                                )
                            }

                            AudioPlayButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    audioPlayerManager.playAudio(targetWord.kasiguranin, targetWord.audioFileName)
                                },
                                size = 44.dp,
                                contentDescription = "Listen"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select the correct translation below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoastInk,
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
                                isChoiceCorrect -> Success.copy(alpha = 0.15f)
                                isChoiceWrong -> Error.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
                                    }
                                    // Once answered, every other option visibly steps back instead of
                                    // staying at full strength as if still tappable.
                                    .alpha(if (selectedOption != null && !isChoiceSelected) 0.45f else 1f),
                                shape = RoundedCornerShape(16.dp),
                                color = optionBg,
                                border = if (isChoiceSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, if (isChoiceCorrect) Success else Error)
                                } else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
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
                                        color = CoastInk
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
                            color = Success.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.TickCircle),
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Correct! +100 XP",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Success
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Word successfully verified and marked as Learned!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoastInk,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                CoastPillButton(
                                    label = "Done",
                                    onClick = {
                                        onSuccess()
                                        onDismiss()
                                    },
                                    variant = PillVariant.Purple
                                )
                            }
                        }
                    } else if (isCorrect == false) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Error.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.InfoCircle),
                                        contentDescription = null,
                                        tint = Error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Incorrect! Try again",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Error
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Review this word in Flashcards or Vocabulary to master its meaning.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoastInk,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = onDismiss) {
                                        Text("Cancel", color = CoastMuted, fontWeight = FontWeight.Bold)
                                    }
                                    CoastPillButton(
                                        label = "Try Again",
                                        onClick = {
                                            selectedOption = null
                                            isCorrect = null
                                        },
                                        variant = PillVariant.Gold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
