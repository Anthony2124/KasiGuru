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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.ui.components.MascotOwlSlot
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
                        "Leaderboard",
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Sticky "My Rank" Bottom Card
            uiState.currentUserEntry?.let { userEntry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
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
                                color = PlayPurpleStart,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${uiState.currentUserRank}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Your Current Rank",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoastMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = userEntry.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CoastInk,
                                    letterSpacing = (-0.2).sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = XpGold.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.FlashBold),
                                    contentDescription = null,
                                    tint = XpGoldDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${userEntry.totalXp} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = XpGoldDark
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PlayPurpleStart)
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
                    shadowElevation = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(StoriesDusk, PlayPurpleStart)))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kasiguranin Champions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Climb the rankings with daily learning streaks!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.CupBold),
                                    contentDescription = null,
                                    tint = XpGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─── 2. Gold Stat Card ───
            uiState.currentUserEntry?.let { userEntry ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )
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
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MascotOwlSlot(size = 44.dp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = userEntry.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CoastInk,
                                    letterSpacing = (-0.3).sp
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
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CoastInk,
                                            letterSpacing = (-0.4).sp
                                        )
                                        Text(
                                            text = "Total XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CoastInk.copy(alpha = 0.7f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(36.dp)
                                            .background(CoastInk.copy(alpha = 0.15f))
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = userEntry.levelTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CoastInk
                                        )
                                        Text(
                                            text = "Rank #${uiState.currentUserRank}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CoastInk.copy(alpha = 0.7f)
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
                        val isSelected = uiState.selectedFilter == filter
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setFilter(filter)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) PlayPurpleStart else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else CoastMuted
                            )
                        }
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
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastInk,
                    letterSpacing = (-0.2).sp
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
        // Rank 2 (Left - Silver)
        rank2?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1f),
                rank = 2,
                learner = learner,
                badgeColor = BadgeSilver,
                badgeIcon = Iconsax.MedalStar,
                cardHeight = 164.dp
            )
        }

        // Rank 1 (Center - Tallest - Gold)
        rank1?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1.1f),
                rank = 1,
                learner = learner,
                badgeColor = XpGold,
                badgeIcon = Iconsax.CupBold,
                cardHeight = 196.dp
            )
        }

        // Rank 3 (Right - Bronze)
        rank3?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1f),
                rank = 3,
                learner = learner,
                badgeColor = BadgeBronze,
                badgeIcon = Iconsax.Medal,
                cardHeight = 148.dp
            )
        }
    }
}

@Composable
private fun PodiumCard(
    modifier: Modifier,
    rank: Int,
    learner: LeaderboardEntity,
    badgeColor: Color,
    badgeIcon: Int,
    cardHeight: Dp
) {
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(24.dp),
        color = if (learner.isCurrentUser) PlayPurpleStart.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (rank == 1) 4.dp else 2.dp,
        border = if (learner.isCurrentUser) androidx.compose.foundation.BorderStroke(2.dp, PlayPurpleStart)
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = badgeIcon),
                    contentDescription = "Rank $rank",
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = learner.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontSize = 14.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = learner.levelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoastMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = badgeColor.copy(alpha = 0.18f)
            ) {
                Text(
                    text = "${learner.totalXp} XP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastInk,
                    fontSize = 10.sp
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
        shadowElevation = 1.dp,
        border = if (learner.isCurrentUser) androidx.compose.foundation.BorderStroke(2.dp, PlayPurpleStart)
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
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
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastMuted,
                    modifier = Modifier.width(28.dp)
                )

                Surface(
                    shape = CircleShape,
                    color = PlayPurpleStart.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = Iconsax.Profile),
                            contentDescription = learner.name,
                            tint = PlayPurpleStart,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = learner.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoastInk,
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp
                        )
                        if (learner.isCurrentUser) {
                            Text(
                                "(You)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PlayPurpleStart,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = learner.levelTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoastMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoastMuted,
                            fontSize = 11.sp
                        )
                        Icon(
                            painter = painterResource(id = Iconsax.FlashBold),
                            contentDescription = null,
                            tint = StreakEmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${learner.currentStreak}d streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = StreakEmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = XpGold.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${learner.totalXp} XP",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = XpGoldDark,
                    fontSize = 11.sp
                )
            }
        }
    }
}
