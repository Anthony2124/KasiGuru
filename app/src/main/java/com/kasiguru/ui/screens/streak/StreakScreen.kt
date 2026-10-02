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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kasiguru.ui.components.StandardBadgeMedal
import com.kasiguru.data.repository.DailyStreakQuota
import com.kasiguru.domain.gamification.*
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.brand.*
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.screens.learn.DayActivity
import com.kasiguru.ui.screens.learn.LearnViewModel
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.tour.*
import com.kasiguru.util.pluralize

/**
 * The streak page, in the order a learner asks about it: how long is it, what does today still
 * need, how did the week go, and what is the next milestone.
 *
 * The hero states the number. Today is a two-item checklist taken from the real quota (one review,
 * three games), so "what keeps my streak?" is answered by ticks rather than a sentence. The week is
 * seven flames or gaps. Milestones are the permanent Consistent Learner tiers as a ladder, with the
 * next one carrying its own progress bar.
 */
@Composable
fun StreakScreen(onBack: () -> Unit, onContinue: () -> Unit, viewModel: LearnViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPlan() }
    val progress = state.progress
    val family = BadgeCatalog.families.first { it.id == "consistent_learner" }
    val next = family.thresholds.firstOrNull { it > progress.longestStreak }
    GroundScaffold(title = "Your streak", onBack = onBack, pattern = GroundPattern.None) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Coral.copy(alpha = .14f), 0.35f to Ground))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = Space.navBarClearance),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            StreakHero(current = progress.currentStreak, longest = progress.longestStreak)

            TodayCard(quota = state.streakQuota, onContinue = onContinue)

            SoftCard(
                modifier = Modifier.fillMaxWidth().tourAnchor(TourAnchor.StreakWeek),
                shape = Shapes.tile,
                border = BorderHairline
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
                    val practised = state.week.count { it.practised }
                    Text("$practised of ${state.week.size} days", style = MaterialTheme.typography.labelLarge, color = Muted)
                }
                Spacer(Modifier.height(Space.md))
                WeekFlames(days = state.week)
            }

            SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
                Text("Streak milestones", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = next?.let { "Next: ${family.name} at $it ${if (it == 1) "day" else "days"}" }
                        ?: "Every streak tier earned!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(Space.md))
                family.thresholds.forEachIndexed { index, days ->
                    MilestoneRow(
                        days = days,
                        tier = BadgeTier.entries[index],
                        longest = progress.longestStreak,
                        isNext = days == next,
                        isLast = index == family.thresholds.lastIndex
                    )
                }
            }
        }
    }
}

/** The number, big, inside a flame medallion; the longest streak under it. A lost streak gets Jepjep and a kind line. */
@Composable
private fun StreakHero(current: Int, longest: Int) {
    Column(
        Modifier.fillMaxWidth().padding(top = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (current == 0 && longest > 0) {
            Jepjep(JepjepPose.Sad, height = 104.dp)
            Spacer(Modifier.height(Space.xs))
        } else {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Coral.copy(alpha = .35f), Coral.copy(alpha = .08f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(Iconsax.FlashBold), null, tint = CoralText, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(Space.sm))
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$current day streak" }
        ) {
            Text("$current", style = MaterialTheme.typography.displayLarge, color = Ink)
            Spacer(Modifier.width(Space.xs))
            Text(
                "day streak",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        if (current == 0 && longest > 0) {
            Text(
                "A fresh start counts. Let's practise today.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xs))
        }
        Row(
            modifier = Modifier
                .clip(Shapes.pill)
                .background(Surface)
                .border(1.dp, BorderHairline, Shapes.pill)
                .padding(horizontal = Space.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(Iconsax.Medal), null, tint = GoldText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Space.xxs))
            Text(
                "Longest: $longest ${if (longest == 1) "day" else "days"}",
                style = MaterialTheme.typography.labelLarge,
                color = Ink
            )
        }
    }
}

/** What today still needs, as ticks: one review and three games. The lime button is the way to do it. */
@Composable
private fun TodayCard(quota: DailyStreakQuota, onContinue: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            TagChip(
                label = if (quota.isQuotaMet) "Streak safe" else "In progress",
                tint = if (quota.isQuotaMet) GreenTint else AmberTint,
                labelColor = if (quota.isQuotaMet) GreenText else AmberText
            )
        }
        Spacer(Modifier.height(Space.sm))
        QuotaRow(
            done = quota.reviewCompleted,
            title = "Finish a review",
            detail = if (quota.reviewCompleted) "Done" else "Go through the words due today",
            iconRes = Iconsax.Repeat
        )
        Spacer(Modifier.height(Space.sm))
        val games = quota.gamesPlayed.coerceAtMost(quota.requiredGames)
        QuotaRow(
            done = games >= quota.requiredGames,
            title = "Play ${pluralize(quota.requiredGames, "game")}",
            detail = "$games of ${quota.requiredGames} played",
            iconRes = Iconsax.Game,
            fraction = games.toFloat() / quota.requiredGames.coerceAtLeast(1)
        )
        Spacer(Modifier.height(Space.md))
        ClayButton(
            label = if (quota.isQuotaMet) "Keep practising" else "Continue practising",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuotaRow(done: Boolean, title: String, detail: String, iconRes: Int, fraction: Float? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(Shapes.chip)
                .background(if (done) Lime else SurfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (done) Iconsax.TickCircle else iconRes),
                contentDescription = if (done) "Done" else "Not done yet",
                tint = if (done) OnLime else Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Muted)
            if (fraction != null && !done) {
                Spacer(Modifier.height(Space.xxs))
                KasiGuruProgressBar(progress = fraction, modifier = Modifier.fillMaxWidth(), height = 6.dp)
            }
        }
    }
}

/** Seven days: a lime flame where the learner practised, a coral ring on today if not yet, a quiet gap otherwise. */
@Composable
private fun WeekFlames(days: List<DayActivity>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "${day.label} ${day.dayOfMonth}, " + when {
                        day.practised -> "practised"
                        day.isToday -> "today, not practised yet"
                        else -> "not practised"
                    }
                }
            ) {
                Text(
                    day.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.isToday) Ink else Muted
                )
                Spacer(Modifier.height(Space.xxs))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                day.practised -> Lime
                                else -> SurfaceSunken
                            }
                        )
                        .then(
                            if (day.isToday && !day.practised) Modifier.border(2.dp, Coral, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.practised) {
                        Icon(painterResource(Iconsax.FlashBold), null, tint = OnLime, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            "${day.dayOfMonth}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (day.isToday) Ink else Faint
                        )
                    }
                }
            }
        }
    }
}

/** One rung of the Consistent Learner ladder. The next rung shows how far there is to go. */
@Composable
private fun MilestoneRow(days: Int, tier: BadgeTier, longest: Int, isNext: Boolean, isLast: Boolean) {
    val earned = longest >= days
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // The ladder's rail: a dot per rung joined by a line, lime up to what has been earned.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            // The rung's own Consistent Learner art: in colour once earned, greyed under a lock until then.
            StandardBadgeMedal(tier = tier, earned = earned, size = 40.dp, familyId = "consistent_learner")
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (earned) Coral.copy(alpha = .5f) else BorderHairline)
                )
            }
        }
        Spacer(Modifier.width(Space.sm))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else Space.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$days ${if (days == 1) "day" else "days"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (earned || isNext) Ink else Muted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    tier.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (earned) CoralText else Faint
                )
            }
            when {
                earned -> Text("Earned", style = MaterialTheme.typography.bodySmall, color = Muted)
                isNext -> {
                    Text(
                        "${days - longest} ${if (days - longest == 1) "day" else "days"} to go",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(Space.xxs))
                    KasiGuruProgressBar(
                        progress = longest.toFloat() / days,
                        modifier = Modifier.fillMaxWidth(),
                        height = 6.dp
                    )
                }
                else -> Text("Locked", style = MaterialTheme.typography.bodySmall, color = Faint)
            }
        }
    }
}
