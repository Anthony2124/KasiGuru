package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.CoralDeep
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RedTint
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet

/**
 * [GameRulesRegistry] is a plain data object built once at class-load time, outside composition, so
 * its Violet gradients can't reference the theme-reactive tokens — same reasoning as
 * `CategoryMetaData.kt`'s `FixedViolet`. Kept local rather than shared since each file's registry is
 * itself the single place that should ever reference it.
 */
private val FixedViolet = Color(0xFF5B4CDB)
private val FixedVioletDeep = Color(0xFF4034A8)

data class GameRuleInfo(
    val gameType: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val unlockStars: Int,
    val rules: List<String>,
    val gradient: List<Color>,
    /** Ink on light gradients (Gold, Coral); white on the deep Violet gradient. See DESIGN.md. */
    val onGradientIsInk: Boolean = false
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
            gradient = listOf(FixedViolet, FixedVioletDeep)
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
            gradient = listOf(Gold, GoldDeep),
            onGradientIsInk = true
        ),
        com.kasiguru.util.Constants.Games.RECALL to GameRuleInfo(
            gameType = com.kasiguru.util.Constants.Games.RECALL,
            title = "Word Recall",
            description = "The meaning is given. Write the Kasiguranin word from memory.",
            iconRes = Iconsax.Keyboard,
            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.RECALL,
            rules = listOf(
                "No choices to pick from — you type the word yourself.",
                "Type e in place of ə, and skip the accents. Neither is marked wrong.",
                "A small typo still counts, and the correct spelling is shown afterwards."
            ),
            gradient = listOf(Coral, CoralDeep),
            onGradientIsInk = true
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
            gradient = listOf(Gold, GoldDeep),
            onGradientIsInk = true
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
            gradient = listOf(FixedViolet, FixedVioletDeep)
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
            gradient = listOf(Coral, CoralDeep),
            onGradientIsInk = true
        )
    )
}

@Composable
fun GameRulesDialog(
    gameType: String,
    totalStars: Int,
    onStartGame: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val ruleInfo = GameRulesRegistry.games[gameType] ?: return
    val isUnlocked = totalStars >= ruleInfo.unlockStars
    val haptic = LocalHapticFeedback.current
    val onGradient = if (ruleInfo.onGradientIsInk) RewardInk else Color.White
    var dontShowAgain by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        SoftCard(shape = Shapes.panel, contentPadding = PaddingValues(0.dp)) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(ruleInfo.gradient))
                        .padding(Space.lg)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(onGradient.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = ruleInfo.iconRes),
                                    contentDescription = null,
                                    tint = onGradient,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(Space.sm))
                            Text(
                                text = ruleInfo.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = onGradient
                            )
                        }
                        Spacer(Modifier.height(Space.sm))
                        GlassChip(tint = onGradient) {
                            Icon(
                                painter = painterResource(
                                    id = if (isUnlocked) Iconsax.TickCircle else Iconsax.Lock
                                ),
                                contentDescription = null,
                                tint = onGradient,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isUnlocked) "Unlocked" else "Needs ${ruleInfo.unlockStars} stars",
                                style = MaterialTheme.typography.labelMedium,
                                color = onGradient
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    Text(text = ruleInfo.description, style = MaterialTheme.typography.bodyMedium, color = Muted)

                    Text(
                        text = "How to play",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink
                    )
                    ruleInfo.rules.forEach { rule ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Violet)
                            )
                            Text(text = rule, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }

                    Spacer(Modifier.height(Space.xs))

                    if (isUnlocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { dontShowAgain = !dontShowAgain }
                        ) {
                            Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                            Text(
                                text = "Don't show these rules again",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                        Spacer(Modifier.height(Space.xs))
                        ClayButton(
                            label = "Start game",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                                onStartGame(dontShowAgain)
                            },
                            tone = ClayButtonTone.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Space.xs))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Not now", color = Muted)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.tile)
                                .background(RedTint)
                                .padding(Space.sm)
                        ) {
                            Text(
                                text = "Earn ${ruleInfo.unlockStars} total stars to unlock this mini-game.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Ink
                            )
                        }
                    }
                }
            }
        }
    }
}
