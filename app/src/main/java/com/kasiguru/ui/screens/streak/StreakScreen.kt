package com.kasiguru.ui.screens.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kasiguru.domain.gamification.*
import com.kasiguru.ui.components.BadgeTierIcon
import com.kasiguru.ui.components.brand.*
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.screens.learn.LearnViewModel
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.tour.*

@Composable
fun StreakScreen(onBack: () -> Unit, onContinue: () -> Unit, viewModel: LearnViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPlan() }
    val progress = state.progress
    val family = BadgeCatalog.families.first { it.id == "consistent_learner" }
    val next = family.thresholds.firstOrNull { it > progress.longestStreak }
    GroundScaffold(title = "Your streak", onBack = onBack, compactTitle = true, pattern = GroundPattern.None) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Coral.copy(alpha = .15f), Ground)))
            .verticalScroll(rememberScrollState()).padding(Space.gutter), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(Iconsax.FlashBold), "Streak flame", tint = CoralText, modifier = Modifier.size(88.dp))
            Text("${progress.currentStreak}", style = MaterialTheme.typography.displayLarge, color = Ink)
            Text("day streak", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Text("Longest streak: ${progress.longestStreak} ${if (progress.longestStreak == 1) "day" else "days"}", color = Muted)
            Spacer(Modifier.height(Space.lg))
            WeekStrip(modifier = Modifier.tourAnchor(TourAnchor.StreakWeek), days = state.week.map { day -> DayMark(day.label, day.dayOfMonth, when {
                day.isToday && day.practised -> DayState.TodayDone
                day.isToday -> DayState.Today
                day.practised -> DayState.Done
                else -> DayState.Missed
            }) }, onCanopy = false)
            Spacer(Modifier.height(Space.lg))
            if (progress.currentStreak == 0 && progress.longestStreak > 0) {
                Jepjep(JepjepPose.Sad, height = 96.dp)
                Text("A fresh start counts. Let's practise today.", color = Ink)
            }
            Text(if (state.streakQuota.isQuotaMet) "Today counts. Your streak is safe!" else
                "Today: ${if (state.streakQuota.reviewCompleted) "review done" else "finish a review"} · ${state.streakQuota.gamesPlayed.coerceAtMost(state.streakQuota.requiredGames)}/${state.streakQuota.requiredGames} games",
                style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(Space.lg))
            Text(next?.let { "Next Consistent Learner badge: $it ${if (it == 1) "day" else "days"}" } ?: "Every streak tier earned!", color = Ink)
            family.thresholds.chunked(3).forEachIndexed { row, thresholds ->
                Row(Modifier.fillMaxWidth().padding(top = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    thresholds.forEachIndexed { column, days ->
                        val earned = progress.longestStreak >= days
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(64.dp).background(if (earned) Coral.copy(alpha = .2f) else SurfaceSunken, CircleShape)
                                .border(if (days == next) 2.dp else 1.dp, if (days == next) Coral else BorderHairline, CircleShape), contentAlignment = Alignment.Center) {
                                BadgeTierIcon(modifier = Modifier.size(36.dp), locked = !earned)
                            }
                            Text("$days ${if (days == 1) "day" else "days"}", color = Ink, style = MaterialTheme.typography.labelLarge)
                            Text(BadgeTier.entries[row * 3 + column].label, color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.lg))
            ClayButton("Continue practising", onContinue, modifier = Modifier.fillMaxWidth())
        }
    }
}
