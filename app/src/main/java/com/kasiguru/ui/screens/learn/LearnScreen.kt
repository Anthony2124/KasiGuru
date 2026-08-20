package com.kasiguru.ui.screens.learn

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.AppUpdateBanner
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.SecureProgressBanner
import com.kasiguru.ui.components.StreakDialog
import com.kasiguru.ui.components.clay.ActivityRow
import com.kasiguru.ui.components.clay.ActivityState
import com.kasiguru.ui.components.clay.CanopyIconButton
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import com.kasiguru.ui.components.clay.StoryCoverCard
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.DayMark
import com.kasiguru.ui.components.clay.DayState
import com.kasiguru.ui.components.clay.ProgressRing
import com.kasiguru.ui.components.clay.SectionCaption
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SkillTile
import com.kasiguru.ui.components.clay.TimelineItem
import com.kasiguru.ui.components.clay.WeekStrip
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.SkyReview
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet

/**
 * The learner's home: a violet canopy carrying today's state, over a sheet carrying today's work.
 *
 * This replaces the old Home screen, which was a link farm — four of its cards duplicated four
 * bottom-nav tabs, and its "Learning Journey Map" was four hardcoded nodes that all navigated to the
 * dictionary. Everything shown here is derived from real progress.
 */
@Composable
fun LearnScreen(
    onStartLesson: (unitId: String, lessonIndex: Int) -> Unit,
    onOpenReview: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenStories: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAccount: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStreakDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    val progress = uiState.progress
    val displayName = progress.fullName.ifBlank { progress.userName }

    if (showStreakDialog) {
        StreakDialog(
            currentStreak = progress.currentStreak,
            longestStreak = progress.longestStreak,
            onDismiss = { showStreakDialog = false }
        )
    }

    CanopyScaffold(
        canopyHeight = 244.dp,
        canopyContent = {
            Spacer(Modifier.height(Space.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                // A solid fill, not glass: a translucent chip's label fails contrast at this end of
                // the canopy gradient (DESIGN.md's translucent-chip rule), and Gold already carries
                // Ink at 9.00 measured, so a solid badge is both safer and closer to the streak
                // flame's original look.
                Row(
                    modifier = Modifier
                        .clip(Shapes.pill)
                        .background(Gold)
                        .clickable { showStreakDialog = true }
                        .padding(horizontal = Space.sm, vertical = Space.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xxs)
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.FlashBold),
                        contentDescription = "Streak",
                        tint = RewardInk,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${progress.currentStreak}",
                        style = MaterialTheme.typography.labelMedium,
                        color = RewardInk
                    )
                }
                Spacer(Modifier.width(Space.xs))
                CanopyIconButton(
                    iconRes = Iconsax.Notification,
                    contentDescription = "Notifications",
                    onClick = onOpenNotifications
                )
            }

            Spacer(Modifier.height(Space.xs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CasiguranAvatarPortrait(
                    resident = CasiguranResident.TEACHER,
                    size = 52.dp,
                    level = progress.level,
                    onClick = onOpenProfile
                )
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Magandang aldew,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnCanopy
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnCanopy,
                        maxLines = 1
                    )
                }

                // Daily goal. The ring is the one place the app states, honestly, how today is going.
                ProgressRing(
                    progress = uiState.dailyGoalFraction,
                    size = 64.dp,
                    strokeWidth = 7.dp,
                    color = if (uiState.dailyGoalMet) Green else Gold,
                    onCanopy = true,
                    contentDescription = "Daily goal: ${uiState.dailyXpEarned} of " +
                        "${progress.dailyGoalXp} XP earned today",
                    modifier = Modifier.clickable(onClick = onOpenProgress)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.dailyXpEarned}",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnCanopy
                        )
                        Text(
                            text = "XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnCanopy
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.md))

            WeekStrip(
                days = uiState.week.map { day ->
                    DayMark(
                        label = day.label,
                        dayOfMonth = day.dayOfMonth,
                        state = when {
                            day.isToday && day.practised -> DayState.TodayDone
                            day.isToday -> DayState.Today
                            day.practised -> DayState.Done
                            else -> DayState.Missed
                        }
                    )
                },
                onCanopy = true
            )
        },
        sheetContent = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter,
                    end = Space.gutter,
                    top = Space.lg,
                    bottom = Space.navBarClearance
                )
            ) {
                uiState.updateRelease?.let { release ->
                    item {
                        AppUpdateBanner(release = release, onDismiss = viewModel::dismissUpdate)
                        Spacer(Modifier.height(Space.md))
                    }
                }

                if (uiState.showBackupPrompt) {
                    item {
                        SecureProgressBanner(
                            onSecure = onOpenAccount,
                            onDismiss = viewModel::dismissBackupPrompt
                        )
                        Spacer(Modifier.height(Space.md))
                    }
                }

                // The app's most-pressed control, and what replaced the docked FAB.
                //
                // It sits here rather than in the navigation bar for three reasons: a full-width button
                // is entirely tappable, where the FAB lost roughly its top 12dp to overflowing the pill
                // it was docked into; it can carry a label, so the action names itself instead of
                // showing a bare glyph whose meaning changed underneath the user; and an action that
                // belongs to Learn should not follow the learner onto Words or Profile.
                item {
                    val next = uiState.currentActivity
                    val label: String
                    val iconRes: Int
                    val onContinue: () -> Unit
                    when (next?.kind) {
                        ActivityKind.Lesson -> {
                            label = "Continue learning"
                            iconRes = Iconsax.Play
                            onContinue = {
                                next.lessonRef
                                    ?.let { onStartLesson(it.unitId, it.lessonIndex) }
                                    ?: onOpenReview()
                            }
                        }
                        ActivityKind.Game -> {
                            label = "Play a game"
                            iconRes = Iconsax.Game
                            onContinue = onOpenGames
                        }
                        ActivityKind.Story -> {
                            label = "Read a story"
                            iconRes = Iconsax.BookBold
                            onContinue = onOpenStories
                        }
                        // Review, or today's path already finished. The deck always has something to
                        // show, so this is a real destination rather than a disabled state.
                        else -> {
                            label = "Review your words"
                            iconRes = Iconsax.Repeat
                            onContinue = onOpenReview
                        }
                    }

                    ClayButton(
                        label = label,
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null, // the label already names the action
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    Spacer(Modifier.height(Space.lg))
                }

                item {
                    SectionHeading(text = "Today")
                    SectionCaption(
                        text = if (progress.currentStreak > 0) {
                            "Day ${progress.currentStreak} of your streak"
                        } else {
                            "Finish one activity to start a streak"
                        }
                    )
                    Spacer(Modifier.height(Space.md))
                }

                itemsIndexed(uiState.activities) { index, activity ->
                    val state = when {
                        activity.isDone -> ActivityState.Done
                        uiState.currentActivity === activity -> ActivityState.Current
                        else -> ActivityState.Upcoming
                    }
                    TimelineItem(state = state, isLast = index == uiState.activities.lastIndex) {
                        ActivityRow(
                            title = activity.title,
                            subtitle = activity.subtitle,
                            accent = activity.kind.accent(),
                            state = state,
                            onClick = {
                                when (activity.kind) {
                                    ActivityKind.Lesson -> activity.lessonRef?.let {
                                        onStartLesson(it.unitId, it.lessonIndex)
                                    } ?: onOpenDictionary()
                                    ActivityKind.Review -> onOpenReview()
                                    ActivityKind.Game -> onOpenGames()
                                    ActivityKind.Story -> onOpenStories()
                                }
                            },
                            leading = {
                                Icon(
                                    painter = painterResource(id = activity.kind.iconRes()),
                                    contentDescription = null,
                                    tint = if (state == ActivityState.Locked) NodeLockedInk
                                    else activity.kind.accent(),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            trailing = {
                                if (activity.isDone) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.TickCircle),
                                        contentDescription = "Done",
                                        tint = Green,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = Iconsax.ArrowRight),
                                        contentDescription = null,
                                        tint = Muted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Story mode. Fully built since v1.2 and reachable only from a tile in the skills
                // grid, which is why it read as missing. A shelf of covers says what it is.
                if (uiState.stories.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(Space.lg))
                        SectionHeading(
                            text = "Story mode",
                            action = {
                                TextButton(onClick = onOpenStories) {
                                    Text(
                                        "All stories",
                                        color = Violet,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        )
                        SectionCaption(
                            text = uiState.stories.count { it.isCompleted }.toString() + " of " +
                                uiState.stories.size + " read, with Tagalog and English alongside"
                        )
                        Spacer(Modifier.height(Space.sm))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            items(uiState.stories, key = { it.id }) { story ->
                                StoryCoverCard(
                                    titleKasiguranin = story.titleKasiguranin,
                                    title = story.title,
                                    totalPages = story.totalPages,
                                    isUnlocked = story.isUnlocked,
                                    isCompleted = story.isCompleted,
                                    requiredXp = story.requiredXp,
                                    onClick = { onOpenStories() },
                                    modifier = Modifier.width(160.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(Space.lg))
                    SectionHeading(text = "Your skills")
                    SectionCaption(text = "Everything here is measured from real progress")
                    Spacer(Modifier.height(Space.md))
                }

                // Two rows of two. A LazyVerticalGrid inside a LazyColumn is not allowed, and four
                // fixed tiles do not need one.
                item {
                    val skills = uiState.skills
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        skills.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                                pair.forEach { skill ->
                                    SkillTile(
                                        title = skill.name,
                                        percent = skill.percent,
                                        accent = skill.kind.accent(),
                                        onClick = {
                                            when (skill.kind) {
                                                ActivityKind.Lesson -> onOpenDictionary()
                                                ActivityKind.Review -> onOpenReview()
                                                ActivityKind.Game -> onOpenGames()
                                                ActivityKind.Story -> onOpenStories()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        artwork = {
                                            // Adrian supplies the real illustrations. Until they
                                            // land the tile shows its own icon, so an empty slot
                                            // reads as restraint rather than as a missing asset.
                                            Icon(
                                                painter = painterResource(id = skill.kind.iconRes()),
                                                contentDescription = null,
                                                tint = skill.kind.accent(),
                                                modifier = Modifier.size(22.dp).align(Alignment.Center)
                                            )
                                        }
                                    )
                                }
                                // Keeps a lone tile at half width rather than letting it stretch.
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    )
}

/** Each activity kind owns one hue, so colour carries meaning across Learn, Practice and Progress. */
@Composable
private fun ActivityKind.accent(): Color = when (this) {
    ActivityKind.Lesson -> Violet
    ActivityKind.Review -> SkyReview
    ActivityKind.Game -> Coral
    ActivityKind.Story -> Gold
}

private fun ActivityKind.iconRes(): Int = when (this) {
    ActivityKind.Lesson -> Iconsax.Book
    ActivityKind.Review -> Iconsax.Repeat
    ActivityKind.Game -> Iconsax.Game
    ActivityKind.Story -> Iconsax.Document
}
