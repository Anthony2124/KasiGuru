package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.SectionCaption
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.Stat
import com.kasiguru.ui.components.clay.StatStrip
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLocked
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.util.gamification.GamificationEngine

/**
 * Practice: the mini-game hub. A violet canopy carrying level, XP and stars, over a sheet listing the
 * six drill modes and, once played, recent activity.
 *
 * Replaces a screen built entirely from ad hoc `Surface` gradients (`PlayPurpleStart`, `XpGold`, the
 * "DICTIONARY CORPUS"-style eyebrow chips' sibling patterns) with the shared canopy/sheet spine so
 * Practice reads as the same app as Learn rather than a screen from a different build.
 */
@Composable
fun GameHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLevelSelection: (String) -> Unit,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedGameRulesType by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val totalXp = uiState.userProgress?.totalXp ?: 0
    val levelInfo = remember(totalXp) { GamificationEngine.getLevelInfo(totalXp) }

    // remember's calculation runs outside composition, so themed colours must be resolved here first.
    val violet = Violet
    val games = remember(uiState.totalStars, uiState.highScores, violet) {
        listOf(
            GameEntry("word_match", "Word Match", Iconsax.Element4Outline, violet,
                com.kasiguru.util.Constants.GameUnlockStars.WORD_MATCH, uiState.highScores["word_match"] ?: 0),
            GameEntry("reverse_match", "Reverse Match", Iconsax.RepeatOutline, Coral,
                com.kasiguru.util.Constants.GameUnlockStars.REVERSE_MATCH, uiState.highScores["reverse_match"] ?: 0),
            GameEntry("fill_blank", "Fill in Blank", Iconsax.Edit, Gold,
                com.kasiguru.util.Constants.GameUnlockStars.FILL_BLANK, uiState.highScores["fill_blank"] ?: 0),
            GameEntry("sentence_order", "Sentence Order", Iconsax.Document, violet,
                com.kasiguru.util.Constants.GameUnlockStars.SENTENCE_ORDER, uiState.highScores["sentence_order"] ?: 0),
            GameEntry("audio_quiz", "Audio Quiz", Iconsax.VolumeHigh, Coral,
                com.kasiguru.util.Constants.GameUnlockStars.AUDIO_QUIZ, uiState.highScores["audio_quiz"] ?: 0),
            GameEntry("aspect_builder", "Aspect Builder", Iconsax.Flash, Gold,
                com.kasiguru.util.Constants.GameUnlockStars.ASPECT_BUILDER, uiState.highScores["aspect_builder"] ?: 0)
        )
    }
    // The single best next move: the unlocked game played least successfully, or — if everything is
    // still locked — whichever is closest to unlocking. Gives the screen one lead item instead of
    // asking the grid alone to double as both catalog and recommendation.
    val recommended = remember(games) {
        games.filter { uiState.totalStars >= it.unlockStars }.minByOrNull { it.highScore }
            ?: games.minByOrNull { it.unlockStars }
    }

    // Skips straight to the level select once a player has opted out of re-seeing a game's rules,
    // instead of the dialog reappearing in full on every single entry.
    val onGameTap: (String) -> Unit = { gameType ->
        if (gameType in uiState.seenGameRules) {
            onNavigateToLevelSelection(gameType)
        } else {
            selectedGameRulesType = gameType
        }
    }

    selectedGameRulesType?.let { gameType ->
        GameRulesDialog(
            gameType = gameType,
            totalStars = uiState.totalStars,
            onStartGame = { dontShowAgain ->
                if (dontShowAgain) viewModel.markRulesSeen(gameType)
                onNavigateToLevelSelection(gameType)
                selectedGameRulesType = null
            },
            onDismiss = { selectedGameRulesType = null }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    CanopyScaffold(
        canopyHeight = 174.dp,
        canopyContent = {
            Spacer(Modifier.height(Space.sm))
            Text(text = "Practice", style = MaterialTheme.typography.headlineMedium, color = OnCanopy)
            Text(
                text = "Level ${levelInfo.level} · ${levelInfo.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnCanopy
            )
            Spacer(Modifier.height(Space.lg))
            StatStrip(
                stats = listOf(
                    Stat("XP", "$totalXp"),
                    Stat("Stars", "${uiState.totalStars}"),
                    Stat("Accuracy", "${(uiState.accuracyRate * 100).toInt()}%")
                ),
                onCanopy = true
            )
        },
        sheetContent = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                )
            ) {
                recommended?.let { game ->
                    item {
                        SectionHeading(text = "Continue practicing")
                        Spacer(Modifier.height(Space.sm))
                        FeaturedGameCard(
                            entry = game,
                            isUnlocked = uiState.totalStars >= game.unlockStars,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGameTap(game.gameType)
                            }
                        )
                        Spacer(Modifier.height(Space.lg))
                    }
                }

                item {
                    SectionHeading(text = "All mini-games")
                    SectionCaption(text = "Earn stars to unlock the rest")
                    Spacer(Modifier.height(Space.md))
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        games.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                                pair.forEach { game ->
                                    GameTile(
                                        entry = game,
                                        isUnlocked = uiState.totalStars >= game.unlockStars,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onGameTap(game.gameType)
                                        }
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (uiState.recentScores.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(Space.lg))
                        SectionHeading(text = "Recent activity")
                        Spacer(Modifier.height(Space.sm))
                    }
                    items(uiState.recentScores) { score ->
                        GameScoreRow(score = score)
                        Spacer(Modifier.height(Space.xs))
                    }
                }
            }
        }
    )
}

/**
 * The one lead item above the catalog grid. A page built entirely from same-size icon-title-subtitle
 * cards is the exact "cards as page structure" failure the craft floor bans; this gives Practice one
 * differentiated recommendation to act on before the grid takes over as reference.
 */
@Composable
private fun FeaturedGameCard(entry: GameEntry, isUnlocked: Boolean, onClick: () -> Unit) {
    val isRewardFill = isUnlocked && (entry.accent == Gold || entry.accent == Coral)
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.panel, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(Shapes.chip)
                    .background(
                        when {
                            !isUnlocked -> NodeLocked
                            isRewardFill -> entry.accent
                            else -> entry.accent.copy(alpha = 0.16f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (isUnlocked) entry.iconRes else Iconsax.Lock),
                    contentDescription = null,
                    tint = when {
                        !isUnlocked -> NodeLockedInk
                        isRewardFill -> RewardInk
                        else -> entry.accent
                    },
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = when {
                        !isUnlocked -> "${entry.unlockStars} stars to unlock"
                        entry.highScore > 0 -> "Best: ${entry.highScore} · beat your record"
                        else -> "Not played yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            Icon(
                painter = painterResource(id = Iconsax.ArrowRight),
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class GameEntry(
    val gameType: String,
    val title: String,
    val iconRes: Int,
    val accent: Color,
    val unlockStars: Int,
    val highScore: Int
)

@Composable
private fun GameTile(
    entry: GameEntry,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Gold and Coral measure below even the 3:1 non-text floor as an icon tint on their own light
    // tint background (1.65 / 2.03, measured) — same "fills carry ink" rule as reward surfaces
    // elsewhere, so those two accents get a solid fill with an ink icon instead.
    val isRewardFill = isUnlocked && (entry.accent == Gold || entry.accent == Coral)
    SoftCard(
        modifier = modifier,
        shape = Shapes.tile,
        onClick = onClick,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(Shapes.chip)
                .background(
                    when {
                        !isUnlocked -> NodeLocked
                        isRewardFill -> entry.accent
                        else -> entry.accent.copy(alpha = 0.16f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = if (isUnlocked) entry.iconRes else Iconsax.Lock),
                contentDescription = null,
                tint = when {
                    !isUnlocked -> NodeLockedInk
                    isRewardFill -> RewardInk
                    else -> entry.accent
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isUnlocked) Ink else NodeLockedInk,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (isUnlocked) "Best: ${entry.highScore}" else "${entry.unlockStars} stars to unlock",
            style = MaterialTheme.typography.labelSmall,
            color = Muted
        )
    }
}

@Composable
private fun GameScoreRow(score: GameScoreEntity) {
    val gameName = when (score.gameType) {
        "word_match" -> "Word Match"
        "reverse_match" -> "Reverse Match"
        "fill_blank" -> "Fill in the Blank"
        "audio_quiz" -> "Audio Quiz"
        "aspect_builder" -> "Aspect Builder"
        "sentence_order" -> "Sentence Construction"
        else -> score.gameType
    }
    val accuracy = if (score.totalQuestions > 0) (score.score.toFloat() / score.totalQuestions * 100).toInt() else 0

    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, elevation = 2.dp, contentPadding = PaddingValues(Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Green.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.TickCircle),
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(text = gameName, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = "${score.score}/${score.totalQuestions} correct · +${score.xpEarned} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint
                )
            }
            Text(text = "$accuracy%", style = MaterialTheme.typography.titleMedium, color = Muted)
        }
    }
}
