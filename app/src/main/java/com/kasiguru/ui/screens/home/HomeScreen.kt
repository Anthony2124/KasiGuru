package com.kasiguru.ui.screens.home

import com.kasiguru.ui.theme.GreenText
import com.kasiguru.ui.theme.LimeText
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.ui.components.AnnouncementBanner
import com.kasiguru.ui.components.AppUpdateBanner
import com.kasiguru.ui.components.SecureProgressBanner
import com.kasiguru.ui.components.StreakDialog
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepAvatarPortrait
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.DayMark
import com.kasiguru.ui.components.clay.DayState
import com.kasiguru.ui.components.clay.ProgressRing
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.StoryCoverCard
import com.kasiguru.ui.components.clay.WeekStrip
import com.kasiguru.ui.components.clay.glowBackground
import com.kasiguru.ui.components.clay.rememberStoryCoverRes
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.screens.learn.ActivityKind
import com.kasiguru.ui.screens.learn.ContinueCard
import com.kasiguru.ui.screens.learn.LearnUiState
import com.kasiguru.ui.screens.learn.LearnViewModel
import com.kasiguru.ui.screens.learn.wordsToReview
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Info
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.StatusBarIcons
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.WidthClass
import com.kasiguru.ui.theme.rememberWidthClass
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.TourRevealInScroll
import com.kasiguru.ui.tour.tourAnchor

/**
 * Home: the one thing to do next, and how today is going.
 *
 * Split out of the old Learn screen, which carried both today's plan and the whole learning path and
 * so answered "what now?" three times over. Home answers it once - with the single lime action - and
 * everything under it is context: the day goal, what is due for review, a shelf of stories, and the
 * week. The path itself lives on the Learn tab.
 *
 * Backed by [LearnViewModel], which already derives all of this from real progress; nothing here is
 * computed twice. Jepjep's line is picked from the same state, so it only ever says something true.
 */
@Composable
fun HomeScreen(
    onStartLesson: (unitId: String, lessonIndex: Int) -> Unit,
    onOpenReview: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenStories: () -> Unit,
    /** Opens one story straight from the shelf, without the detour through the list. */
    onOpenStory: (storyId: Int) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenStreak: () -> Unit,
    onOpenWord: (Int) -> Unit,
    onOpenGame: (String) -> Unit,
    viewModel: LearnViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Today's plan and the day goal are a snapshot taken when this screen was built, and all the work
    // that changes them -- a review session, a lesson, a game -- happens on another screen. Without
    // this the learner clears their whole review deck and comes back to a card that still says five
    // words are due. The lifecycle owner inside a NavHost is the back-stack entry, so this fires on
    // return to the tab rather than only on Activity resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPlan() }

    StatusBarIcons()

    if (uiState.isLoading) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ground)
                .statusBarsPadding()
        ) {
            LoadingState(label = "Getting today ready")
        }
        return
    }

    val progress = uiState.progress

    val scroll = rememberScrollState()
    // Every Home anchor lives in this one column, so the tour can always scroll its target back.
    TourRevealInScroll(scroll)

    val wide = rememberWidthClass() != WidthClass.COMPACT

    val today: @Composable () -> Unit = {
        HomeHero(
            progress = progress,
            line = jepjepLine(uiState),
            onOpenProfile = onOpenProfile,
            onOpenNotifications = onOpenNotifications,
            onOpenStreak = onOpenStreak
        )
        Spacer(Modifier.height(Space.md))
        PrimaryAction(
            uiState = uiState,
            onStartLesson = onStartLesson,
            onOpenReview = onOpenReview,
            onOpenGames = onOpenGames,
            onOpenStories = onOpenStories
        )

        Spacer(Modifier.height(Space.md))
        TodayTiles(uiState = uiState, onOpenProgress = onOpenProgress, onOpenReview = onOpenReview)
    }

    val later: @Composable () -> Unit = {
        SectionHeading(text = "Quick practice")
        Spacer(Modifier.height(Space.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            val game = uiState.lastGameType?.takeIf { it in setOf("word_match", "word_search", "word_wheel") }
            val gameName = game?.substringBefore(":")?.split("_")?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) } ?: "Games"
            listOf(Triple("Flashcards", Iconsax.Repeat, onOpenReview),
                Triple(gameName, Iconsax.Game, { if (game == null) onOpenGames() else onOpenGame(game) }),
                Triple("Story", Iconsax.BookBold, { uiState.stories.firstOrNull { it.isUnlocked }?.let { onOpenStory(it.id) } ?: onOpenStories() })
            ).forEach { (label, icon, action) ->
                SoftCard(modifier = Modifier.weight(1f), shape = Shapes.tile, onClick = action,
                    contentPadding = PaddingValues(horizontal = Space.xs, vertical = Space.sm)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(icon), null, tint = BrandLime, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(Space.xs))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        uiState.wordOfDay?.let { word ->
            Spacer(Modifier.height(Space.lg))
            SectionHeading(text = "Word of the day")
            Spacer(Modifier.height(Space.sm))
            SoftCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenWord(word.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(word.kasiguranin, style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("${word.english} · ${word.tagalog}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                    Jepjep(JepjepPose.Curious, height = 84.dp)
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        uiState.announcements.forEach { announcement ->
            Spacer(Modifier.height(Space.md))
            AnnouncementBanner(announcement = announcement, collapsed = true)
        }
        if (uiState.showBackupPrompt) {
            Spacer(Modifier.height(Space.md))
            SecureProgressBanner(onSecure = onOpenAccount, onDismiss = viewModel::dismissBackupPrompt, collapsed = true)
        }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .verticalScroll(scroll)
            .statusBarsPadding()
            .padding(horizontal = Space.gutter)
            .padding(top = Space.sm, bottom = Space.navBarClearance)
    ) {
        // Rare, and when it is a forced update it outranks everything else on the screen.
        uiState.updateRelease?.let { release ->
            AppUpdateBanner(release = release, onDismiss = viewModel::dismissUpdate)
            Spacer(Modifier.height(Space.md))
        }

        if (wide) {
            // Two columns from 600dp: the next action and today on the left, stories and the week on
            // the right, so a tablet shows the whole day without scrolling.
            Row(horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                Column(Modifier.weight(1f)) { today() }
                Column(Modifier.weight(1f)) { later() }
            }
        } else {
            today()
            Spacer(Modifier.height(Space.xl))
            later()
        }
    }
}

// ── Jepjep's line ───────────────────────────────────────────────────────────────

/** What Jepjep says on Home, and the pose that means it. */
private data class JepjepLine(val pose: JepjepPose, val text: String)

/**
 * One short line, chosen from real state in order of what cannot wait.
 *
 * A streak about to lapse comes first because it is the only thing on the screen that cannot be made
 * up tomorrow; words due come next because a word due today is a word about to be forgotten. The
 * Speaking pose's bubble is drawn empty, so the words always sit beside him, never inside it.
 */
private fun jepjepLine(state: LearnUiState): JepjepLine {
    val progress = state.progress
    return when {
        state.streakAtRisk -> JepjepLine(
            JepjepPose.Worried,
            "Keep your ${progress.currentStreak}-day streak alive today."
        )
        state.wordsDue > 0 -> JepjepLine(
            JepjepPose.Speaking,
            if (state.wordsDue == 1) "1 word is due for review." else "${state.wordsDue} words are due for review."
        )
        state.dailyGoalMet -> JepjepLine(
            JepjepPose.Celebrating,
            "All done for today. See you tomorrow!"
        )
        progress.totalXp == 0 -> JepjepLine(
            JepjepPose.Waving,
            "Let's learn your first Kasiguranin words."
        )
        else -> JepjepLine(JepjepPose.Waving, "Ready for today's lesson?")
    }
}

// ── Hero ────────────────────────────────────────────────────────────────────────

/**
 * Who you are and how today stands, with Jepjep saying the one thing worth saying.
 *
 * The panel carries the soft glow, centred behind Jepjep, because this greeting is the one moment on
 * an everyday screen where he is the point. Everything else on Home is flat night.
 */
@Composable
private fun HomeHero(
    progress: UserProgressEntity,
    line: JepjepLine,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenStreak: () -> Unit
) {
    val displayName = progress.fullName.ifBlank { progress.userName }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.panel)
            .glowBackground(centerX = 0.2f, centerY = 0.8f)
            .border(1.dp, BorderHairline, Shapes.panel)
            .padding(Space.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JepjepAvatarPortrait(
                avatar = JepjepAvatar.fromId(progress.profileIconId),
                size = 56.dp,
                level = progress.level,
                contentDescription = "Your profile, level ${progress.level}",
                onClick = onOpenProfile
            )
            Spacer(Modifier.width(Space.sm))
            // The name is the highlighted word in the heading, as on the onboarding's "Nice to meet
            // you" screen.
            Text(
                text = buildAnnotatedString {
                    append("Magandang aldew, ")
                    withStyle(SpanStyle(color = BrandLime)) { append(displayName) }
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Space.xs))
            Box(
                modifier = Modifier
                    .tourAnchor(TourAnchor.NotificationBell)
                    .size(Touch.minTarget)
                    .clip(CircleShape)
                    .background(Surface)
                    .border(1.dp, BorderHairline, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onOpenNotifications
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Notification),
                    contentDescription = "Notifications",
                    tint = Ink,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(Space.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            HeroChip(
                iconRes = Iconsax.FlashBold,
                tint = Coral,
                text = if (progress.currentStreak == 1) "1 day streak" else "${progress.currentStreak} day streak",
                spoken = "Streak, ${progress.currentStreak} ${if (progress.currentStreak == 1) "day" else "days"}. " +
                    "Shows what keeps it going.",
                onClick = onOpenStreak,
                modifier = Modifier.tourAnchor(TourAnchor.StreakBadge)
            )
            HeroChip(
                iconRes = Iconsax.StarBold,
                tint = Gold,
                text = "${progress.totalXp} XP",
                spoken = "${progress.totalXp} XP in total"
            )
        }


    }
}

/**
 * A streak or XP figure. Status colour only ever tints the icon; the number and its unit are words,
 * so neither depends on telling orange from gold.
 *
 * A tappable chip keeps a 48dp touch target around a 40dp pill, so the target is honest without the
 * chip looking like a button.
 */
@Composable
private fun HeroChip(
    iconRes: Int,
    tint: Color,
    text: String,
    spoken: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val touch = if (onClick != null) {
        Modifier
            .clip(Shapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    } else {
        Modifier.padding(vertical = 4.dp)
    }

    Row(
        modifier = modifier
            .then(touch)
            .heightIn(min = 40.dp)
            .clip(Shapes.pill)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.pill)
            .padding(horizontal = Space.sm)
            .clearAndSetSemantics {
                contentDescription = spoken
                if (onClick != null) role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(Space.xxs))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}

// ── The one action ──────────────────────────────────────────────────────────────

/**
 * The app's most-pressed control, and what replaced the docked FAB.
 *
 * With a next lesson it is [ContinueCard], led by that lesson's first Kasiguranin word; otherwise a
 * single button naming the first thing not yet done today. Both are the same job, so the tour's
 * anchor wraps whichever is showing.
 */
@Composable
private fun PrimaryAction(
    uiState: LearnUiState,
    onStartLesson: (String, Int) -> Unit,
    onOpenReview: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenStories: () -> Unit
) {
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
        // Review, or today's path already finished. The deck always has something to show, so this
        // is a real destination rather than a disabled state.
        else -> {
            label = "Review your words"
            iconRes = Iconsax.Repeat
            onContinue = onOpenReview
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tourAnchor(TourAnchor.ContinueAction)
    ) {
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
                    tint = OnLime,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

// ── Today ───────────────────────────────────────────────────────────────────────

/** The day goal and the review deck, side by side: the two things a day is measured by. */
@Composable
private fun TodayTiles(
    uiState: LearnUiState,
    onOpenProgress: () -> Unit,
    onOpenReview: () -> Unit
) {
    val goal = uiState.progress.dailyGoalXp
    val earned = uiState.dailyXpEarned

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        SoftCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = Shapes.tile,
            border = BorderHairline,
            onClick = onOpenProgress,
            contentPadding = PaddingValues(Space.md)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // The ring is the one place the app states, honestly, how today is going.
                ProgressRing(
                    progress = uiState.dailyGoalFraction,
                    size = 72.dp,
                    strokeWidth = 7.dp,
                    color = if (uiState.dailyGoalMet) Green else Gold,
                    contentDescription = "Daily goal: $earned of $goal XP earned today, " +
                        uiState.dailyGoalRemainder,
                    modifier = Modifier.tourAnchor(TourAnchor.DailyGoalRing)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$earned", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(text = "XP", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                }
                Spacer(Modifier.height(Space.sm))
                Text(text = "Daily goal", style = MaterialTheme.typography.titleSmall, color = Ink)
                if (uiState.dailyGoalMet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = Iconsax.TickCircle),
                            contentDescription = null,
                            tint = GreenText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(Space.xxs))
                        Text(text = "Goal met", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                } else {
                    Text(
                        text = "$earned of $goal XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        SoftCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = Shapes.tile,
            border = BorderHairline,
            onClick = onOpenReview,
            contentPadding = PaddingValues(Space.md)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Info.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.Repeat),
                        contentDescription = null,
                        tint = Info,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(Space.sm))
                Text(text = "Review", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(
                    text = if (uiState.wordsDue > 0) "${wordsToReview(uiState.wordsDue)} due today"
                    else "Nothing due today",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Stories ─────────────────────────────────────────────────────────────────────

/**
 * The story shelf: every story, locked ones included, because the lock is the motivation. "See all"
 * opens the Stories side of the Library.
 */
@Composable
private fun StoryShelf(
    uiState: LearnUiState,
    onOpenStories: () -> Unit,
    onOpenStory: (Int) -> Unit
) {
    SectionHeading(
        text = "Stories",
        action = {
            TextButton(
                onClick = onOpenStories,
                modifier = Modifier.semantics { contentDescription = "See all stories" }
            ) {
                Text("See all", color = LimeText, style = MaterialTheme.typography.labelMedium)
            }
        }
    )
    Spacer(Modifier.height(Space.sm))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(uiState.stories, key = { it.id }) { story ->
            StoryCoverCard(
                titleKasiguranin = story.titleKasiguranin,
                title = story.title,
                totalPages = story.totalPages,
                isUnlocked = story.isUnlocked,
                isCompleted = story.isCompleted,
                requiredXp = story.requiredXp,
                onClick = { onOpenStory(story.id) },
                // A cover is roughly 160 x 107 dp on this shelf.
                modifier = Modifier.width(160.dp),
                cover = rememberStoryCoverRes(story.id)
            )
        }
    }
}
