package com.kasiguru.ui.screens.achievements

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.components.clay.GlassPanel
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SegmentedToggle
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.TierBronze
import com.kasiguru.ui.theme.TierBronzeDeep
import com.kasiguru.ui.theme.TierSilver
import com.kasiguru.ui.theme.TierSilverDeep
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLocked
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Violet

/**
 * Progress: a tall canopy carrying the badge count, with a translucent glass panel riding at the
 * canopy's deep end — the one screen where DESIGN.md's "glass on vivid backdrops only" gets its
 * fullest expression, since the panel's lower edge sits right where the sheet overlaps upward into
 * the canopy — over the sheet's own badge grid.
 */
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val haptic = LocalHapticFeedback.current

    val displayedAchievements = remember(selectedCategory, uiState.achievements) {
        if (selectedCategory == "All") uiState.achievements
        else uiState.achievements.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    val total = uiState.achievements.size.coerceAtLeast(1)

    GroundScaffold(
        title = "Progress",
        subtitle = "Every badge here is earned, not given",
        pattern = GroundPattern.Arcs,
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                GroundTitleBlock(
                    title = "Progress",
                    subtitle = "Every badge here is earned, not given",
                    modifier = Modifier.padding(horizontal = Space.gutter),
                    // This was a GlassPanel on the canopy. Glass needs a vivid backdrop and the Ground
                    // is not one, so rather than degrade it into a duller SoftCard it becomes clay -
                    // which DESIGN.md reserves for things you earn, and this panel counts nothing else.
                    // Gold carries RewardInk at 9.00 measured, so no new contrast pairing is created.
                    lead = {
                        ClaySurface(
                            face = Gold,
                            lipColor = GoldDeep,
                            shape = Shapes.panel,
                            modifier = Modifier.fillMaxWidth()
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
                val categories = listOf("All", "Level", "Progress", "Streaks")
                SegmentedToggle(
                    options = categories,
                    selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    onSelect = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedCategory = categories[index]
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.gutter, vertical = Space.md)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.gutter, end = Space.gutter, bottom = Space.navBarClearance
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    items(displayedAchievements, key = { it.id }) { achievement ->
                        BadgeCard(achievement)
                    }
                }
            }
        }
    )
}

private fun achievementIcon(category: String): Int = when (category.lowercase()) {
    "level" -> Iconsax.MedalStar
    "streaks" -> Iconsax.FlashBold
    "progress" -> Iconsax.BookBold
    else -> Iconsax.Trophy
}

@Composable
private fun BadgeCard(achievement: AchievementEntity) {
    val isUnlocked = achievement.isUnlocked

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        color = if (isUnlocked) Surface else SurfaceSunken,
        elevation = if (isUnlocked) 4.dp else 0.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isUnlocked) {
                // Wires the tier tokens to a real badge instead of leaving them unused: gold is
                // still the default face for every untiered badge, so nothing already-shipped
                // changes look.
                val (tierFace, tierLip) = when (achievement.tier) {
                    "silver" -> TierSilver to TierSilverDeep
                    "bronze" -> TierBronze to TierBronzeDeep
                    else -> Gold to GoldDeep
                }
                ClayCircle(size = 56.dp, face = tierFace, lipColor = tierLip) {
                    Icon(
                        painter = painterResource(id = achievementIcon(achievement.category)),
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
                        painter = painterResource(id = Iconsax.Lock),
                        contentDescription = null,
                        tint = NodeLockedInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(Space.sm))
            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isUnlocked) Ink else Muted,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (!isUnlocked && achievement.requiredValue > 1) {
                Spacer(Modifier.height(Space.xs))
                val fraction = (achievement.currentValue.toFloat() / achievement.requiredValue).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(Shapes.pill).background(NodeLocked)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction).height(4.dp).clip(Shapes.pill).background(Violet)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${achievement.currentValue}/${achievement.requiredValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Faint
                )
            }
            Spacer(Modifier.height(Space.sm))
            Box(
                modifier = Modifier
                    .clip(Shapes.chip)
                    .background(if (isUnlocked) Gold.copy(alpha = 0.18f) else NodeLocked)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Icon(
                        painter = painterResource(id = if (isUnlocked) Iconsax.TickCircle else Iconsax.Lock),
                        contentDescription = null,
                        tint = if (isUnlocked) Ink else NodeLockedInk,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+${achievement.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) Ink else NodeLockedInk
                    )
                }
            }
        }
    }
}
