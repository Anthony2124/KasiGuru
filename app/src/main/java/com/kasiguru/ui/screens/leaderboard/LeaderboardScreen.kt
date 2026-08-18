package com.kasiguru.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.ui.components.MascotOwlSlot
import com.kasiguru.ui.components.clay.CanopyBackButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SegmentedToggle
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // The ViewModel keys off these exact strings; the segmented control shows shorter labels so
    // three options plus a rounded track don't crowd or wrap "Streak Masters" at typical widths.
    val filterKeys = listOf("All-Time XP", "Weekly XP", "Streak Masters")
    val filterLabels = listOf("All-Time", "This Week", "Streaks")

    CanopyScaffold(
        canopyHeight = 180.dp,
        canopyContent = {
            CanopyBackButton(onClick = onNavigateBack)
            Spacer(Modifier.height(Space.sm))
            Text(text = "Kasiguranin Champions", style = MaterialTheme.typography.headlineMedium, color = OnCanopy)
            Text(
                text = "Climb the rankings with daily learning streaks!",
                style = MaterialTheme.typography.bodyMedium,
                color = OnCanopy
            )
            Spacer(Modifier.weight(1f))
            uiState.currentUserEntry?.let { userEntry ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GlassChip {
                        Icon(
                            painter = painterResource(id = Iconsax.FlashBold),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "You're #${uiState.currentUserRank} · ${userEntry.totalXp} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnCanopy
                        )
                    }
                }
            }
        },
        sheetContent = {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                return@CanopyScaffold
            }

            val list = uiState.leaderboard
            val top3 = list.take(3)
            val rest = if (list.size > 3) list.drop(3) else emptyList()
            val selectedFilterIndex = filterKeys.indexOf(uiState.selectedFilter).coerceAtLeast(0)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.md, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                uiState.currentUserEntry?.let { userEntry ->
                    item {
                        ClaySurface(face = Gold, lipColor = GoldDeep, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MascotOwlSlot(size = 44.dp)
                                }
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = userEntry.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RewardInk
                                )
                                Spacer(Modifier.height(Space.sm))
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
                                            color = RewardInk
                                        )
                                        Text(
                                            text = "Total XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = RewardInk.copy(alpha = 0.7f)
                                        )
                                    }
                                    Box(Modifier.width(1.dp).height(36.dp).background(RewardInk.copy(alpha = 0.15f)))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = userEntry.levelTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RewardInk
                                        )
                                        Text(
                                            text = "Rank #${uiState.currentUserRank}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = RewardInk.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SegmentedToggle(
                        options = filterLabels,
                        selectedIndex = selectedFilterIndex,
                        onSelect = { viewModel.setFilter(filterKeys[it]) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (top3.isNotEmpty()) {
                    item { Top3PodiumView(top3 = top3) }
                }

                item {
                    Text(
                        text = "Top Learners (${list.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                }

                itemsIndexed(rest, key = { _, item -> item.id }) { index, learner ->
                    RankedLearnerRow(rank = index + 4, learner = learner)
                }
            }
        }
    )
}

@Composable
private fun Top3PodiumView(top3: List<LeaderboardEntity>) {
    val rank1 = top3.getOrNull(0)
    val rank2 = top3.getOrNull(1)
    val rank3 = top3.getOrNull(2)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.Bottom
    ) {
        rank2?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1f),
                rank = 2,
                learner = learner,
                face = TierSilver,
                lip = TierSilverDeep,
                badgeIcon = Iconsax.MedalStar,
                cardHeight = 164.dp
            )
        }
        rank1?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1.1f),
                rank = 1,
                learner = learner,
                face = TierGold,
                lip = TierGoldDeep,
                badgeIcon = Iconsax.CupBold,
                cardHeight = 196.dp
            )
        }
        rank3?.let { learner ->
            PodiumCard(
                modifier = Modifier.weight(1f),
                rank = 3,
                learner = learner,
                face = TierBronze,
                lip = TierBronzeDeep,
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
    face: Color,
    lip: Color,
    badgeIcon: Int,
    cardHeight: Dp
) {
    ClaySurface(
        face = face,
        lipColor = lip,
        modifier = modifier.height(cardHeight),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Space.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = badgeIcon),
                contentDescription = "Rank $rank",
                tint = RewardInk,
                modifier = Modifier.size(22.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = learner.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = RewardInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontSize = 14.sp
                )
                Text(
                    text = learner.levelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = RewardInk.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Surface(shape = Shapes.pill, color = Color.White.copy(alpha = 0.35f)) {
                Text(
                    text = "${learner.totalXp} XP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = RewardInk,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RankedLearnerRow(rank: Int, learner: LeaderboardEntity) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Muted,
                    modifier = Modifier.width(28.dp)
                )

                Surface(shape = CircleShape, color = Violet.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = Iconsax.Profile),
                            contentDescription = learner.name,
                            tint = Violet,
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
                            color = Ink,
                            fontSize = 15.sp
                        )
                        if (learner.isCurrentUser) {
                            Text("(You)", style = MaterialTheme.typography.labelSmall, color = Violet, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = learner.levelTitle, style = MaterialTheme.typography.bodySmall, color = Muted, fontSize = 11.sp)
                        Text(text = "•", style = MaterialTheme.typography.bodySmall, color = Muted, fontSize = 11.sp)
                        // Coral/Gold measure 2.31/1.83 as a foreground on white (DESIGN.md's own
                        // measured failure) — Violet is the safe substitute the rule itself names.
                        Icon(
                            painter = painterResource(id = Iconsax.FlashBold),
                            contentDescription = null,
                            tint = Violet,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${learner.currentStreak}d streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = Violet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Surface(shape = Shapes.pill, color = Gold) {
                Text(
                    text = "${learner.totalXp} XP",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = RewardInk,
                    fontSize = 11.sp
                )
            }
        }
    }
}
