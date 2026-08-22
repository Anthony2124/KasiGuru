package com.kasiguru.ui.screens.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.components.clay.GroundIconButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.SectionCaption
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint
import com.kasiguru.util.gamification.GamificationEngine

/**
 * Profile: a tall canopy carrying the avatar and headline stats, over a sheet of personal details, a
 * learning-overview summary, and an "Explore" section — this is the only place Leaderboard, Cultural
 * Heritage and About are reachable from since the old Home dashboard (which linked to all three) was
 * retired. A tab root — no back chevron, matching Learn's precedent — with its canopy actions
 * (settings, edit) as small circular icon buttons in the corner instead of a top bar.
 */
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToCultural: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    val progress = uiState.userProgress ?: com.kasiguru.data.local.entity.UserProgressEntity()
    val levelInfo = remember(progress.totalXp) { GamificationEngine.getLevelInfo(progress.totalXp) }
    val nextLevel = remember(levelInfo.level) { GamificationEngine.getNextLevelInfo(levelInfo.level) }
    val levelFraction = remember(progress.totalXp) { GamificationEngine.getXpProgressInLevel(progress.totalXp) }

    GroundScaffold(
        title = progress.fullName.ifBlank { progress.userName },
        subtitle = "@" + progress.userName.lowercase().replace(" ", "_"),
        pattern = GroundPattern.Arcs,
        // The identity card leads with the rank, so the name has nowhere else to be. Pinned in the
        // bar it is visible on arrival rather than only after a scroll that may never happen.
        compactTitle = true,
        actions = {
            GroundIconButton(Iconsax.Setting, "Settings", onNavigateToSettings)
            GroundIconButton(Iconsax.Edit, "Edit profile", onNavigateToEditProfile)
        },
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                // Identity leads. The name already sits in the bar, so this card spends its space on
                // what was earned rather than repeating it: rank, the title held, and the next level.
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CasiguranAvatarPortrait(
                                    resident = CasiguranResident.TEACHER,
                                    size = 72.dp,
                                    level = progress.level,
                                    onClick = onNavigateToEditProfile
                                )
                                Spacer(Modifier.width(Space.md))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = levelInfo.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Ink
                                    )
                                    Spacer(Modifier.height(Space.xxs))
                                    // Stored for every learner since onboarding and displayed nowhere
                                    // until now, despite being an earned title.
                                    TagChip(label = progress.titleBadge)
                                }
                            }

                            Spacer(Modifier.height(Space.md))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Level " + levelInfo.level,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Ink
                                )
                                Text(
                                    text = progress.totalXp.toString() + " XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Muted
                                )
                            }
                            Spacer(Modifier.height(Space.xs))
                            KasiGuruProgressBar(
                                progress = levelFraction,
                                modifier = Modifier.fillMaxWidth(),
                                height = 8.dp,
                                gradientColors = listOf(Violet, VioletDeep)
                            )
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                text = nextLevel?.let {
                                    (it.minXp - progress.totalXp).coerceAtLeast(0).toString() +
                                        " XP to " + it.title
                                } ?: "Highest rank reached",
                                style = MaterialTheme.typography.labelSmall,
                                color = Faint
                            )
                        }
                    }
                }

                // Achievements. This finally calls onNavigateToAchievements, which has been passed
                // from NavGraph since the tab was built and referenced by nothing.
                item {
                    SectionHeading(
                        text = "Achievements",
                        action = {
                            TextButton(onClick = onNavigateToAchievements) {
                                Text("See all", color = Violet, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                    SectionCaption(
                        text = uiState.unlockedCount.toString() + " of " +
                            uiState.achievements.size + " badges earned"
                    )
                    Spacer(Modifier.height(Space.sm))

                    val recent = uiState.recentlyUnlocked
                    if (recent.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            items(recent, key = { it.id }) { badge ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(72.dp)
                                ) {
                                    ClayCircle(face = Gold, lipColor = GoldDeep, size = 56.dp) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.MedalStar),
                                            contentDescription = null,
                                            tint = RewardInk,
                                            modifier = Modifier.size(26.dp).align(Alignment.Center)
                                        )
                                    }
                                    Spacer(Modifier.height(Space.xs))
                                    Text(
                                        text = badge.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Muted,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // A designed empty state: name the nearest badge and how close it is, rather
                        // than a blank row that reads as a loading failure.
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "No badges yet. Finish a lesson to earn your first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ink
                                )
                                uiState.closestLocked?.let { next ->
                                    Spacer(Modifier.height(Space.sm))
                                    Text(
                                        text = next.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Muted
                                    )
                                    Spacer(Modifier.height(Space.xxs))
                                    KasiGuruProgressBar(
                                        progress = if (next.requiredValue == 0) 0f
                                            else next.currentValue.toFloat() / next.requiredValue,
                                        modifier = Modifier.fillMaxWidth(),
                                        height = 6.dp,
                                        gradientColors = listOf(Violet, VioletDeep)
                                    )
                                    Spacer(Modifier.height(Space.xxs))
                                    Text(
                                        text = next.currentValue.toString() + " of " + next.requiredValue,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Faint
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeading(text = "Personal details")
                    Spacer(Modifier.height(Space.sm))
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                            ProfileInfoRow(Iconsax.Sms, "Email", progress.email.ifEmpty { "Not linked" })
                            ProfileInfoRow(
                                Iconsax.Calendar, "Age",
                                progress.age?.let { "$it years old" } ?: "Not set"
                            )
                            ProfileInfoRow(Iconsax.Location, "Address", progress.address.ifEmpty { "Not set" })
                        }
                    }
                }

                item {
                    SectionHeading(text = "Learning overview")
                    Spacer(Modifier.height(Space.sm))
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            // Two different, both-honest numbers. "Mastered" counts words that
                            // currently satisfy the SM-2 bar and can fall when one lapses;
                            // "practised" is the lifetime tally of words ever taken that far and
                            // only rises. The second used to be labelled "mastered", which made a
                            // number that never goes down stand for something that certainly can.
                            StatDetailRow("Words mastered", "${uiState.masteredCount}", Iconsax.BookBold)
                            StatDetailRow("Words practised", "${progress.wordsLearned}", Iconsax.Book)
                            StatDetailRow("Current streak", "${progress.currentStreak} days", Iconsax.FlashBold)
                            StatDetailRow("Longest streak", "${progress.longestStreak} days", Iconsax.Medal)
                            StatDetailRow("Games played", "${progress.gamesPlayed}", Iconsax.Game)
                            // Three figures the entity has always carried and Profile never showed.
                            StatDetailRow("Lessons completed", "${progress.lessonsCompleted}", Iconsax.Book)
                            StatDetailRow("Stories read", "${progress.storiesCompleted}", Iconsax.BookBold)
                            StatDetailRow(
                                "Accuracy",
                                if (progress.totalQuestionsAnswered == 0) "Not measured yet"
                                else "${(uiState.accuracy * 100).toInt()}%",
                                Iconsax.MedalStar
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(Space.xs))
                    SectionHeading(text = "Explore")
                    Spacer(Modifier.height(Space.sm))
                    SoftCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Space.xs)) {
                        Column {
                            ProfileLinkRow(Iconsax.MedalStar, Violet, "Leaderboard", "See how you rank this week", onNavigateToLeaderboard)
                            ProfileLinkRow(Iconsax.Courthouse, Coral, "Cultural heritage", "Stories and context from Casiguran", onNavigateToCultural)
                            ProfileLinkRow(Iconsax.InfoCircle, Gold, "About KasiGuru", "The project, the team, the mission", onNavigateToAbout)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ProfileLinkRow(iconRes: Int, accent: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(Shapes.chip).background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Faint)
        }
        Icon(painter = painterResource(id = Iconsax.ArrowRight), contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ProfileInfoRow(iconRes: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(VioletTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(Space.sm))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Faint)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Ink)
        }
    }
}

@Composable
private fun StatDetailRow(label: String, value: String, iconRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Space.xs))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}
