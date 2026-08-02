package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = { Text("Mini-Games") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Secondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GameCard(
                    title = "Word Match",
                    description = "Match Kasiguranin words to Tagalog or English.",
                    icon = "🧩",
                    highScore = uiState.highScores["word_match"] ?: 0,
                    gradient = listOf(Secondary, SecondaryContainer),
                    onClick = onNavigateToWordMatch
                )
            }

            item {
                GameCard(
                    title = "Aspect Builder",
                    description = "Conjugate root verbs into correct aspectual forms.",
                    icon = "🌱",
                    highScore = uiState.highScores["aspect_builder"] ?: 0,
                    gradient = listOf(PrimaryLight, PrimaryContainer),
                    onClick = onNavigateToAspectBuilder
                )
            }

            item {
                GameCard(
                    title = "Sentence Order",
                    description = "Arrange words into predicate-initial Kasiguranin syntax.",
                    icon = "🔤",
                    highScore = uiState.highScores["sentence_order"] ?: 0,
                    gradient = listOf(Accent, AccentContainer),
                    onClick = onNavigateToSentenceOrder
                )
            }

            item {
                GameCard(
                    title = "Fill in the Blank",
                    description = "Complete sentences with the correct verb aspect.",
                    icon = "✍️",
                    highScore = uiState.highScores["fill_blank"] ?: 0,
                    gradient = listOf(Primary, PrimaryContainer),
                    onClick = onNavigateToFillBlank
                )
            }

            item {
                GameCard(
                    title = "Audio Quiz",
                    description = "Listen to pronunciation and select the correct word.",
                    icon = "🎧",
                    highScore = uiState.highScores["audio_quiz"] ?: 0,
                    gradient = listOf(SecondaryDark, Secondary),
                    onClick = onNavigateToAudioQuiz
                )
            }

            if (uiState.recentScores.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Scores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
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
    icon: String,
    highScore: Int,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, style = MaterialTheme.typography.displayMedium)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "High Score",
                        tint = XpGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "High Score: $highScore",
                        style = MaterialTheme.typography.labelMedium,
                        color = XpGold
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
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = gradient.first()
                )
            }
        }
    }
}

@Composable
fun ScoreRow(gameName: String, score: Int, total: Int, xp: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = gameName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$score/$total",
                style = MaterialTheme.typography.labelLarge,
                color = Success
            )
            Text(
                text = "+$xp XP",
                style = MaterialTheme.typography.labelMedium,
                color = XpGold
            )
        }
    }
}
