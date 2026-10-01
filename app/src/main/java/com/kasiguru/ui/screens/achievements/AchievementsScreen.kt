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
import com.kasiguru.ui.components.BadgeTierIcon
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Muted
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

@Composable
fun AchievementsScreen(onNavigateBack: () -> Unit, onNavigateToActivity: (String) -> Unit = {},
    viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val pins = state.progress?.pinnedBadgeIds.orEmpty().split(',').filter { it.isNotBlank() }
    GroundScaffold(title = "Badges", onBack = onNavigateBack, pattern = GroundPattern.Arcs, compactTitle = true) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Your collection", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.tourAnchor(TourAnchor.ProgressBadgePanel))
                Text("${state.families.count { it.current != null }} of 11 badges · ${state.unlockedCount} of 66 tiers",
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("Beginner → Learner → Achiever → Expert → Master → Legend", color = Muted,
                    style = MaterialTheme.typography.bodySmall)
                Text("${state.progress?.activityXp ?: 0} activity XP · ${state.progress?.badgeBonusXp ?: 0} badge XP",
                    style = MaterialTheme.typography.bodySmall, color = BrandLime)
            }
            state.normalization?.takeIf { !it.acknowledged }?.let { report ->
                item {
                    SoftCard {
                        Text("Your progress has been updated", style = MaterialTheme.typography.titleMedium)
                        Text("Previous: ${report.originalXp} XP · Level ${report.originalLevel}\nNew baseline: ${report.normalizedXp} XP",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Completed learning, earned badges and unlocked content are kept. ${report.unsupportedHistory}",
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                        TextButton(onClick = viewModel::acknowledgeNormalization) { Text("Got it") }
                    }
                }
            }
            item {
                LazyRow(modifier = Modifier.tourAnchor(TourAnchor.ProgressFilter),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Learning", "Practice", "Games", "Community", "Legacy")) { section ->
                        FilterChip(selected = filter == section, onClick = { filter = section }, label = { Text(section) })
                    }
                }
            }
            if (filter == "Legacy") {
                item { Text("Previously earned badges", style = MaterialTheme.typography.titleLarge) }
                if (state.legacy.isEmpty()) item { Text("Your older earned badges will appear here.", color = Muted) }
                items(state.legacy, key = { it.id }) { badge ->
                    SoftCard {
                        Text(badge.name, style = MaterialTheme.typography.titleMedium)
                        Text(badge.description, color = Muted)
                        Text("Earned ${badge.unlockedDate ?: "previously"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(state.families.filter { filter == "All" || it.family.section == filter }, key = { it.family.id }) { family ->
                    SoftCard(onClick = { selectedId = family.family.id }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgeTierIcon(Modifier.size(64.dp), locked = family.current == null)
                            Column(Modifier.weight(1f)) {
                                Text(family.family.name, style = MaterialTheme.typography.titleMedium)
                                Text(family.current?.let { family.tier.label } ?: "Not earned yet", color = Muted)
                                family.next?.let { next ->
                                    Text("${family.value.coerceAtMost(next.requiredValue)} / ${next.requiredValue} ${family.family.unit} · Next: ${BadgeCatalog.tierFor(next.id)?.label}",
                                        style = MaterialTheme.typography.bodySmall)
                                    LinearProgressIndicator(progress = { (family.value.toFloat() / next.requiredValue).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                                } ?: Text("Legend achieved", color = BrandLime)
                                if (family.family.id in pins) Text("Pinned to profile", style = MaterialTheme.typography.labelSmall, color = BrandLime)
                            }
                        }
                    }
                }
            }
        }
    }
    state.families.firstOrNull { it.family.id == selectedId }?.let { family ->
        AlertDialog(onDismissRequest = { selectedId = null }, title = { Text(family.family.name) },
            text = {
                Column {
                    Text("One badge, six tiers. Each tier stays earned.", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(family.rows, key = { it.id }) { row ->
                            val tier = BadgeCatalog.tierFor(row.id) ?: BadgeTier.BEGINNER
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                BadgeTierIcon(Modifier.size(48.dp), locked = !row.isUnlocked)
                                Column(Modifier.weight(1f)) {
                                    Text(tier.label, style = MaterialTheme.typography.titleSmall)
                                    Text("${row.requiredValue} ${family.family.unit}", style = MaterialTheme.typography.bodySmall)
                                    Text(if (row.isUnlocked) "Earned · ${row.unlockedDate.orEmpty()}" else "${family.value.coerceAtMost(row.requiredValue)} / ${row.requiredValue}",
                                        style = MaterialTheme.typography.labelSmall, color = Muted)
                                    if (row.xpReward > 0) Text("+${row.xpReward} badge XP", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    if (family.current != null) TextButton(
                        enabled = family.family.id in pins || pins.size < 3,
                        onClick = { viewModel.pin(family.family.id) }) {
                        Text(if (family.family.id in pins) "Unpin from profile" else if (pins.size == 3) "Three badges already pinned" else "Pin to profile")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedId = null; onNavigateToActivity(family.family.id) }) { Text(family.family.action) } },
            dismissButton = { TextButton(onClick = { selectedId = null }) { Text("Close") } })
    }
}
