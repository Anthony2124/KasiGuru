package com.kasiguru.ui.screens.leaderboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Leaderboard 🏆",
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Sticky "My Rank" Bottom Card
            uiState.currentUserEntry?.let { userEntry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, HeroCardStart)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = HeroCardStart,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${uiState.currentUserRank}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = TextHeadingBlack
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Your Current Rank",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSubtleGray
                                )
                                Text(
                                    text = userEntry.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHeadingBlack
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HeroCardStart.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "${userEntry.totalXp} XP",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HeroCardStart)
            }
            return@Scaffold
        }

        val list = uiState.leaderboard
        val top3 = list.take(3)
        val rest = if (list.size > 3) list.drop(3) else emptyList()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── 1. Header Banner ───
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
                            .background(Brush.linearGradient(listOf(NudgeBurple, NudgeBink)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Kasiguranin Champions 🏆",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compete with fellow learners & climb the rankings!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // ─── 2. Gold Stat Card (Inspo Panel 6 "Ranking") ───
            uiState.currentUserEntry?.let { userEntry ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(PlayGoldStart, PlayGoldEnd)))
                                .padding(20.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = TextWhite,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "👑", fontSize = 32.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = userEntry.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextHeadingBlack
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${userEntry.totalXp}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = TextHeadingBlack
                                        )
                                        Text(
                                            text = "Total XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextHeadingBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(36.dp)
                                            .background(TextHeadingBlack.copy(alpha = 0.2f))
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = userEntry.levelTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = TextHeadingBlack
                                        )
                                        Text(
                                            text = "Rank #${uiState.currentUserRank}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextHeadingBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── 3. Filter Chips ───
            item {
                val filters = listOf("All-Time XP", "Weekly XP", "Streak Masters")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setFilter(filter)
                            },
                            label = { Text(filter, fontWeight = FontWeight.Bold) },
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
            }

            // ─── 4. Top 3 Podium Component ───
            if (top3.isNotEmpty()) {
                item {
                    Top3PodiumView(top3 = top3)
                }
            }

            // ─── 5. Ranked List Header ───
            item {
                Text(
                    text = "Top Learners (${list.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
            }

            // ─── 6. Rank List (#4 to #10+) ───
            itemsIndexed(rest, key = { _, item -> item.id }) { index, learner ->
                RankedLearnerRow(rank = index + 4, learner = learner)
            }
        }
    }
}

@Composable
private fun Top3PodiumView(top3: List<LeaderboardEntity>) {
    val rank1 = top3.getOrNull(0)
    val rank2 = top3.getOrNull(1)
    val rank3 = top3.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Left)
        rank2?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1f),
                rank = 2,
                learner = learner,
                badgeEmoji = "🥈",
                badgeColor = Color(0xFFC0C0C0),
                cardHeight = 160.dp
            )
        }

        // Rank 1 (Center - Tallest)
        rank1?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1.1f),
                rank = 1,
                learner = learner,
                badgeEmoji = "👑",
                badgeColor = HeroCardStart,
                cardHeight = 190.dp
            )
        }

        // Rank 3 (Right)
        rank3?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1f),
                rank = 3,
                learner = learner,
                badgeEmoji = "🥉",
                badgeColor = Color(0xFFCD7F32),
                cardHeight = 145.dp
            )
        }
    }
}

@Composable
private fun PodiumCard(
    modifier: Modifier,
    rank: Int,
    learner: LeaderboardEntity,
    badgeEmoji: String,
    badgeColor: Color,
    cardHeight: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(24.dp),
        color = if (learner.isCurrentUser) HeroCardStart.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (rank == 1) 4.dp else 2.dp,
        border = if (learner.isCurrentUser) androidx.compose.foundation.BorderStroke(2.dp, HeroCardStart) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = badgeEmoji, fontSize = 22.sp)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = learner.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = learner.levelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtleGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.25f)
            ) {
                Text(
                    text = "${learner.totalXp} XP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = TextHeadingBlack
                )
            }
        }
    }
}

@Composable
private fun RankedLearnerRow(rank: Int, learner: LeaderboardEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = if (learner.isCurrentUser) androidx.compose.foundation.BorderStroke(2.dp, PlayPurpleStart) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSubtleGray,
                    modifier = Modifier.width(28.dp)
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👤", fontSize = 20.sp)
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = learner.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        if (learner.isCurrentUser) {
                            Text("(You)", style = MaterialTheme.typography.labelSmall, color = TextSubtleGray, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "${learner.levelTitle} • 🔥 ${learner.currentStreak}d streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtleGray,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Text(
                    text = "${learner.totalXp} XP",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = TextHeadingBlack
                )
            }
        }
    }
}
