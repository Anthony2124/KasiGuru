package com.kasiguru.ui.screens.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kasiguru.ui.components.clay.SectionCaption
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Space

/** How many learners the glance shows before "See all". */
private const val PREVIEW_SIZE = 3

/**
 * The leaderboard at a glance, for Practice: where you stand, then the top three, with the full
 * board a tap away. Practice is where XP is earned, so the rankings sit where a learner sees them
 * move without leaving the page.
 *
 * All-time XP only. The period toggle stays on the full screen; carrying it here would turn a glance
 * into a second leaderboard. The rows and the rank card are the full screen's own, so the two never
 * disagree about how a learner is shown.
 */
@Composable
fun LeaderboardPreview(
    state: LeaderboardUiState,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val byStreak = state.selectedFilter == "Streak Masters"

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        SectionHeading(
            text = "Leaderboard",
            action = {
                TextButton(
                    onClick = onSeeAll,
                    modifier = Modifier.semantics { contentDescription = "See the full leaderboard" }
                ) {
                    Text("See all", color = Lime, style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        if (state.isLoading) {
            LoadingState(label = "Loading the rankings")
            return@Column
        }

        MyRankCard(
            entry = state.currentUserEntry,
            rank = state.currentUserRank,
            avatarId = state.currentUserEntry?.avatarIconId ?: state.myAvatarId,
            byStreak = byStreak
        )

        if (state.leaderboard.isEmpty()) {
            SectionCaption(text = "No one is ranked yet. Yours could be the first name here.")
        } else {
            state.leaderboard.take(PREVIEW_SIZE).forEachIndexed { index, learner ->
                LeaderRow(rank = index + 1, learner = learner, byStreak = byStreak)
            }
        }
    }
}
