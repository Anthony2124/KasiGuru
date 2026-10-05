package com.kasiguru.ui.screens.xp

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.gamification.BadgeCatalog
import com.kasiguru.domain.gamification.BadgeTier
import com.kasiguru.domain.gamification.XpSource
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.StandardBadgeMedal
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.*
import com.kasiguru.util.gamification.GamificationEngine
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * The XP page, the streak page's twin, in the order a learner asks about it: how much do I have,
 * how far is the next level, what did today earn, how did the week go, where does it come from,
 * and which level is the next milestone.
 */
@Composable
fun XpScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onOpenGames: () -> Unit,
    viewModel: XpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.progress
    GroundScaffold(title = "Your XP", onBack = onBack, pattern = GroundPattern.None) {
        if (state.isLoading) {
            LoadingState(label = "Counting your XP")
            return@GroundScaffold
        }
        val family = BadgeCatalog.families.first { it.id == "journey_rank" }
        val nextRank = family.thresholds.firstOrNull { it > progress.level }
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Gold.copy(alpha = .12f), 0.35f to Ground))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = Space.navBarClearance),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            XpHero(totalXp = progress.totalXp, level = progress.level)

            TodayCard(earned = state.todayXp, goal = progress.dailyGoalXp, onContinue = onContinue)

            SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
                    Text("${state.weekXp} XP", style = MaterialTheme.typography.labelLarge, color = Muted)
                }
                Spacer(Modifier.height(Space.md))
                WeekBars(days = state.week)
            }

            SourcesCard(bySource = state.bySource)

            GameStarsCard(stars = state.gameStars, possible = state.gameStarsPossible, onOpenGames = onOpenGames)

            SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
                Text("Level milestones", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = nextRank?.let { "Next: ${family.name} at Level $it" } ?: "Every level milestone earned!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(Space.md))
                family.thresholds.forEachIndexed { index, level ->
                    MilestoneRow(
                        level = level,
                        tier = BadgeTier.entries[index],
                        totalXp = progress.totalXp,
                        currentLevel = progress.level,
                        isNext = level == nextRank,
                        isLast = index == family.thresholds.lastIndex
                    )
                }
            }
        }
    }
}

/** The total, big, inside a star medallion; the level and how far the next one is under it. */
@Composable
private fun XpHero(totalXp: Int, level: Int) {
    val current = GamificationEngine.getLevelInfo(totalXp)
    val next = GamificationEngine.getNextLevelInfo(current.level)
    Column(Modifier.fillMaxWidth().padding(top = Space.sm), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Gold.copy(alpha = .35f), Gold.copy(alpha = .08f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(Iconsax.StarBold), null, tint = GoldText, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(Space.sm))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$totalXp XP in total" }
        ) {
            Text("$totalXp", style = MaterialTheme.typography.displayLarge, color = Ink)
            Spacer(Modifier.width(Space.xs))
            Text("XP", style = MaterialTheme.typography.headlineSmall, color = Ink, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(Space.xs))
        SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Level $level", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
                Text(
                    text = next?.let { "${it.minXp - totalXp} XP to Level ${it.level}" } ?: "Top level reached",
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted
                )
            }
            Spacer(Modifier.height(Space.xs))
            KasiGuruProgressBar(
                progress = GamificationEngine.getXpProgressInLevel(totalXp),
                modifier = Modifier.fillMaxWidth(),
                height = 8.dp
            )
            if (next != null) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    "${totalXp - current.minXp} of ${next.minXp - current.minXp} XP this level",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

/** Today's XP against the daily goal the learner chose. The lime button is the way to earn more. */
@Composable
private fun TodayCard(earned: Int, goal: Int, onContinue: () -> Unit) {
    val met = goal > 0 && earned >= goal
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            TagChip(
                label = if (met) "Goal met" else "In progress",
                tint = if (met) GreenTint else AmberTint,
                labelColor = if (met) GreenText else AmberText
            )
        }
        Spacer(Modifier.height(Space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(Shapes.chip).background(if (met) Lime else SurfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(if (met) Iconsax.TickCircle else Iconsax.StarBold),
                    contentDescription = null,
                    tint = if (met) OnLime else GoldText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text("Daily goal", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(
                    if (met) "$earned XP today, goal of $goal reached" else "$earned of $goal XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                if (!met && goal > 0) {
                    Spacer(Modifier.height(Space.xxs))
                    KasiGuruProgressBar(
                        progress = (earned.toFloat() / goal).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        height = 6.dp
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        ClayButton(
            label = if (met) "Keep learning" else "Earn more XP",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Seven bars, oldest first, each as tall as its share of the best day; today is labelled in ink. */
@Composable
private fun WeekBars(days: List<Pair<LocalDate, Int>>) {
    val best = days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val today = LocalDate.now()
    Row(
        Modifier.fillMaxWidth().height(132.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { (day, xp) ->
            val label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clearAndSetSemantics { contentDescription = "$label ${day.dayOfMonth}, $xp XP" }
            ) {
                if (xp > 0) {
                    Text("$xp", style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
                    Spacer(Modifier.height(Space.xxs))
                }
                Box(
                    Modifier
                        .width(22.dp)
                        // A day with nothing still shows a stub, so the week reads as seven slots.
                        .height(if (xp > 0) (76f * xp / best).coerceAtLeast(6f).dp else 4.dp)
                        .clip(Shapes.pill)
                        .background(if (xp > 0) (if (day == today) Lime else Gold) else SurfaceSunken)
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day == today) Ink else Muted,
                    maxLines = 1
                )
            }
        }
    }
}

/** Every source that has earned something, largest first, with its share of the total. */
@Composable
private fun SourcesCard(bySource: Map<XpSource, Int>) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
        Text("Where your XP comes from", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(Space.sm))
        if (bySource.isEmpty()) {
            Text("Finish a lesson or play a game to earn your first XP.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        } else {
            val total = bySource.values.sum().coerceAtLeast(1)
            bySource.entries.sortedByDescending { it.value }.forEachIndexed { index, (source, xp) ->
                if (index > 0) Spacer(Modifier.height(Space.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "${source.label}, $xp XP" }
                ) {
                    Box(
                        Modifier.size(36.dp).clip(Shapes.chip).background(SurfaceSunken),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(iconFor(source)), null, tint = LimeText, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(source.label, style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.weight(1f))
                            Text("$xp XP", style = MaterialTheme.typography.labelLarge, color = Ink)
                        }
                        Spacer(Modifier.height(Space.xxs))
                        KasiGuruProgressBar(progress = xp.toFloat() / total, modifier = Modifier.fillMaxWidth(), height = 6.dp)
                    }
                }
            }
        }
    }
}

private fun iconFor(source: XpSource): Int = when (source) {
    XpSource.Lessons -> Iconsax.Teacher
    XpSource.Games -> Iconsax.Game
    XpSource.Reviews -> Iconsax.Repeat
    XpSource.Stories -> Iconsax.Book
    XpSource.Community -> Iconsax.People
    XpSource.Badges -> Iconsax.Medal
}

/** The stars won on game levels: a separate tally from XP, so it gets its own card and a way to earn more. */
@Composable
private fun GameStarsCard(stars: Int, possible: Int, onOpenGames: () -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        onClick = onOpenGames
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(Shapes.chip).background(Gold.copy(alpha = .18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(Iconsax.StarBold), null, tint = GoldText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text("$stars game ${if (stars == 1) "star" else "stars"}", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(
                    if (possible > 0) "Out of $possible across every game level" else "Clear a game level to earn stars",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            Text("Play", style = MaterialTheme.typography.labelLarge, color = LimeText)
        }
        if (possible > 0) {
            Spacer(Modifier.height(Space.xs))
            KasiGuruProgressBar(progress = stars.toFloat() / possible, modifier = Modifier.fillMaxWidth(), height = 6.dp)
        }
    }
}

/** One rung of the Journey Rank ladder. The next rung shows the XP still to earn. */
@Composable
private fun MilestoneRow(level: Int, tier: BadgeTier, totalXp: Int, currentLevel: Int, isNext: Boolean, isLast: Boolean) {
    val earned = currentLevel >= level
    val needed = GamificationEngine.LEVELS.getOrNull(level - 1)?.minXp ?: 0
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            StandardBadgeMedal(tier = tier, earned = earned, size = 40.dp, familyId = "journey_rank")
            if (!isLast) {
                Box(Modifier.width(2.dp).weight(1f).background(if (earned) Gold.copy(alpha = .5f) else BorderHairline))
            }
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Level $level",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (earned || isNext) Ink else Muted,
                    modifier = Modifier.weight(1f)
                )
                Text(tier.label, style = MaterialTheme.typography.labelMedium, color = if (earned) GoldText else Faint)
            }
            when {
                earned -> Text("Earned", style = MaterialTheme.typography.bodySmall, color = Muted)
                isNext -> {
                    Text("${(needed - totalXp).coerceAtLeast(0)} XP to go", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Spacer(Modifier.height(Space.xxs))
                    KasiGuruProgressBar(
                        progress = if (needed > 0) (totalXp.toFloat() / needed).coerceIn(0f, 1f) else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        height = 6.dp
                    )
                }
                else -> Text("$needed XP", style = MaterialTheme.typography.bodySmall, color = Faint)
            }
        }
    }
}
