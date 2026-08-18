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
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.clay.CanopyIconButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.Stat
import com.kasiguru.ui.components.clay.StatStrip
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
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

    CanopyScaffold(
        canopyHeight = 216.dp,
        canopyContent = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CanopyIconButton(iconRes = Iconsax.Setting, contentDescription = "Settings", onClick = onNavigateToSettings)
                Spacer(Modifier.width(Space.xs))
                CanopyIconButton(iconRes = Iconsax.Edit, contentDescription = "Edit profile", onClick = onNavigateToEditProfile)
            }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CasiguranAvatarPortrait(
                    resident = CasiguranResident.TEACHER,
                    size = 84.dp,
                    level = progress.level,
                    onClick = onNavigateToEditProfile
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = progress.fullName.ifBlank { progress.userName },
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnCanopy
                )
                Text(
                    text = "@${progress.userName.lowercase().replace(" ", "_")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnCanopy
                )
            }
            Spacer(Modifier.height(Space.md))
            StatStrip(
                stats = listOf(
                    Stat("Level", "${levelInfo.level}"),
                    Stat("Total XP", "${progress.totalXp}"),
                    Stat("Streak", "${progress.currentStreak}d")
                ),
                onCanopy = true
            )
        },
        sheetContent = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
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
                            StatDetailRow("Words mastered", "${progress.wordsLearned}", Iconsax.BookBold)
                            StatDetailRow("Current streak", "${progress.currentStreak} days", Iconsax.FlashBold)
                            StatDetailRow("Longest streak", "${progress.longestStreak} days", Iconsax.Medal)
                            StatDetailRow("Games played", "${progress.gamesPlayed}", Iconsax.Game)
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
