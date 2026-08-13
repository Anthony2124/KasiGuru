package com.kasiguru.ui.screens.games

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

data class GameRuleInfo(
    val gameType: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val unlockStars: Int,
    val rules: List<String>,
    val gradient: List<Color>
)

object GameRulesRegistry {
    val games = mapOf(
        "word_match" to GameRuleInfo(
            gameType = "word_match",
            title = "Word Match Blitz",
            description = "Test your vocabulary speed by matching Kasiguranin terms to English or Tagalog.",
            iconRes = Iconsax.Element4Outline,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.WORD_MATCH,
            rules = listOf(
                "Match 5 Kasiguranin words to their correct translation.",
                "Consecutive correct matches build a Combo Multiplier (x2, x3 XP!).",
                "Earn up to +100 XP per round."
            ),
            gradient = listOf(VocabCardStart, VocabCardEnd)
        ),
        "fill_blank" to GameRuleInfo(
            gameType = "fill_blank",
            title = "Fill in the Blank",
            description = "Complete authentic Kasiguranin sentences with the missing verb or noun.",
            iconRes = Iconsax.Edit,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.FILL_BLANK,
            rules = listOf(
                "Read the sentence context carefully.",
                "Select the correct inflected word from 4 choices.",
                "Earn +20 XP for each correct sentence completion."
            ),
            gradient = listOf(QuestsCardStart, QuestsCardEnd)
        ),
        "audio_quiz" to GameRuleInfo(
            gameType = "audio_quiz",
            title = "Audio Listening Quiz",
            description = "Listen to authentic Kasiguranin pronunciation and identify the spoken word.",
            iconRes = Iconsax.VolumeHigh,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.AUDIO_QUIZ,
            rules = listOf(
                "Tap the speaker button to hear native Kasiguranin audio.",
                "Select the matching word from 4 choices.",
                "Combos boost your XP payout by up to 3x!"
            ),
            gradient = listOf(MiniGamesCardStart, MiniGamesCardEnd)
        ),
        "aspect_builder" to GameRuleInfo(
            gameType = "aspect_builder",
            title = "Verb Aspect Builder",
            description = "Conjugate Kasiguranin root verbs into Neutral, Imperfective, Perfective, or Contemplative aspects.",
            iconRes = Iconsax.Flash,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.ASPECT_BUILDER,
            rules = listOf(
                "Given a root verb and target aspect (e.g. Past/Perfective).",
                "Assemble or select the correct verb inflection.",
                "Master authentic Northern Luzon verb morphology!"
            ),
            gradient = listOf(HeroCardStart, HeroCardEnd)
        ),
        "sentence_order" to GameRuleInfo(
            gameType = "sentence_order",
            title = "Sentence Construction",
            description = "Arrange scrambled words into natural Kasiguranin predicate-initial syntax.",
            iconRes = Iconsax.Document,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.SENTENCE_ORDER,
            rules = listOf(
                "Kasiguranin uses Predicate-Initial word order.",
                "Tap word blocks in order to construct authentic sentences.",
                "Earn +150 XP for perfect syntax construction!"
            ),
            gradient = listOf(StoriesCardStart, StoriesCardEnd)
        ),
        "reverse_match" to GameRuleInfo(
            gameType = "reverse_match",
            title = "Reverse Match",
            description = "See the Tagalog meaning and recall the Kasiguranin word — the reverse of Word Match.",
            iconRes = Iconsax.RepeatOutline,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.REVERSE_MATCH,
            rules = listOf(
                "Read the Tagalog (or English) meaning.",
                "Pick the matching Kasiguranin word from 4 choices.",
                "Recall is harder than recognition — answer fast for bonus XP!"
            ),
            gradient = listOf(PlayPurpleStart, PlayPurpleEnd)
        )
    )
}

@Composable
fun GameRulesDialog(
    gameType: String,
    totalStars: Int,
    onStartGame: () -> Unit,
    onDismiss: () -> Unit
) {
    val ruleInfo = GameRulesRegistry.games[gameType] ?: return
    val isUnlocked = totalStars >= ruleInfo.unlockStars
    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(ruleInfo.gradient))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = ruleInfo.iconRes),
                                contentDescription = null,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = ruleInfo.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = if (isUnlocked) "UNLOCKED (Req. ${ruleInfo.unlockStars} ⭐)" else "LOCKED (Req. ${ruleInfo.unlockStars} ⭐)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHeadingBlack
                                )
                            }
                        }
                    }
                }

                // Content & Rules
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = ruleInfo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHeadingBlack
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "HOW TO PLAY & RULES:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )

                    ruleInfo.rules.forEach { rule ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("•", fontWeight = FontWeight.Bold, color = HeroCardStart)
                            Text(
                                text = rule,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextHeadingBlack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isUnlocked) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                                onStartGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Start Game!", color = TextHeadingBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔒 Earn ${ruleInfo.unlockStars} Total Stars to unlock this mini-game!",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
