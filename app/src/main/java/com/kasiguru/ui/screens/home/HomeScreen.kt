package com.kasiguru.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.KasiGuruCard
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.StreakCounter
import com.kasiguru.ui.components.XpBadge
import com.kasiguru.ui.theme.*
import com.kasiguru.util.calculateLevelProgress
import com.kasiguru.util.getLevelTitle
import com.kasiguru.util.toXpString

@Composable
fun HomeScreen(
    onNavigateToStories: () -> Unit,
    onNavigateToVocabulary: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToCultural: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val progress = uiState.userProgress ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Magandang aldaw, ${progress.userName}! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Let's learn Kasiguranin today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextWhite)
                }
                StreakCounter(streak = progress.currentStreak)
            }
        }

        // Level & XP Card
        KasiGuruCard(
            gradientColors = listOf(PrimaryDark, DarkCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level ${progress.level}: ${getLevelTitle(progress.level)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total XP: ${progress.totalXp.toXpString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
                XpBadge(xp = progress.totalXp, animate = true)
            }
            Spacer(modifier = Modifier.height(16.dp))
            KasiGuruProgressBar(
                progress = calculateLevelProgress(progress.totalXp),
                showLabel = true,
                gradientColors = listOf(Secondary, SecondaryLight),
                backgroundColor = DarkSurfaceVariant
            )
        }

        // Quick Daily Review Banner
        KasiGuruCard(
            gradientColors = listOf(SecondaryDark, DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToFlashcards() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚡ Daily Review Deck",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "10 flashcards ready for spaced repetition recall",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                Button(
                    onClick = onNavigateToFlashcards,
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Review")
                }
            }
        }

        // Action Grid
        Text(
            text = "Your Learning Journey",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                title = "Vocabulary",
                subtitle = "${progress.wordsLearned} learned",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                gradient = listOf(Primary, PrimaryContainer),
                onClick = onNavigateToVocabulary,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Stories",
                subtitle = "${progress.storiesCompleted} read",
                icon = Icons.Filled.AutoStories,
                gradient = listOf(Accent, AccentContainer),
                onClick = onNavigateToStories,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                title = "Mini-Games",
                subtitle = "${progress.gamesPlayed} played",
                icon = Icons.Filled.Gamepad,
                gradient = listOf(Secondary, SecondaryContainer),
                onClick = onNavigateToGames,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Achievements",
                subtitle = "View awards",
                icon = Icons.Filled.EmojiEvents,
                gradient = listOf(Warning, DarkSurface),
                onClick = onNavigateToAchievements,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                title = "Heritage",
                subtitle = "Language Context",
                icon = Icons.Filled.Info,
                gradient = listOf(PrimaryDark, DarkSurface),
                onClick = onNavigateToCultural,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Help & About",
                subtitle = "App Info & FAQ",
                icon = Icons.Filled.Help,
                gradient = listOf(DarkSurfaceVariant, DarkCard),
                onClick = onNavigateToAbout,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextWhite.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
