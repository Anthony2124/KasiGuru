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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kasiguru.ui.components.AnnouncementBanner
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
import com.kasiguru.ui.components.clay.rememberStoryCoverRes
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.DayMark
import com.kasiguru.ui.components.clay.DayState
import com.kasiguru.ui.components.clay.ProgressRing
import com.kasiguru.ui.components.clay.SectionCaption
import com.kasiguru.ui.components.clay.SectionHeading
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
import com.kasiguru.ui.screens.learn.tree.learningPath
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.WidthClass
import com.kasiguru.ui.theme.rememberWidthClass

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

    // Today's Path and the day goal are a snapshot taken when this screen was built, and all the
    // work that changes them -- a review session, a lesson, a game -- happens on another screen.
    // Without this the learner clears their whole review deck and comes back to a card that still
    // says five words are due, and a goal ring that cannot turn green until the app is restarted.
    // The lifecycle owner inside a NavHost is the back-stack entry, so this fires on return to the
    // tab rather than only on Activity resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPlan() }

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
            streakQuota = uiState.streakQuota,
            onDismiss = { showStreakDialog = false }
        )
    }

    // Steps down one Space tier at a wider width, same spirit as the skills grid below going
    // from 2 to 4 columns - more width means the canopy doesn't need to claim as much height to
    // read as a real "who you are and where you stand today" band.
    val widthClass = rememberWidthClass()
    // Was 244dp. The canopy is pinned, so every dp of it is spent on every screenful the learner
    // ever scrolls -- roughly a quarter of the screen, to say hello and show a week that on day one
    // is empty. It now carries only who you are and today's streak; the week strip moved into the
    // sheet, where it scrolls away like the history it is.
    val canopyHeight = when (widthClass) {
        WidthClass.COMPACT -> 168.dp
        WidthClass.MEDIUM -> 160.dp
        WidthClass.EXPANDED -> 152.dp
    }

    CanopyScaffold(
        canopyHeight = canopyHeight,
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
                        "${progress.dailyGoalXp} XP earned today, ${uiState.dailyGoalRemainder}",
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

                items(uiState.announcements, key = { it.id }) { announcement ->
                    AnnouncementBanner(announcement = announcement)
                    Spacer(Modifier.height(Space.md))
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

                    // One action, not three. The screen used to answer "what do I do now?" with a
                    // Continue button, then a list of four cards, then the path below -- so none of
                    // them read as the answer. The card is now the only call to action above the
                    // path, and it leads with a Kasiguranin word rather than a verb.
                    uiState.continueCard?.let { card ->
                        ContinueCard(
                            card = card,
                            onClick = { onStartLesson(card.lessonRef.unitId, card.lessonRef.lessonIndex) }
                        )
                    } ?: ClayButton(
                        label = label,
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    // Due review is the one thing urgent enough to sit above the path: a word due
                    // today is a word about to be forgotten, which outranks meeting a new one.
                    if (uiState.wordsDue > 0) {
                        Spacer(Modifier.height(Space.sm))
                        ActivityRow(
                            title = "Review",
                            subtitle = "${wordsToReview(uiState.wordsDue)} due today",
                            accent = SkyReview,
                            state = ActivityState.Current,
                            onClick = onOpenReview,
                            leading = {
                                Icon(
                                    painter = painterResource(id = Iconsax.Repeat),
                                    contentDescription = null,
                                    tint = SkyReview,
                                    modifier = Modifier.size(20.dp).align(Alignment.Center)
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(Space.lg))
                }

                // The learning tree, in place of the four skill tiles that used to sit here. The
                // tiles were a summary of progress; this is the thing progress is made of, and it
                // answers "what do I do next" with a single node instead of a percentage.
                learningPath(
                    sections = uiState.tree,
                    onOpenLesson = onStartLesson,
                    onOpenMastery = { /* Section mastery test: wired with the test itself. */ }
                )

                // Below the path: the things that are genuinely secondary. They used to sit above
                // it as four same-size cards, which made a game and a story look exactly as
                // important as the lesson the learner came to do.
                item {
                    Spacer(Modifier.height(Space.xl))
                    SectionHeading(text = "Also today")
                    Spacer(Modifier.height(Space.sm))

                    ActivityRow(
                        title = "Practice game",
                        subtitle = "Earn stars and XP",
                        accent = Coral,
                        state = ActivityState.Upcoming,
                        onClick = onOpenGames,
                        leading = {
                            Icon(
                                painter = painterResource(id = Iconsax.Game),
                                contentDescription = null,
                                tint = Coral,
                                modifier = Modifier.size(20.dp).align(Alignment.Center)
                            )
                        }
                    )

                    if (uiState.wordsDue == 0) {
                        Spacer(Modifier.height(Space.sm))
                        ActivityRow(
                            title = "Review",
                            subtitle = "Nothing due today",
                            accent = SkyReview,
                            state = ActivityState.Done,
                            onClick = onOpenReview,
                            leading = {
                                Icon(
                                    painter = painterResource(id = Iconsax.Repeat),
                                    contentDescription = null,
                                    tint = SkyReview,
                                    modifier = Modifier.size(20.dp).align(Alignment.Center)
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(Space.xl))

                    // The week, where history belongs: below the work, not pinned above it.
                    SectionHeading(text = "This week")
                    Spacer(Modifier.height(Space.sm))
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
                        onCanopy = false
                    )
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
