package com.kasiguru.ui.screens.achievements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.MetricType
import com.kasiguru.domain.gamification.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.StandardBadgeMedal
import com.kasiguru.ui.components.badgeTierColor
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor

// Kept for archived badges and callers grouping the original achievement catalogue.
internal fun familyOf(badge: AchievementEntity): String = when (badge.metricType) {
    MetricType.LEVEL -> "Levels"
    MetricType.WORDS_LEARNED, MetricType.CATEGORY_MASTERED -> "Words"
    MetricType.STREAK -> "Streaks"
    MetricType.GAMES_PLAYED, MetricType.PERFECT_GAME, MetricType.GAME_MODES_PLAYED,
    "perfectSixInOneSitting" -> "Games"
    MetricType.STORIES_COMPLETED -> "Stories"
    MetricType.SUBMISSIONS_MADE, MetricType.SUBMISSIONS_APPROVED, MetricType.WEEKLY_TOP_TEN -> "Community"
    else -> BadgeCatalog.familyFor(badge.id)?.section ?: badge.category.ifBlank { "Other" }
}

/**
 * Badges: eleven families, each one badge with six permanent tiers.
 *
 * A summary card states the collection and how the tiers climb; filter pills narrow by section; the
 * families sit in a two-column grid of medals, each wearing its current tier's colour with a six-pip
 * track and the progress to the next tier, so the wall reads at a glance instead of as a list of
 * same-sized rows. Tapping one opens a sheet with the whole ladder, pinning and the way to earn more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onNavigateBack: () -> Unit, onNavigateToActivity: (String) -> Unit = {},
    viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val pins = state.progress?.pinnedBadgeIds.orEmpty().split(',').filter { it.isNotBlank() }
    val sections = listOf("All", "Learning", "Practice", "Games", "Community", "Legacy")
    GroundScaffold(title = "Badges", onBack = onNavigateBack, pattern = GroundPattern.Arcs) {
        LazyColumn(
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.navBarClearance),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item { GroundTitleBlock(title = "Badges") }
            item {
                CollectionSummary(
                    families = state.families,
                    tiersEarned = state.unlockedCount,
                    tiersTotal = state.achievements.size.takeIf { it > 0 } ?: 66,
                    activityXp = state.progress?.activityXp ?: 0,
                    badgeXp = state.progress?.badgeBonusXp ?: 0,
                    modifier = Modifier.tourAnchor(TourAnchor.ProgressBadgePanel)
                )
            }
            state.normalization?.takeIf { !it.acknowledged }?.let { report ->
                item {
                    SoftCard(shape = Shapes.tile, border = BorderHairline) {
                        Text("Your progress has been updated", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text("Previous: ${report.originalXp} XP · Level ${report.originalLevel}\nNew baseline: ${report.normalizedXp} XP",
                            style = MaterialTheme.typography.bodyMedium, color = Ink)
                        Text("Completed learning, earned badges and unlocked content are kept. ${report.unsupportedHistory}",
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                        TextButton(onClick = viewModel::acknowledgeNormalization) { Text("Got it", color = LimeText) }
                    }
                }
            }
            item {
                LazyRow(
                    modifier = Modifier.tourAnchor(TourAnchor.ProgressFilter).padding(vertical = Space.xxs),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    item {
                        FilterPills(
                            options = sections,
                            selectedIndex = sections.indexOf(filter).coerceAtLeast(0),
                            onSelect = { filter = sections[it] }
                        )
                    }
                }
            }
            if (filter == "Legacy") {
                item { Text("Previously earned badges", style = MaterialTheme.typography.titleLarge, color = Ink) }
                if (state.legacy.isEmpty()) item { Text("Your older earned badges will appear here.", color = Muted) }
                items(state.legacy, key = { it.id }) { badge ->
                    SoftCard(shape = Shapes.tile, border = BorderHairline) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StandardBadgeMedal(tier = null, earned = true, size = 48.dp)
                            Spacer(Modifier.width(Space.sm))
                            Column(Modifier.weight(1f)) {
                                Text(badge.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                                Text(badge.description, style = MaterialTheme.typography.bodySmall, color = Muted)
                                Text("Earned ${badge.unlockedDate ?: "previously"}", style = MaterialTheme.typography.labelSmall, color = Faint)
                            }
                        }
                    }
                }
            } else {
                // Earned badges first, highest tier first; then the locked ones nearest to their
                // first tier. What the learner has, then what they are about to get.
                val shown = state.families
                    .filter { filter == "All" || it.family.section == filter }
                    .sortedWith(
                        compareByDescending<FamilyProgress> { fp -> fp.rows.count { it.isUnlocked } }
                            .thenByDescending { fp ->
                                fp.next?.let { (fp.value.toFloat() / it.requiredValue).coerceIn(0f, 1f) } ?: 1f
                            }
                    )
                items(shown.chunked(2), key = { row -> row.joinToString { it.family.id } }) { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        row.forEach { family ->
                            BadgeTile(
                                family = family,
                                pinned = family.family.id in pins,
                                onClick = { selectedId = family.family.id },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    state.families.firstOrNull { it.family.id == selectedId }?.let { family ->
        ModalBottomSheet(
            onDismissRequest = { selectedId = null },
            containerColor = Surface
        ) {
            BadgeDetail(
                family = family,
                pinned = family.family.id in pins,
                canPin = family.family.id in pins || pins.size < 3,
                onPin = { viewModel.pin(family.family.id) },
                onAction = { selectedId = null; onNavigateToActivity(family.family.id) }
            )
        }
    }
}

/** How many badges and tiers, the six tier colours in order, and where the XP came from. */
@Composable
private fun CollectionSummary(
    families: List<FamilyProgress>,
    tiersEarned: Int,
    tiersTotal: Int,
    activityXp: Int,
    badgeXp: Int,
    modifier: Modifier = Modifier
) {
    val badgesEarned = families.count { it.current != null }
    SoftCard(modifier = modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = if (tiersTotal == 0) 0f else tiersEarned.toFloat() / tiersTotal,
                size = 76.dp,
                strokeWidth = 7.dp,
                color = Lime,
                contentDescription = "$tiersEarned of $tiersTotal tiers earned"
            ) {
                Text("$tiersEarned", style = MaterialTheme.typography.headlineSmall, color = Ink)
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text("$badgesEarned of ${families.size} badges", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text("$tiersEarned of $tiersTotal tiers earned", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(Modifier.height(Space.xxs))
                Text("+$badgeXp badge XP · $activityXp activity XP", style = MaterialTheme.typography.labelMedium, color = LimeText)
            }
        }
        Spacer(Modifier.height(Space.md))
        // The ladder every badge climbs, in its colours, with how many families sit on each rung.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
            BadgeTier.entries.forEach { tier ->
                val atTier = families.count { it.current != null && it.tier == tier }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(Shapes.pill)
                            .background(badgeTierColor(tier))
                    )
                    Spacer(Modifier.height(Space.xxs))
                    Text(tier.label, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
                    Text("$atTier", style = MaterialTheme.typography.labelMedium, color = Ink)
                }
            }
        }
    }
}

/** One family in the grid: its medal in the current tier's colour, six pips, and the way to the next tier. */
@Composable
private fun BadgeTile(family: FamilyProgress, pinned: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val earned = family.current != null
    val earnedCount = family.rows.count { it.isUnlocked }
    SoftCard(
        modifier = modifier,
        shape = Shapes.tile,
        border = if (earned) badgeTierColor(family.tier).copy(alpha = .55f) else BorderHairline,
        onClick = onClick,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(Space.xxs))
                StandardBadgeMedal(tier = family.tier, earned = earned, size = 64.dp, familyId = family.family.id)
                Spacer(Modifier.height(Space.xs))
                Text(
                    family.family.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (earned) family.tier.label else "Not earned yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (earned) LimeText else Faint
                )
                Spacer(Modifier.height(Space.xs))
                TierPips(earned = earnedCount)
                Spacer(Modifier.height(Space.xs))
                if (family.isComingSoon) {
                    Text("Coming soon", style = MaterialTheme.typography.labelMedium, color = Muted)
                } else family.next?.let { next ->
                    KasiGuruProgressBar(
                        progress = (family.value.toFloat() / next.requiredValue).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        height = 6.dp
                    )
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        family.family.progress(family.value.coerceAtMost(next.requiredValue), next.requiredValue),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        maxLines = 1
                    )
                } ?: Text("Legend achieved", style = MaterialTheme.typography.labelMedium, color = LimeText)
            }
            if (pinned) {
                Icon(
                    painterResource(Iconsax.StarBold),
                    contentDescription = "Pinned to profile",
                    tint = GoldText,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                )
            }
        }
    }
}

/** Six dots, one per tier, filled in each tier's colour up to the ones earned. */
@Composable
private fun TierPips(earned: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BadgeTier.entries.forEachIndexed { index, tier ->
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (index < earned) badgeTierColor(tier) else TrackNeutral)
            )
        }
    }
}

/** The sheet for one family: the medal, the whole ladder with each rung's requirement and status, pinning and the way to earn more. */
@Composable
private fun BadgeDetail(
    family: FamilyProgress,
    pinned: Boolean,
    canPin: Boolean,
    onPin: () -> Unit,
    onAction: () -> Unit
) {
    val earned = family.current != null
    LazyColumn(
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                StandardBadgeMedal(tier = family.tier, earned = earned, size = 88.dp, familyId = family.family.id)
                Spacer(Modifier.height(Space.sm))
                Text(family.family.name, style = MaterialTheme.typography.headlineSmall, color = Ink, textAlign = TextAlign.Center)
                Text(
                    if (earned) "${family.tier.label} · ${family.family.amount(family.value)}" else "Not earned yet · ${family.family.amount(family.value)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    "One badge, six tiers. Each tier stays earned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint
                )
                Spacer(Modifier.height(Space.md))
            }
        }
        items(family.rows, key = { it.id }) { row ->
            val tier = BadgeCatalog.tierFor(row.id) ?: BadgeTier.BEGINNER
            val isNext = row.id == family.next?.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Shapes.tile)
                    .background(if (isNext) LimeTint else SurfaceSunken)
                    .then(if (isNext) Modifier.border(1.dp, Lime, Shapes.tile) else Modifier)
                    .padding(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StandardBadgeMedal(tier = tier, earned = row.isUnlocked, size = 40.dp, familyId = family.family.id)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tier.label, style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.weight(1f))
                        if (row.xpReward > 0) Text("+${row.xpReward} XP", style = MaterialTheme.typography.labelMedium, color = GoldText)
                    }
                    Text(family.family.amount(row.requiredValue), style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (row.isUnlocked) {
                        Text(
                            "Earned" + row.unlockedDate?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = LimeText
                        )
                    } else {
                        Spacer(Modifier.height(Space.xxs))
                        KasiGuruProgressBar(
                            progress = (family.value.toFloat() / row.requiredValue).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            height = 5.dp
                        )
                        Text(
                            "${family.value.coerceAtMost(row.requiredValue)} / ${row.requiredValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Faint
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(Space.sm))
            if (family.isComingSoon) {
                // Stories are not narrated yet, so there is nothing to do towards this one today.
                Text(
                    "Stories are coming soon. This badge opens up when they arrive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ClayButton(label = family.family.action, onClick = onAction, modifier = Modifier.fillMaxWidth())
            }
            if (earned) {
                Spacer(Modifier.height(Space.xs))
                ClayButton(
                    label = when {
                        pinned -> "Unpin from profile"
                        !canPin -> "Three badges already pinned"
                        else -> "Pin to profile"
                    },
                    onClick = onPin,
                    enabled = canPin,
                    tone = ClayButtonTone.Quiet,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
