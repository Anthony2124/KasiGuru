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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.domain.gamification.BadgeCatalog
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepAvatarPortrait
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.components.clay.GroundIconButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.ProgressRing
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Info
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.TierBronze
import com.kasiguru.ui.theme.TierBronzeDeep
import com.kasiguru.ui.theme.TierSilver
import com.kasiguru.ui.theme.TierSilverDeep
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor
import com.kasiguru.util.gamification.GamificationEngine

/**
 * Me: who you are and what you have earned. The old Profile and Progress tabs, merged.
 *
 * Reads top to bottom as identity, then record, then the ways out: the avatar inside its level ring
 * with the rank it has earned; streak, XP and words as three figures; the latest badges with a way
 * into the whole wall; and finally the rows for settings, the account and the rest of the app. The
 * leaderboard lives on Practice, where XP is earned. A guest sees one calm prompt near the top, because an anonymous account is the one
 * thing here that can be lost.
 *
 * A tab root, so no back chevron. Settings and Edit sit in the bar.
 */
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToCultural: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = uiState.userProgress ?: com.kasiguru.data.local.entity.UserProgressEntity()
    val displayName = progress.fullName.ifBlank { progress.userName }
    val isGuest = !uiState.account.isRecoverable

    GroundScaffold(
        title = displayName,
        pattern = GroundPattern.None,
        actions = {
            GroundIconButton(
                Iconsax.Setting,
                "Settings",
                onNavigateToSettings,
                modifier = Modifier.tourAnchor(TourAnchor.ProfileSettingsIcon)
            )
            GroundIconButton(Iconsax.Edit, "Edit profile", onNavigateToEditProfile)
        },
        content = {
            if (uiState.isLoading) {
                LoadingState(label = "Loading your profile")
                return@GroundScaffold
            }

            val levelInfo = remember(progress.totalXp) { GamificationEngine.getLevelInfo(progress.totalXp) }
            val nextLevel = remember(levelInfo.level) { GamificationEngine.getNextLevelInfo(levelInfo.level) }
            val levelFraction = remember(progress.totalXp) { GamificationEngine.getXpProgressInLevel(progress.totalXp) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.lg)
            ) {
                // ── Identity ──
                item(key = "identity") {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressRing(
                            progress = levelFraction,
                            size = 120.dp,
                            strokeWidth = 6.dp,
                            color = BrandLime,
                            contentDescription = "Level ${progress.level}, " +
                                (nextLevel?.let { "${(it.minXp - progress.totalXp).coerceAtLeast(0)} XP to ${it.title}" }
                                    ?: "highest rank reached")
                        ) {
                            JepjepAvatarPortrait(
                                avatar = JepjepAvatar.fromId(progress.profileIconId),
                                size = 96.dp,
                                level = progress.level,
                                contentDescription = "Your avatar. Edit profile",
                                onClick = onNavigateToEditProfile
                            )
                        }
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = levelInfo.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandLime,
                            textAlign = TextAlign.Center
                        )
                        if (progress.titleBadge.isNotBlank()) {
                            Spacer(Modifier.height(Space.xs))
                            // Stored for every learner since onboarding; an earned title, so it shows.
                            TagChip(label = progress.titleBadge)
                        }
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = nextLevel?.let {
                                "${(it.minXp - progress.totalXp).coerceAtLeast(0)} XP to ${it.title}"
                            } ?: "Highest rank reached",
                            style = MaterialTheme.typography.bodySmall,
                            color = Faint
                        )
                    }
                }

                // ── Guest ──
                if (isGuest) {
                    item(key = "guest") { GuestBanner(onSignIn = onNavigateToAccount) }
                }

                // ── Record ──
                item(key = "stats") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        StatTile(
                            iconRes = Iconsax.FlashBold,
                            tint = Coral,
                            value = "${progress.currentStreak}",
                            label = "day streak",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            iconRes = Iconsax.StarBold,
                            tint = Gold,
                            value = "${progress.totalXp}",
                            label = "total XP",
                            modifier = Modifier.weight(1f)
                        )
                        // Words practised: the lifetime tally, which only rises. Mastered - the
                        // honest retention figure, which can fall - is in the overview below.
                        StatTile(
                            iconRes = Iconsax.BookBold,
                            tint = BrandLime,
                            value = "${progress.wordsLearned}",
                            label = "words practised",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Badges ──
                item(key = "badges") {
                    BadgesSection(
                        unlocked = uiState.unlockedCount,
                        total = uiState.achievements.size,
                        recent = uiState.recentlyUnlocked,
                        closestLocked = uiState.closestLocked,
                        onSeeAll = onNavigateToAchievements
                    )
                }

                // ── Overview ──
                item(key = "overview") {
                    SectionHeading(text = "Learning overview")
                    Spacer(Modifier.height(Space.sm))
                    SoftCard(modifier = Modifier.fillMaxWidth(), border = BorderHairline, shape = Shapes.tile) {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            // Two different, both-honest numbers. "Mastered" counts words that
                            // currently satisfy the SM-2 bar and can fall when one lapses;
                            // "practised" is the lifetime tally and only rises.
                            StatDetailRow("Words mastered", "${uiState.masteredCount}", Iconsax.BookBold)
                            StatDetailRow("Longest streak", "${progress.longestStreak} ${if (progress.longestStreak == 1) "day" else "days"}", Iconsax.Medal)
                            StatDetailRow("Lessons completed", "${uiState.lessonsCompleted}", Iconsax.Teacher)
                            StatDetailRow("Games played", "${progress.gamesPlayed}", Iconsax.Game)
                            StatDetailRow("Stories read", "${progress.storiesCompleted}", Iconsax.Book)
                            StatDetailRow(
                                "Accuracy",
                                if (progress.totalQuestionsAnswered == 0) "Not measured yet"
                                else "${(uiState.accuracy * 100).toInt()}%",
                                Iconsax.TickCircle
                            )
                        }
                    }
                }

                // ── Settings and account ──
                item(key = "settings") {
                    SectionHeading(text = "Settings and account")
                    Spacer(Modifier.height(Space.sm))
                    RowGroup {
                        LinkRow(Iconsax.Setting, Lime, "Settings", "Reminders, sound and more", onNavigateToSettings)
                        GroupDivider()
                        LinkRow(
                            Iconsax.Lock,
                            if (isGuest) Coral else Lime,
                            "Account",
                            if (isGuest) "Guest - progress is only on this phone"
                            else progress.email.ifEmpty { uiState.account.email ?: "Signed in" },
                            onNavigateToAccount
                        )
                        GroupDivider()
                        LinkRow(Iconsax.Edit, Lime, "Edit profile", "Name, avatar and details", onNavigateToEditProfile)
                    }
                }

                // ── Explore ──
                item(key = "explore") {
                    SectionHeading(text = "Explore")
                    Spacer(Modifier.height(Space.sm))
                    RowGroup(modifier = Modifier.tourAnchor(TourAnchor.ProfileExplore)) {
                        LinkRow(Iconsax.Teacher, Info, "How to use KasiGuru", "A short guide to every tab", onNavigateToHelp)
                        GroupDivider()
                        LinkRow(Iconsax.Courthouse, Coral, "Cultural heritage", "Stories and context from Casiguran", onNavigateToCultural)
                        GroupDivider()
                        LinkRow(Iconsax.InfoCircle, Gold, "About KasiGuru", "The project, the team, the mission", onNavigateToAbout)
                    }
                }
            }
        }
    )
}

/**
 * The one prompt a guest sees: calm, and specific about the risk. Jepjep looks worried because an
 * anonymous account really is lost with the phone - not to alarm, just to say so.
 */
@Composable
private fun GuestBanner(onSignIn: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), border = BorderHairline, shape = Shapes.panel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Jepjep(pose = JepjepPose.Worried, height = 84.dp)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Sign in so you don't lose your progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = "Right now your XP, streak and badges live only on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
        Spacer(Modifier.height(Space.md))
        // The one lime action on Me: for a guest, nothing here matters more.
        ClayButton(label = "Sign in", onClick = onSignIn, modifier = Modifier.fillMaxWidth())
    }
}

/** A figure with its icon and unit. The colour tints the icon only; the words carry the meaning. */
@Composable
private fun StatTile(iconRes: Int, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$value $label" },
        shape = Shapes.tile,
        border = BorderHairline,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(Space.xxs))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Ink, maxLines = 1)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** The latest badges, with the way into all of them. A learner with none is told which is closest. */
@Composable
private fun BadgesSection(
    unlocked: Int,
    total: Int,
    recent: List<AchievementEntity>,
    closestLocked: AchievementEntity?,
    onSeeAll: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeading(
            text = "Badges",
            action = {
                TextButton(
                    onClick = onSeeAll,
                    modifier = Modifier.semantics { contentDescription = "See all badges" }
                ) {
                    Text("See all", color = Lime, style = MaterialTheme.typography.labelMedium)
                }
            }
        )
        Text(
            text = "$unlocked of $total tiers earned",
            style = MaterialTheme.typography.bodySmall,
            color = Faint
        )
        Spacer(Modifier.height(Space.sm))

        if (recent.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                items(recent, key = { it.id }) { badge ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(76.dp)
                            .clickable(onClick = onSeeAll)
                    ) {
                        val (face, lip) = when (badge.tier) {
                            "silver" -> TierSilver to TierSilverDeep
                            "bronze" -> TierBronze to TierBronzeDeep
                            else -> Gold to GoldDeep
                        }
                        ClayCircle(face = face, lipColor = lip, size = 56.dp) {
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
                        BadgeCatalog.tierFor(badge.id)?.let {
                            Text(it.label,style = MaterialTheme.typography.labelSmall,color = Faint)
                        }
                    }
                }
            }
        } else {
            // A designed empty state: name the nearest badge and how close it is, rather than a blank
            // row that reads as a loading failure.
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                border = BorderHairline,
                shape = Shapes.tile,
                onClick = onSeeAll
            ) {
                Text(
                    text = "No badges yet. Finish a lesson to earn your first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink
                )
                closestLocked?.let { next ->
                    Spacer(Modifier.height(Space.sm))
                    Text(text = "Closest: ${next.name}", style = MaterialTheme.typography.labelLarge, color = Muted)
                    Spacer(Modifier.height(Space.xxs))
                    KasiGuruProgressBar(
                        progress = if (next.requiredValue == 0) 0f
                        else next.currentValue.toFloat() / next.requiredValue,
                        modifier = Modifier.fillMaxWidth(),
                        height = 6.dp
                    )
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        text = "${next.currentValue} of ${next.requiredValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Faint
                    )
                }
            }
        }
    }
}

/** A grouped list of rows on one card, as the settings-style groups in the design. */
@Composable
private fun RowGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        contentPadding = PaddingValues(vertical = Space.xxs)
    ) {
        content()
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Space.md),
        thickness = 1.dp,
        color = BorderHairline
    )
}

/** A row with a tinted icon tile, a title and a line of detail. [onClick] null when the parent is the target. */
@Composable
private fun LinkRow(iconRes: Int, accent: Color, title: String, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.minTarget)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(painter = painterResource(id = Iconsax.ArrowRight), contentDescription = null, tint = Faint, modifier = Modifier.size(16.dp))
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
