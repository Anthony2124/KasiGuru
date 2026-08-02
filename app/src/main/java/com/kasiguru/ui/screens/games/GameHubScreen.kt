package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.R
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWordMatch: () -> Unit,
    onNavigateToFillBlank: () -> Unit,
    onNavigateToAudioQuiz: () -> Unit,
    onNavigateToAspectBuilder: () -> Unit = {},
    onNavigateToSentenceOrder: () -> Unit = {},
    viewModel: GamesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
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
                            painter = painterResource(id = R.drawable.ic_arrow_left),
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
        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GameCard(
                    title = "Word Match",
                    description = "Match Kasiguranin words to Tagalog or English.",
                    iconRes = R.drawable.ic_grid_outline,
                    highScore = uiState.highScores["word_match"] ?: 0,
                    gradient = listOf(VocabCardStart, VocabCardEnd),
                    onClick = onNavigateToWordMatch
                )
            }

            item {
                GameCard(
                    title = "Aspect Builder",
                    description = "Conjugate root verbs into correct aspectual forms.",
                    iconRes = R.drawable.ic_flash_outline,
                    highScore = uiState.highScores["aspect_builder"] ?: 0,
                    gradient = listOf(HeroCardStart, HeroCardEnd),
                    onClick = onNavigateToAspectBuilder
                )
            }

            item {
                GameCard(
                    title = "Sentence Order",
                    description = "Arrange words into predicate-initial Kasiguranin syntax.",
                    iconRes = R.drawable.ic_document_outline,
                    highScore = uiState.highScores["sentence_order"] ?: 0,
                    gradient = listOf(StoriesCardStart, StoriesCardEnd),
                    onClick = onNavigateToSentenceOrder
                )
            }

            item {
                GameCard(
                    title = "Fill in the Blank",
                    description = "Complete sentences with the correct verb aspect.",
                    iconRes = R.drawable.ic_edit_outline,
                    highScore = uiState.highScores["fill_blank"] ?: 0,
                    gradient = listOf(QuestsCardStart, QuestsCardEnd),
                    onClick = onNavigateToFillBlank
                )
            }

            item {
                GameCard(
                    title = "Audio Quiz",
                    description = "Listen to pronunciation and select the correct word.",
                    iconRes = R.drawable.ic_volume_high,
                    highScore = uiState.highScores["audio_quiz"] ?: 0,
                    gradient = listOf(MiniGamesCardStart, MiniGamesCardEnd),
                    onClick = onNavigateToAudioQuiz
                )
            }

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
                            "word_match" -> "Word Match"
                            "aspect_builder" -> "Aspect Builder"
                            "sentence_order" -> "Sentence Order"
                            "fill_blank" -> "Fill in the Blank"
                            "audio_quiz" -> "Audio Quiz"
                            else -> score.gameType
                        },
                        score = score.score,
                        total = score.totalQuestions,
                        xp = score.xpEarned
                    )
                }
            }
        }
    }
}

@Composable
fun GameCard(
    title: String,
    description: String,
    iconRes: Int,
    highScore: Int,
    gradient: List<Color>,
    onClick: () -> Unit
) {
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
                .background(Brush.linearGradient(gradient))
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextHeadingBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHeadingBlack.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cup_outline),
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
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_outline),
                    contentDescription = "Play",
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ScoreRow(gameName: String, score: Int, total: Int, xp: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gameName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHeadingBlack
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$score/$total",
                    style = MaterialTheme.typography.labelLarge,
                    color = Success,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+$xp XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextHeadingBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
