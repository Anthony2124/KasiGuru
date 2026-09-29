package com.kasiguru.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.data.local.entity.MetricType
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLocked
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.Olive
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TierBronze
import com.kasiguru.ui.theme.TierBronzeDeep
import com.kasiguru.ui.theme.TierSilver
import com.kasiguru.ui.theme.TierSilverDeep
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.WidthClass
import com.kasiguru.ui.theme.rememberWidthClass
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor

/**
 * The badge families, in the order the filter shows them.
 *
 * Grouped by what the badge counts ([AchievementEntity.metricType]) rather than by the stored
 * `category` string. The old filter offered All / Level / Progress / Streaks against a corpus whose
 * categories also include Games, Contribution, Mastery and Social, so those badges were reachable
 * only under All. A badge whose metric is not listed here falls back to its own category name as a
 * family (see [familyOf]), so a new kind added from the admin portal still gets a filter of its own.
 */
private val FamilyOrder = listOf("Levels", "Words", "Streaks", "Games", "Stories", "Community")

internal fun familyOf(badge: AchievementEntity): String = when (badge.metricType) {
    MetricType.LEVEL -> "Levels"
    MetricType.WORDS_LEARNED, MetricType.CATEGORY_MASTERED -> "Words"
    MetricType.STREAK -> "Streaks"
    MetricType.GAMES_PLAYED, MetricType.PERFECT_GAME, MetricType.GAME_MODES_PLAYED,
    "perfectSixInOneSitting" -> "Games"
    MetricType.STORIES_COMPLETED -> "Stories"
    MetricType.SUBMISSIONS_MADE, MetricType.SUBMISSIONS_APPROVED, MetricType.WEEKLY_TOP_TEN -> "Community"
    else -> badge.category.ifBlank { "Other" }
}

private fun familyIcon(family: String): Int = when (family) {
    "Levels" -> Iconsax.MedalStar
    "Words" -> Iconsax.BookBold
    "Streaks" -> Iconsax.FlashBold
    "Games" -> Iconsax.GameBold
    "Stories" -> Iconsax.Book
    "Community" -> Iconsax.People
    else -> Iconsax.Trophy
}

/**
 * Badges: every achievement, earned or waiting. Pushed from Me and from Home's goal ring.
 *
 * The count leads, on a gold clay panel - clay is reserved for things you earn, and this panel counts
 * nothing else. Then a filter by family, and the wall. A locked badge shows what it is waiting for and
 * how far along the learner already is.
 */
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFamily by rememberSaveable { mutableStateOf(ALL) }
    val haptic = LocalHapticFeedback.current
    val columns = if (rememberWidthClass() == WidthClass.COMPACT) 2 else 4

    // Only families that actually have badges, in a fixed order, then anything unrecognised.
    val families = remember(uiState.achievements) {
        val present = uiState.achievements.map(::familyOf).toSet()
        listOf(ALL) + FamilyOrder.filter { it in present } + (present - FamilyOrder.toSet()).sorted()
    }
    val displayed = remember(selectedFamily, uiState.achievements) {
        if (selectedFamily == ALL) uiState.achievements
        else uiState.achievements.filter { familyOf(it) == selectedFamily }
    }

    GroundScaffold(
        title = "Badges",
        onBack = onNavigateBack,
        pattern = GroundPattern.None,
        content = {
            if (uiState.isLoading) {
                LoadingState(label = "Loading badges")
                return@GroundScaffold
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
                ),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "title") {
                    GroundTitleBlock(
                        title = "Badges",
                        subtitle = "Every badge here is earned, not given",
                        lead = {
                            ClaySurface(
                                face = Gold,
                                lipColor = GoldDeep,
                                shape = Shapes.panel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tourAnchor(TourAnchor.ProgressBadgePanel)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "${uiState.unlockedCount} / ${uiState.achievements.size}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = RewardInk
                                        )
                                        Text(
                                            text = "Badges unlocked",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = RewardInk
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = Iconsax.MedalStar),
                                        contentDescription = null,
                                        tint = RewardInk,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }, key = "filter") {
                    FamilyFilter(
                        families = families,
                        selected = selectedFamily,
                        onSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFamily = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .tourAnchor(TourAnchor.ProgressFilter)
                    )
                }

                if (displayed.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                        EmptyState(
                            pose = JepjepPose.Sitting,
                            title = "No badges here yet",
                            message = "Badges appear once the app has loaded them. Try All.",
                            actionLabel = "Show all",
                            onAction = { selectedFamily = ALL }
                        )
                    }
                } else {
                    items(displayed, key = { it.id }) { achievement ->
                        BadgeTile(achievement)
                    }
                }
            }
        }
    )
}

private const val ALL = "All"

/**
 * One chip per family, scrolling sideways: seven labels do not fit a segmented track on a phone, and
 * squeezing them is how the old filter came to show only four.
 */
@Composable
private fun FamilyFilter(
    families: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        contentPadding = PaddingValues(vertical = Space.xs)
    ) {
        items(families, key = { it }) { family ->
            val isSelected = family == selected
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(Shapes.pill)
                    .background(if (isSelected) Olive else Surface)
                    .border(1.dp, if (isSelected) BrandLime else BorderHairline, Shapes.pill)
                    .clickable(role = Role.Tab, onClick = { onSelect(family) })
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = Space.md, vertical = Space.xs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = family,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Ink else Muted
                )
            }
        }
    }
}

@Composable
private fun BadgeTile(achievement: AchievementEntity) {
    val isUnlocked = achievement.isUnlocked
    val family = familyOf(achievement)
    val fraction = if (achievement.requiredValue > 0) {
        (achievement.currentValue.toFloat() / achievement.requiredValue).coerceIn(0f, 1f)
    } else 0f
    val spoken = buildString {
        append(achievement.name).append(", ").append(achievement.description).append(". ")
        if (isUnlocked) append("Earned, ${achievement.xpReward} XP.")
        else append("Locked, ${achievement.currentValue} of ${achievement.requiredValue}.")
    }

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        shape = Shapes.tile,
        border = if (isUnlocked) BorderHairline else TrackNeutral,
        color = Surface
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isUnlocked) {
                val (tierFace, tierLip) = when (achievement.tier) {
                    "silver" -> TierSilver to TierSilverDeep
                    "bronze" -> TierBronze to TierBronzeDeep
                    else -> Gold to GoldDeep
                }
                ClayCircle(size = 56.dp, face = tierFace, lipColor = tierLip) {
                    Icon(
                        painter = painterResource(id = familyIcon(family)),
                        contentDescription = null,
                        tint = RewardInk,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(NodeLocked),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = familyIcon(family)),
                        contentDescription = null,
                        tint = NodeLockedInk,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(Space.sm))
            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (isUnlocked) Ink else Muted,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
            Spacer(Modifier.height(Space.sm))

            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(Shapes.pill)
                        .background(Gold.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.TickCircle),
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = "+${achievement.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = Ink)
                }
            } else {
                // Progress on every locked badge, even a one-step one: "0 of 1" still says what is
                // left, and a lock alone makes the badge a mystery box.
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(Shapes.pill).background(NodeLocked)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction).height(4.dp).clip(Shapes.pill).background(BrandLime)
                    )
                }
                Spacer(Modifier.height(Space.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = Iconsax.Lock),
                        contentDescription = null,
                        tint = NodeLockedInk,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${achievement.currentValue.coerceAtMost(achievement.requiredValue)} of ${achievement.requiredValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Faint
                    )
                }
            }
        }
    }
}
