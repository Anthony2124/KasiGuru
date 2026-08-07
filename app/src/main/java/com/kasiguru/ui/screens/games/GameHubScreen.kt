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
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
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
                CircularProgressIndicator(color = MiniGamesCardEnd)
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(levelInfo.iconEmoji, fontSize = 32.sp)
                                    Column {
                                        Text(
                                            text = "Level ${levelInfo.level}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = levelInfo.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextWhite.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = TextWhite.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "$totalXp XP",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            KasiGuruProgressBar(
                                progress = xpProgress,
                                height = 6.dp,
                                gradientColors = listOf(Color.White, Color.White.copy(alpha = 0.7f)),
                                animated = true
                            )
                        }
                    }
                }
            }

            // ─── 2. Games Section Title ───
            item {
                Text(
                    text = "Select a Mini-Game",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
            }

            // ─── 2-Column Game Tile Grid ───
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
                            gradient = listOf(PlayGoldStart, PlayGoldEnd),
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
                            gradient = listOf(PlayGoldStart, PlayGoldEnd),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "aspect_builder"
                            }
                        )
                    }

                    // Row 3: Sentence Construction
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
                            gradient = listOf(PlayPinkStart, PlayPinkEnd),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGameRulesType = "sentence_order"
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // ─── 3. Recent Scores Activity ───
            if (uiState.recentScores.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.recentScores) { score ->
                    ScoreRow(
                        gameName = when (score.gameType) {
                            "word_match" -> "Word Match Blitz"
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
private fun GameCardWithLock(
    title: String,
    description: String,
    iconRes: Int,
    highScore: Int,
    minLevelReq: Int,
    userLevel: Int,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    val isUnlocked = userLevel >= minLevelReq

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isUnlocked) Brush.linearGradient(gradient)
                    else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFCCCCCC)))
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    if (!isUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "Lv. $minLevelReq",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHeadingBlack.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.Cup),
                            contentDescription = "High Score",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Best Score: $highScore",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHeadingBlack
                        )
                    }
                } else {
                    Text(
                        text = "🔒 Unlocks at Level $minLevelReq",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack.copy(alpha = 0.7f)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (isUnlocked) Iconsax.Play else Iconsax.Lock),
                    contentDescription = if (isUnlocked) "Play" else "Locked",
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(20.dp)
                )
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
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(gradient))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) Color.White.copy(alpha = 0.35f) else PlayGoldStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isUnlocked) iconRes else Iconsax.Lock),
                        contentDescription = null,
                        tint = TextHeadingBlack,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isUnlocked) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "Best: $highScore",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = PlayGoldStart
                    ) {
                        Text(
                            text = "$unlockStars ⭐",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
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
                    text = gameName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
                Text(
                    text = "${score.score}/${score.totalQuestions} Correct • +${score.xpEarned} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtleGray
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = QuestsCardStart
            ) {
                Text(
                    text = "${(score.score.toFloat() / score.totalQuestions * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
            }
        }
    }
}
