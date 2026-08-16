package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.MascotOwlSlot
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.util.gamification.GamificationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLevelSelection: (String) -> Unit,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedGameRulesType by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val userLevel = uiState.userProgress?.level ?: 1
    val totalXp = uiState.userProgress?.totalXp ?: 0
    val levelInfo = remember(totalXp) { GamificationEngine.getLevelInfo(totalXp) }
    val xpProgress = remember(totalXp) { GamificationEngine.getXpProgressInLevel(totalXp) }

    // Selected Rules Dialog
    selectedGameRulesType?.let { gameType ->
        GameRulesDialog(
            gameType = gameType,
            totalStars = uiState.totalStars,
            onStartGame = {
                onNavigateToLevelSelection(gameType)
                selectedGameRulesType = null
            },
            onDismiss = { selectedGameRulesType = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Mini-Games Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowLeft),
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PlayPurpleStart)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── 1. Level Status Card ───
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MascotOwlSlot(size = 36.dp)
                                    }

                                    Column {
                                        Text(
                                            text = "Level ${levelInfo.level}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextWhite,
                                            letterSpacing = (-0.2).sp
                                        )
                                        Text(
                                            text = levelInfo.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextWhite.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.FlashBold),
                                            contentDescription = null,
                                            tint = XpGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$totalXp XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextWhite
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            KasiGuruProgressBar(
                                progress = xpProgress,
                                height = 6.dp,
                                gradientColors = listOf(XpGold, Color.White),
                                animated = true
                            )
                        }
                    }
                }
            }

            // ─── 2. Games Section Title ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select a Mini-Game",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.2).sp
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PlayPurpleStart.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.StarBold),
                                contentDescription = null,
                                tint = XpGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${uiState.totalStars} Stars",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PlayPurpleStart
                            )
                        }
                    }
                }
            }

            // ─── 2-Column Game Tile Grid (Casiguran Coast Palette) ───
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Word Match & Fill in the Blank
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameTileCardWithLock(
                            modifier = Modifier.weight(1f),
                            title = "Word Match",
                            iconRes = Iconsax.Element4Outline,
                            highScore = uiState.highScores["word_match"] ?: 0,
                            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.WORD_MATCH,
                            totalStars = uiState.totalStars,
                            gradient = listOf(GamesCoralLight, GamesCoral),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "word_match"
                            }
                        )
                        GameTileCardWithLock(
                            modifier = Modifier.weight(1f),
                            title = "Fill in Blank",
                            iconRes = Iconsax.Edit,
                            highScore = uiState.highScores["fill_blank"] ?: 0,
                            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.FILL_BLANK,
                            totalStars = uiState.totalStars,
                            gradient = listOf(PlayPinkStart, PlayPinkEnd),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "fill_blank"
                            }
                        )
                    }

                    // Row 2: Audio Quiz & Aspect Builder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameTileCardWithLock(
                            modifier = Modifier.weight(1f),
                            title = "Audio Quiz",
                            iconRes = Iconsax.VolumeHigh,
                            highScore = uiState.highScores["audio_quiz"] ?: 0,
                            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.AUDIO_QUIZ,
                            totalStars = uiState.totalStars,
                            gradient = listOf(PlayPurpleStart, PlayPurpleEnd),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "audio_quiz"
                            }
                        )
                        GameTileCardWithLock(
                            modifier = Modifier.weight(1f),
                            title = "Aspect Builder",
                            iconRes = Iconsax.Flash,
                            highScore = uiState.highScores["aspect_builder"] ?: 0,
                            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.ASPECT_BUILDER,
                            totalStars = uiState.totalStars,
                            gradient = listOf(XpGold, XpGoldDark),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "aspect_builder"
                            }
                        )
                    }

                    // Row 3: Sentence Construction & Reverse Match
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameTileCardWithLock(
                            modifier = Modifier.weight(1f),
                            title = "Sentence Order",
                            iconRes = Iconsax.Document,
                            highScore = uiState.highScores["sentence_order"] ?: 0,
                            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.SENTENCE_ORDER,
                            totalStars = uiState.totalStars,
                            gradient = listOf(StoriesDusk, PlayPurpleStart),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "sentence_order"
                            }
                        )
                        GameTileCardWithLock(
                            modifier = Modifier.weight(1f),
                            title = "Reverse Match",
                            iconRes = Iconsax.RepeatOutline,
                            highScore = uiState.highScores["reverse_match"] ?: 0,
                            unlockStars = com.kasiguru.util.Constants.GameUnlockStars.REVERSE_MATCH,
                            totalStars = uiState.totalStars,
                            gradient = listOf(VocabSea, VocabSeaDark),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "reverse_match"
                            }
                        )
                    }
                }
            }

            // ─── 3. Recent Scores Activity ───
            if (uiState.recentScores.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        modifier = Modifier.padding(top = 8.dp),
                        letterSpacing = (-0.2).sp
                    )
                }

                items(uiState.recentScores) { score ->
                    ScoreRow(
                        gameName = when (score.gameType) {
                            "word_match" -> "Word Match Blitz"
                            "reverse_match" -> "Reverse Match"
                            "fill_blank" -> "Fill in the Blank"
                            "audio_quiz" -> "Audio Quiz"
                            "aspect_builder" -> "Aspect Builder"
                            "sentence_order" -> "Sentence Construction"
                            else -> score.gameType
                        },
                        score = score
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTileCardWithLock(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int,
    highScore: Int,
    unlockStars: Int,
    totalStars: Int,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    val isUnlocked = totalStars >= unlockStars

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isUnlocked) Brush.linearGradient(gradient)
                    else Brush.linearGradient(listOf(Color(0xFFE2E6EE), Color(0xFFCDD3DE)))
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isUnlocked) iconRes else Iconsax.Lock),
                        contentDescription = null,
                        tint = if (isUnlocked) TextWhite else CoastMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnlocked) TextWhite else CoastInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    letterSpacing = (-0.2).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.28f)
                    ) {
                        Text(
                            text = "Best: $highScore",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PlayPurpleStart.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.StarBold),
                                contentDescription = null,
                                tint = XpGoldDark,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "$unlockStars Stars",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoastInk,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(
    gameName: String,
    score: GameScoreEntity
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastInk,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = "${score.score}/${score.totalQuestions} Correct • +${score.xpEarned} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoastMuted,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = XpGold.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${(score.score.toFloat() / score.totalQuestions * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = XpGoldDark
                )
            }
        }
    }
}
