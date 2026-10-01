package com.kasiguru.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.gamification.*
import com.kasiguru.domain.lesson.LearningTree
import com.kasiguru.ui.components.StandardBadgeMedal
import com.kasiguru.ui.components.brand.*
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.components.states.*
import com.kasiguru.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PublicProfileScreen(onBack: () -> Unit, onReport: () -> Unit, viewModel: PublicProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var menu by remember { mutableStateOf(false) }
    GroundScaffold(title = state.profile?.displayName ?: "Player profile", onBack = onBack, compactTitle = true, pattern = GroundPattern.None,
        actions = { Box {
            TextButton(onClick = { menu = true }) { Text("More", color = Ink) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Report player") }, onClick = { menu = false; onReport() })
            }
        } }) {
        val profile = state.profile
        if (state.loading) LoadingState(label = "Loading player profile")
        else if (profile == null) EmptyState(JepjepPose.Curious, "Profile unavailable", state.error ?: "This learner hasn't shared a public profile yet.", actionLabel = "Try again", onAction = viewModel::refresh)
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            item {
                Box(Modifier.fillMaxWidth().height(184.dp).clip(Shapes.panel)) {
                    Image(painterResource(Scenery.forProfile(profile.profileBackgroundId).res), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))))
                    Text(state.weeklyRank?.let { "#$it this week" } ?: "Learning profile", color = Cream, modifier = Modifier.align(Alignment.BottomEnd).padding(Space.md))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.sm)) {
                    JepjepAvatarPortrait(JepjepAvatar.fromId(profile.profileIconId), size = 88.dp, level = profile.level)
                    Column(Modifier.weight(1f).padding(start = Space.sm)) {
                        Text(profile.displayName, style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text(com.kasiguru.util.gamification.GamificationEngine.getLevelInfo(profile.totalXp).title, color = BrandLime)
                        val joined = if (profile.createdAt > 0) Instant.ofEpochMilli(profile.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM yyyy")) else null
                        val active = if (profile.updatedAt > 0) Instant.ofEpochMilli(profile.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate() else null
                        Text(listOfNotNull(joined?.let { "Joined $it" }, active?.let { if (it == java.time.LocalDate.now()) "active today" else "active $it" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            if (state.error != null) item { TextButton(onClick = viewModel::refresh) { Text(state.error.orEmpty(), color = Muted) } }
            item {
                listOf("day streak" to profile.currentStreak, "total XP" to profile.totalXp,
                    "words learned" to profile.wordsLearned, "lessons done" to profile.lessonsCompleted).chunked(2).forEach { stats ->
                    Row(Modifier.fillMaxWidth().padding(bottom = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        stats.forEach { (label, value) -> SoftCard(Modifier.weight(1f)) {
                            Text("$value", color = Ink, style = MaterialTheme.typography.headlineMedium)
                            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
                        } }
                    }
                }
            }
            if (state.showcase.isNotEmpty()) item {
                SectionHeading("Showcase")
                Row(Modifier.fillMaxWidth().padding(top = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    state.showcase.forEach { badge -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        StandardBadgeMedal(BadgeCatalog.tierFor(badge.id), true)
                        Text(badge.name, color = Ink, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        Text(BadgeCatalog.tierFor(badge.id)?.label.orEmpty(), color = Muted, style = MaterialTheme.typography.labelSmall)
                    } }
                }
            }
            item {
                SectionHeading("You vs ${profile.displayName} · this week")
                val theirs = viewModel.weeklyXp(profile)
                val max = maxOf(theirs, state.myWeeklyXp, 1)
                Column(Modifier.padding(top = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(profile.displayName to theirs, "You" to state.myWeeklyXp).forEach { (name, xp) ->
                        Text("$name · $xp XP", color = Ink)
                        com.kasiguru.ui.components.KasiGuruProgressBar(xp.toFloat() / max, modifier = Modifier.fillMaxWidth(), height = 8.dp)
                    }
                }
            }
            item { SectionHeading("Badges · ${profile.badgeIds.size} of 66 tiers") }
            items(BadgeCatalog.rows().chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    row.forEach { badge -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        StandardBadgeMedal(BadgeCatalog.tierFor(badge.id), badge.id in profile.badgeIds)
                        Text(badge.name, color = Ink, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text(BadgeCatalog.tierFor(badge.id)?.label.orEmpty(), color = Muted, style = MaterialTheme.typography.labelSmall)
                    } }
                }
            }
            item { SectionHeading("Path progress") }
            items(LearningTree.sections.filter { it.id in profile.sectionTotals }, key = { it.id }) { section ->
                SoftCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(Scenery.forSection(section.id).res), null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(Shapes.tile))
                        Column(Modifier.weight(1f).padding(Space.sm)) {
                            Text(section.title, color = Ink, style = MaterialTheme.typography.titleSmall)
                            Text(when { section.id in profile.masteredSections -> "Mastered"
                                section.id !in profile.unlockedSections -> "Locked"
                                else -> "${profile.sections[section.id] ?: 0} / ${profile.sectionTotals[section.id] ?: 0} lessons" }, color = Muted)
                        }
                    }
                }
            }
            item { Text("Only your display name and game stats are public. Personal details stay private.", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
