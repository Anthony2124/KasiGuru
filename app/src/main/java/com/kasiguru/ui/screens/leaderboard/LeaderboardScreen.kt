package com.kasiguru.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.ui.components.brand.*
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.components.states.*
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.tour.*

@Composable
fun LeaderboardScreen(onNavigateBack: () -> Unit, onOpenPlayer: (String) -> Unit = {}, viewModel: LeaderboardViewModel = hiltViewModel()) {
    GroundScaffold(title = "Leaderboard", onBack = onNavigateBack, compactTitle = true, pattern = GroundPattern.None) {
        LeaderboardContent(onOpenPlayer, viewModel = viewModel)
    }
}

@Composable
fun LeaderboardContent(onOpenPlayer: (String) -> Unit, modifier: Modifier = Modifier, viewModel: LeaderboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val keys = listOf("Weekly XP", "All-Time XP", "Streak Masters")
    val streak = state.selectedFilter == "Streak Masters"
    val weekly = state.selectedFilter == "Weekly XP"
    val top = state.leaderboard.filter { it.rank in 1..50 }.sortedBy { it.rank }
    Column(modifier.fillMaxSize()) {
        FilterPills(
            options = listOf("This week", "All-time", "Streaks"),
            selectedIndex = keys.indexOf(state.selectedFilter).coerceAtLeast(0),
            onSelect = { viewModel.setFilter(keys[it]) },
            modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.xs)
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (state.isLoading) item { LoadingState(label = "Loading rankings") }
            else if (top.isEmpty()) item { EmptyState(JepjepPose.Curious, "No one ranked yet", "Signed-in learners appear here when they practise.") }
            else {
                item {
                    val podium = top.take(3)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.Bottom) {
                        listOf(1, 0, 2).forEach { position ->
                            val player = podium.getOrNull(position)
                            if (player == null) Spacer(Modifier.weight(1f))
                            else SoftCard(modifier = Modifier.weight(1f).then(if (position == 0) Modifier.tourAnchor(TourAnchor.LeaderboardPlayer) else Modifier), shape = Shapes.tile,
                                color = if (position == 0) Gold.copy(alpha = .16f) else Surface,
                                onClick = { if (player.firebaseUid.isNotEmpty()) onOpenPlayer(player.firebaseUid) }, contentPadding = PaddingValues(Space.xs)) {
                                Column(Modifier.fillMaxWidth().padding(top = if (position == 0) 22.dp else 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    JepjepAvatarPortrait(JepjepAvatar.fromId(player.avatarIconId), size = 52.dp, level = player.level)
                                    Text(player.name, color = Ink, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("#${player.rank}", color = BrandLime, style = MaterialTheme.typography.headlineSmall)
                                    Text(figure(player, streak, weekly), color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                items(top.drop(3), key = { it.firebaseUid.ifBlank { it.id.toString() } }) { player ->
                    LeaderRow(player.rank, player, streak, weekly, { onOpenPlayer(player.firebaseUid) })
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.sm)) {
            MyRankCard(state.currentUserEntry, state.currentUserRank, state.myAvatarId, streak, weekly,
                { state.currentUserEntry?.firebaseUid?.takeIf { it.isNotEmpty() }?.let(onOpenPlayer) })
        }
    }
}

private fun figure(entry: LeaderboardEntity, streak: Boolean, weekly: Boolean): String =
    if (streak) "${entry.currentStreak} days" else "${if (weekly) entry.weeklyXp else entry.totalXp} XP"

@Composable
internal fun MyRankCard(entry: LeaderboardEntity?, rank: Int, avatarId: Int?, byStreak: Boolean, weekly: Boolean = false, onClick: () -> Unit = {}) {
    SoftCard(modifier = Modifier.fillMaxWidth(), border = BrandLime, color = LimeTint, onClick = if (entry != null) onClick else null,
        contentPadding = PaddingValues(Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JepjepAvatarPortrait(JepjepAvatar.fromId(entry?.avatarIconId ?: avatarId), size = 44.dp)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(if (rank > 0) "You · #$rank" else "Your rank", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(entry?.let { figure(it, byStreak, weekly) } ?: "Practise to join the rankings", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun LeaderRow(rank: Int, learner: LeaderboardEntity, byStreak: Boolean, weekly: Boolean = false, onClick: () -> Unit = {}) {
    SoftCard(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "Rank $rank, ${learner.name}, ${figure(learner, byStreak, weekly)}"
    }, shape = Shapes.tile,
        border = if (learner.isCurrentUser) BrandLime else BorderHairline,
        onClick = if (learner.firebaseUid.isNotBlank()) onClick else null, contentPadding = PaddingValues(Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$rank", color = Muted, modifier = Modifier.width(28.dp))
            JepjepAvatarPortrait(JepjepAvatar.fromId(learner.avatarIconId), size = 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = Space.sm)) {
                Text(if (learner.isCurrentUser) "${learner.name} (you)" else learner.name, color = Ink, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(learner.levelTitle, color = Faint, style = MaterialTheme.typography.bodySmall)
            }
            Text(figure(learner, byStreak, weekly), color = Ink, style = MaterialTheme.typography.labelLarge)
        }
    }
}
