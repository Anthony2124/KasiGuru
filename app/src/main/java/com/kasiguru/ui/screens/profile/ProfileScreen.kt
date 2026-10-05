package com.kasiguru.ui.screens.profile

import com.kasiguru.util.Constants
import com.kasiguru.ui.theme.RedText
import com.kasiguru.ui.theme.LimeText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import com.kasiguru.ui.theme.Scenery
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.screens.leaderboard.LeaderboardViewModel
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kasiguru.domain.gamification.BadgeFamilySummary
import com.kasiguru.domain.gamification.BadgeSummary
import com.kasiguru.domain.gamification.BadgeTier
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.StandardBadgeMedal
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepAvatarPortrait
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
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
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Info
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor
import com.kasiguru.util.gamification.GamificationEngine

/**
 * Me: who you are and what you have earned. The old Profile and Progress tabs, merged.
 *
 * Reads top to bottom as identity, then record, then the ways out: the avatar standing on the scenery
 * inside its XP ring, with the level spelled out under the name; streak, XP and words on one card;
 * the latest badges; a two-column overview; and finally the rows for settings, the account and the
 * rest of the app. The
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
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToStreak: () -> Unit = {},
    leaderboardViewModel: LeaderboardViewModel = hiltViewModel(),
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val leaderboard by leaderboardViewModel.uiState.collectAsState()
    var showBackgrounds by rememberSaveable { mutableStateOf(false) }
    val progress = uiState.userProgress ?: com.kasiguru.data.local.entity.UserProgressEntity()
    val displayName = progress.fullName.ifBlank { progress.userName }
    val isGuest = !uiState.account.isRecoverable
    if (showBackgrounds) ProfileBackgroundPicker(progress, onSelect = {
        viewModel.selectBackground(it); showBackgrounds = false
    }, onDismiss = { showBackgrounds = false })


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
                    ProfileHero(
                        backgroundRes = Scenery.forProfile(progress.profileBackgroundId).res,
                        avatar = JepjepAvatar.fromId(progress.profileIconId),
                        level = levelInfo.level,
                        levelFraction = levelFraction,
                        ringDescription = "Level ${levelInfo.level}, " +
                            (nextLevel?.let { "${(it.minXp - progress.totalXp).coerceAtLeast(0)} XP to level ${it.level}" }
                                ?: "highest rank reached"),
                        onChangeBackground = { showBackgrounds = true },
                        onEditProfile = onNavigateToEditProfile
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(top = Space.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(Space.xxs))
                        // What the ring around the avatar measures, in words.
                        Text(
                            text = nextLevel?.let {
                                val inLevel = (progress.totalXp - levelInfo.minXp).coerceAtLeast(0)
                                val span = (it.minXp - levelInfo.minXp).coerceAtLeast(1)
                                "${levelInfo.title} · $inLevel / $span XP"
                            } ?: "${levelInfo.title} · highest rank reached",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandLime,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(Space.sm))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stored for every learner since onboarding; an earned title, so it shows.
                            if (progress.titleBadge.isNotBlank()) TagChip(label = progress.titleBadge)
                            RankChip(rank = leaderboard.currentUserRank, onClick = onNavigateToLeaderboard)
                        }
                    }
                }

                // A guest's progress lives only on this phone: said once, near the top, calmly.
                if (isGuest) item(key = "guest") { GuestBanner(onSignIn = onNavigateToAccount) }

                // ── Record: three figures on one card ──
                item(key = "stats") {
                    StatsCard(
                        streak = progress.currentStreak,
                        totalXp = progress.totalXp,
                        // Words practised: the lifetime tally, which only rises. Mastered - the
                        // honest retention figure, which can fall - is in the overview below.
                        wordsPractised = progress.wordsLearned,
                        onOpenStreak = onNavigateToStreak
                    )
                }

                // ── Badges ──
                item(key = "badges") {
                    val pins = uiState.userProgress?.pinnedBadgeIds.orEmpty().split(',')
                    val families = remember(uiState.achievements, pins) {
                        BadgeSummary.families(uiState.achievements, pins)
                    }
                    BadgesSection(
                        unlocked = uiState.unlockedCount,
                        total = uiState.achievements.size,
                        families = families,
                        nextUp = remember(families) { BadgeSummary.nextUp(families) },
                        onSeeAll = onNavigateToAchievements
                    )
                }

                // ── Overview: always open, two columns ──
                item(key = "overview") {
                    SectionHeading(text = "Learning overview")
                    Spacer(Modifier.height(Space.sm))
                    // Two different, both-honest numbers. "Mastered" counts words that currently
                    // satisfy the SM-2 bar and can fall when one lapses; "practised" is the lifetime
                    // tally and only rises.
                    val facts = listOf(
                        Triple(Iconsax.BookBold, "${uiState.masteredCount}", "words mastered"),
                        Triple(Iconsax.Medal, "${progress.longestStreak}", if (progress.longestStreak == 1) "day longest streak" else "days longest streak"),
                        Triple(Iconsax.Teacher, "${uiState.lessonsCompleted}", "lessons completed"),
                        Triple(Iconsax.Game, "${progress.gamesPlayed}", "games played"),
                        // Stories are not narrated yet; until they are, this slot counts the words met.
                        if (Constants.STORIES_ENABLED) Triple(Iconsax.Book, "${progress.storiesCompleted}", "stories read")
                        else Triple(Iconsax.Book, "${uiState.wordsMet}", if (uiState.wordsMet == 1) "word met" else "words met"),
                        Triple(
                            Iconsax.TickCircle,
                            if (progress.totalQuestionsAnswered == 0) "-" else "${(uiState.accuracy * 100).toInt()}%",
                            if (progress.totalQuestionsAnswered == 0) "accuracy, not measured yet" else "accuracy"
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        facts.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                                pair.forEach { (icon, value, label) ->
                                    OverviewTile(icon, value, label, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (uiState.error != null) item(key = "error") { Text(uiState.error.orEmpty(), color = com.kasiguru.ui.theme.RedText) }

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

/** "#12 this week", or the way to the leaderboard before there is a rank. Opens the leaderboard. */
@Composable
private fun RankChip(rank: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(Shapes.pill)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.pill)
            .clickable(onClickLabel = "Open the leaderboard", onClick = onClick)
            .padding(horizontal = Space.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Iconsax.CupBold),
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(Space.xxs))
        Text(
            text = if (rank > 0) "#$rank this week" else "Leaderboard",
            style = MaterialTheme.typography.labelMedium,
            color = Ink
        )
    }
}

/**
 * The one prompt a guest sees: calm, and specific about the risk. Jepjep looks worried because an
 * anonymous account really is lost with the phone - not to alarm, just to say so.
 */
@Composable
private fun GuestBanner(onSignIn: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), border = BorderHairline, shape = Shapes.tile, contentPadding = PaddingValues(Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Iconsax.Lock), null, tint = BrandLime, modifier = Modifier.size(24.dp))
            Text("Save your progress with an account", color = Ink, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(horizontal = Space.sm))
            TextButton(onClick = onSignIn) { Text("Sign in", color = BrandLime) }
        }
    }
}

/**
 * The scenery banner with the avatar standing on its lower edge inside the XP ring.
 *
 * The ring sits on a solid Ground disc. Drawn straight over the scenery its lime arc vanished into
 * the green forest and its track into the night below, which is why the XP ring looked missing.
 */
@Composable
private fun ProfileHero(
    backgroundRes: Int,
    avatar: JepjepAvatar,
    level: Int,
    levelFraction: Float,
    ringDescription: String,
    onChangeBackground: () -> Unit,
    onEditProfile: () -> Unit
) {
    val bannerHeight = 156.dp
    val ringSize = 132.dp
    Box(Modifier.fillMaxWidth().height(bannerHeight + ringSize / 2)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .clip(Shapes.panel)
        ) {
            Image(
                painterResource(backgroundRes), null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = .30f),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = .25f)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Space.sm)
                    .tourAnchor(TourAnchor.ProfileBackground)
                    .clip(Shapes.pill)
                    .background(Color.Black.copy(alpha = .55f))
                    .clickable(onClickLabel = "Change background", onClick = onChangeBackground)
                    .padding(horizontal = Space.sm, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(Iconsax.Edit), null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Space.xxs))
                Text("Background", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(ringSize)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(com.kasiguru.ui.theme.Ground),
            contentAlignment = Alignment.Center
        ) {
            ProgressRing(
                progress = levelFraction,
                size = ringSize - 8.dp,
                strokeWidth = 8.dp,
                color = Lime,
                contentDescription = ringDescription
            ) {
                JepjepAvatarPortrait(
                    avatar = avatar,
                    size = ringSize - 32.dp,
                    level = level,
                    contentDescription = "Your avatar. Edit profile",
                    onClick = onEditProfile
                )
            }
        }
    }
}

/** Streak, XP and words as three columns on one card, split by hairlines. The streak opens its page. */
@Composable
private fun StatsCard(streak: Int, totalXp: Int, wordsPractised: Int, onOpenStreak: () -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        contentPadding = PaddingValues(vertical = Space.sm)
    ) {
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            StatColumn(Iconsax.FlashBold, Coral, "$streak", "day streak",
                Modifier.weight(1f).clickable(onClickLabel = "Open your streak", onClick = onOpenStreak))
            StatDivider()
            StatColumn(Iconsax.StarBold, Gold, "$totalXp", "total XP", Modifier.weight(1f))
            StatDivider()
            StatColumn(Iconsax.BookBold, BrandLime, "$wordsPractised", "words practised", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = Space.xs)
            .background(BorderHairline)
    )
}

/** A figure with its icon and unit. The colour tints the icon only; the words carry the meaning. */
@Composable
private fun StatColumn(iconRes: Int, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = Space.xs)
            .clearAndSetSemantics { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(id = iconRes), null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.xxs))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Ink, maxLines = 1)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** One fact in the overview grid: a tinted icon, the figure, and what it counts. */
@Composable
private fun OverviewTile(iconRes: Int, value: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Shapes.tile)
            .background(com.kasiguru.ui.theme.Surface)
            .border(1.dp, BorderHairline, Shapes.tile)
            .padding(Space.sm)
            .clearAndSetSemantics { contentDescription = "$value $label" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(Shapes.chip).background(com.kasiguru.ui.theme.LimeTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = BrandLime, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 2)
        }
    }
}

/** Badges on the grid, four to a row: enough width for a two-line name at the largest text size. */
private const val BadgeColumns = 4

/**
 * Every badge at once: one medal per family at its highest tier, a six-step track under it, and the
 * tier being worked on called out above. The old strip showed only the latest few earned tiers in a
 * side scroller, so what was still to earn - most of the set - never appeared on Profile at all.
 */
@Composable
private fun BadgesSection(
    unlocked: Int,
    total: Int,
    families: List<BadgeFamilySummary>,
    nextUp: BadgeFamilySummary?,
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
                    Text("See all", color = LimeText, style = MaterialTheme.typography.labelMedium)
                }
            }
        )
        Text(
            text = if (unlocked == 0) "No badges yet. Finish a lesson to earn your first."
            else "$unlocked of $total tiers earned",
            style = MaterialTheme.typography.bodySmall,
            color = Faint
        )
        Spacer(Modifier.height(Space.sm))

        nextUp?.let { summary ->
            val next = summary.next ?: return@let
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                border = BorderHairline,
                shape = Shapes.tile,
                onClick = onSeeAll
            ) {
                Text(
                    text = "NEXT UP",
                    style = MaterialTheme.typography.labelSmall,
                    color = LimeText
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = listOfNotNull(summary.family.name, summary.nextTier?.label).joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink
                )
                Spacer(Modifier.height(Space.xs))
                KasiGuruProgressBar(
                    progress = summary.progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 6.dp
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = summary.family.progress(next.currentValue, next.requiredValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = Faint
                )
            }
            Spacer(Modifier.height(Space.md))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            families.chunked(BadgeColumns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    row.forEach { summary ->
                        BadgeGridCell(summary, onClick = onSeeAll, modifier = Modifier.weight(1f))
                    }
                    // Keep a short last row on the same column grid.
                    repeat(BadgeColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BadgeGridCell(summary: BadgeFamilySummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val earned = summary.highest != null
    val tierText = summary.highest?.label ?: "Locked"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(Shapes.tile)
            .clickable(onClick = onClick)
            .padding(vertical = Space.xxs)
            .clearAndSetSemantics {
                contentDescription = "${summary.family.name}, $tierText, ${summary.earnedTiers} of 6 tiers"
            }
    ) {
        StandardBadgeMedal(
            tier = summary.highest,
            earned = earned,
            size = 52.dp,
            familyId = summary.family.id
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = summary.family.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (earned) Ink else Muted,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.xxs))
        TierTrack(earnedTiers = summary.earnedTiers)
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = tierText,
            style = MaterialTheme.typography.labelSmall,
            color = if (earned) LimeText else Faint,
            maxLines = 1
        )
    }
}

/** Six dots, one per tier: how far through the family this badge is. */
@Composable
private fun TierTrack(earnedTiers: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(BadgeTier.entries.size) { i ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(Shapes.pill)
                    .background(if (i < earnedTiers) Lime else BorderHairline)
            )
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
