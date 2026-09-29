package com.kasiguru.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepAvatarPortrait
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SegmentedToggle
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.TierBronze
import com.kasiguru.ui.theme.TierGold
import com.kasiguru.ui.theme.TierSilver

/**
 * Where the learner stands. Real rankings only: every row comes from the server-maintained public
 * leaderboard (cached for offline), and an empty board says so instead of inventing players.
 *
 * Segmented period, then your own rank card with your avatar, then the list. The top three carry a
 * gold, silver or bronze rank disc - with the number on it, so the medal is never colour alone.
 */
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // The ViewModel keys off these exact strings; the toggle shows shorter labels.
    val filterKeys = listOf("All-Time XP", "Weekly XP", "Streak Masters")
    val filterLabels = listOf("All-time", "This week", "Streaks")
    val selectedFilterIndex = filterKeys.indexOf(uiState.selectedFilter).coerceAtLeast(0)
    val byStreak = uiState.selectedFilter == "Streak Masters"

    GroundScaffold(
        title = "Leaderboard",
        onBack = onNavigateBack,
        pattern = GroundPattern.None,
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item(key = "title") {
                    GroundTitleBlock(title = "Leaderboard", subtitle = "Learners of Kasiguranin, ranked")
                }
                item(key = "filter") {
                    SegmentedToggle(
                        options = filterLabels,
                        selectedIndex = selectedFilterIndex,
                        onSelect = { viewModel.setFilter(filterKeys[it]) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.isLoading) {
                    item(key = "loading") { LoadingState(label = "Loading the rankings") }
                    return@LazyColumn
                }

                item(key = "me") {
                    MyRankCard(
                        entry = uiState.currentUserEntry,
                        rank = uiState.currentUserRank,
                        avatarId = uiState.currentUserEntry?.avatarIconId ?: uiState.myAvatarId,
                        byStreak = byStreak
                    )
                }

                if (uiState.leaderboard.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            pose = JepjepPose.Curious,
                            title = "No one ranked yet",
                            message = "Rankings appear once signed-in learners start earning XP. Yours " +
                                "could be the first name here."
                        )
                    }
                } else {
                    itemsIndexed(uiState.leaderboard, key = { _, item -> item.id }) { index, learner ->
                        LeaderRow(rank = index + 1, learner = learner, byStreak = byStreak)
                    }
                }
            }
        }
    )
}

/** Your own standing, or - for a learner not on the board - why not, without a made-up rank. */
@Composable
private fun MyRankCard(entry: LeaderboardEntity?, rank: Int, avatarId: Int?, byStreak: Boolean) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BrandLime,
        color = LimeTint
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JepjepAvatarPortrait(avatar = JepjepAvatar.fromId(avatarId), size = 52.dp)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                if (entry != null) {
                    Text(text = "You are #$rank", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text(
                        text = if (byStreak) "${entry.currentStreak} day streak" else "${entry.totalXp} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                } else {
                    Text(text = "You're not ranked yet", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(
                        text = "Only signed-in learners appear here. Sign in from Me, then keep earning XP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(rank: Int, learner: LeaderboardEntity, byStreak: Boolean) {
    val medal: Color? = when (rank) {
        1 -> TierGold
        2 -> TierSilver
        3 -> TierBronze
        else -> null
    }
    val figure = if (byStreak) "${learner.currentStreak} day streak" else "${learner.totalXp} XP"
    val spoken = buildString {
        append("Rank $rank, ${learner.name}")
        if (learner.isCurrentUser) append(", you")
        append(", ${learner.levelTitle}, $figure")
    }

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        shape = Shapes.tile,
        border = if (learner.isCurrentUser) BrandLime else BorderHairline,
        contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(medal ?: SurfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (medal != null) RewardInk else Muted
                )
            }
            Spacer(Modifier.width(Space.sm))
            JepjepAvatarPortrait(avatar = JepjepAvatar.fromId(learner.avatarIconId), size = 44.dp)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (learner.isCurrentUser) "${learner.name} (you)" else learner.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = learner.levelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = if (byStreak) Iconsax.FlashBold else Iconsax.StarBold),
                    contentDescription = null,
                    tint = if (byStreak) Coral else Gold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(Space.xxs))
                Text(
                    text = if (byStreak) "${learner.currentStreak}d" else "${learner.totalXp}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink
                )
            }
        }
    }
}
