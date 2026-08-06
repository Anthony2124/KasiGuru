package com.kasiguru.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val haptic = LocalHapticFeedback.current

    val displayedAchievements = remember(selectedCategory, uiState.achievements) {
        if (selectedCategory == "All") {
            uiState.achievements
        } else {
            uiState.achievements.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Badges & Achievements",
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
                CircularProgressIndicator(color = QuestsCardEnd)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Summary Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(QuestsCardStart, QuestsCardEnd)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unlocked Badges",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextHeadingBlack,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.unlockedCount} / ${uiState.achievements.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextHeadingBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        KasiGuruProgressBar(
                            progress = if (uiState.achievements.isNotEmpty()) {
                                uiState.unlockedCount.toFloat() / uiState.achievements.size
                            } else 0f,
                            gradientColors = listOf(TextHeadingBlack, TextHeadingBlack)
                        )
                    }
                }
            }

            // Weekly Mission Strip (Inspo Panel 3)
            com.kasiguru.ui.components.WeeklyMissionStrip(
                currentStreak = uiState.unlockedCount,
                onCheckInClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Chips
            val categories = listOf("All", "Level", "Progress", "Streaks")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat
                        },
                        label = { Text(cat, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PlayPurpleStart,
                            selectedLabelColor = TextWhite,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = TextSubtleGray
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedAchievements, key = { it.id }) { achievement ->
                    BadgeCard(achievement = achievement)
                }
            }
        }
    }
}

/**
 * Badge Card rendering:
 * - Unlocked: Full vibrant color, gold border, full opacity emoji, green "✓ Unlocked" pill.
 * - Locked: Visible on screen in elegant gray/grayscale style, 45% alpha emoji, gray "🔒 Locked" pill.
 */
@Composable
private fun BadgeCard(achievement: AchievementEntity) {
    val isUnlocked = achievement.isUnlocked

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (isUnlocked) MaterialTheme.colorScheme.surface else Color(0xFFF2F3F5),
        shadowElevation = if (isUnlocked) 2.dp else 0.dp,
        border = if (isUnlocked) androidx.compose.foundation.BorderStroke(1.5.dp, HeroCardStart) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) QuestsCardStart else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.iconEmoji,
                    fontSize = 28.sp,
                    modifier = Modifier.alpha(if (isUnlocked) 1.0f else 0.45f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) TextHeadingBlack else TextSubtleGray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) TextSubtleGray else Color.Gray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isUnlocked) Success.copy(alpha = 0.15f) else Color(0xFFE0E0E0)
            ) {
                Text(
                    text = if (isUnlocked) "✓ Unlocked (+${achievement.xpReward} XP)" else "🔒 Locked (+${achievement.xpReward} XP)",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Success else TextSubtleGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
