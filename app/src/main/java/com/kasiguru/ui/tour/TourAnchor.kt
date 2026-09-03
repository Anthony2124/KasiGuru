package com.kasiguru.ui.tour

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The elements the guided tour can point at.
 *
 * One entry per stop that has a target. These are positions in the live UI, not screens: the tour
 * cuts a hole around the real control the learner will be tapping a minute later, which is the whole
 * reason it is a spotlight rather than a slideshow of pictures.
 */
enum class TourAnchor {
    // ── Learn ────────────────────────────────────────────────────────────────
    /** Learn's primary action - the button that names whatever comes next. */
    ContinueAction,

    /** The Gold streak pill in Learn's canopy. Pinned, so it cannot scroll out of view. */
    StreakBadge,

    /** The notification bell in Learn's canopy. */
    NotificationBell,

    /** The daily-goal ring in Learn's canopy. */
    DailyGoalRing,

    // ── The bar ──────────────────────────────────────────────────────────────
    NavLearn,
    NavPractice,
    NavWords,
    NavProgress,
    NavProfile,

    // ── Dictionary ───────────────────────────────────────────────────────────
    DictSubmitBanner,
    DictWordOfDay,

    // ── Contributing a word ──────────────────────────────────────────────────
    SubmitWordField,
    SubmitButton,

    // ── Practice ─────────────────────────────────────────────────────────────
    PracticeStats,
    PracticeFeatured,

    // ── Progress ─────────────────────────────────────────────────────────────
    ProgressBadgePanel,
    ProgressFilter,

    // ── Profile and Settings ─────────────────────────────────────────────────
    ProfileSettingsIcon,
    ProfileExplore,
    SettingsAccount,
    SettingsPreferences,
    SettingsReplayTutorial,

    // Notifications and stories
    InboxFilters,
    StoryShelf
}

/**
 * Where each [TourAnchor] currently is on screen.
 *
 * The overlay is drawn at the navigation root; the things it points at live several layers down, in
 * `LearnScreen` and `KasiGuruBottomBar`. This is the bridge: anchored composables report their
 * bounds up, the overlay reads them back down.
 *
 * Deliberately a plain composition-scoped holder rather than a Hilt singleton. These are root-space
 * pixel rectangles measured against the current window; surviving a configuration change would not
 * make them useful, it would make them wrong, and a rotation would hand the new composition
 * coordinates from the old window size. Tour *progress* - which stop we are on - does belong in a
 * ViewModel, and lives in [TourViewModel]. The split is: the ViewModel owns "where are we", this
 * owns "where is it".
 */
@Stable
class TourAnchorRegistry {

    /**
     * Whether anchors should measure themselves at all.
     *
     * False for all but the few seconds a tour is running. [tourAnchor] returns the modifier chain
     * untouched while it is false, so the app does not carry seven `onGloballyPositioned` nodes
     * around for a feature that fires once. Flipping it costs one recomposition of the anchored
     * screens, which is the better half of that trade.
     */
    var active by mutableStateOf(false)
        internal set

    /**
     * The anchor the tour would like brought on screen.
     *
     * Set by the navigation shell when the stop changes; read by the small reveal helpers each
     * scrolling screen installs. It lives here rather than being pushed into the screens because the
     * overlay has no way to reach into a `LazyColumn`'s scroll state, and an item scrolled far out of
     * a lazy list is not composed at all - so nothing attached to the item itself could scroll it back.
     */
    var pendingReveal by mutableStateOf<TourAnchor?>(null)
        private set

    fun requestReveal(key: TourAnchor?) {
        if (pendingReveal != key) pendingReveal = key
    }

    private val bounds = mutableStateMapOf<TourAnchor, Rect>()
    private val unclipped = mutableStateMapOf<TourAnchor, Rect>()

    fun report(key: TourAnchor, rect: Rect, unclippedRect: Rect) {
        if (bounds[key] != rect) bounds[key] = rect
        if (unclipped[key] != unclippedRect) unclipped[key] = unclippedRect
    }

    fun forget(key: TourAnchor) {
        bounds.remove(key)
        unclipped.remove(key)
    }

    /**
     * Where the element is regardless of whether it is currently on screen.
     *
     * [boundsOf] is clipped by every ancestor, which is right for cutting a hole - an element
     * scrolled out of view has nothing to cut - but useless for scrolling it back, because a fully
     * clipped element reports an empty rectangle and so cannot say how far away it is. This is the
     * unclipped position, and it exists only so the reveal helpers can compute that distance.
     */
    fun unclippedBoundsOf(key: TourAnchor): Rect? = unclipped[key]

    /**
     * The anchor's rectangle, or null if it has not been measured or is not currently visible.
     *
     * An anchor clipped entirely out of view by an ancestor reports an empty rect rather than
     * disappearing, so emptiness is filtered here rather than at every call site. Callers treat null
     * as "dim the screen, do not cut a hole" - never as "cut a hole at the origin".
     */
    fun boundsOf(key: TourAnchor): Rect? = bounds[key]?.takeIf { !it.isEmpty }

    companion object {
        /** Used wherever no tour can run: previews, tests, and any screen rendered standalone. */
        val Inert = TourAnchorRegistry()
    }
}

/**
 * Defaults to [TourAnchorRegistry.Inert] rather than throwing, so a composable carrying a
 * [tourAnchor] still renders in a `@Preview` and in tests that mount it without the nav graph.
 */
val LocalTourAnchors = staticCompositionLocalOf { TourAnchorRegistry.Inert }

/**
 * Reports this element's position to the tour, so the spotlight can cut a hole around it.
 *
 * Uses `boundsInRoot`, not `boundsInWindow`: the overlay is a full-size child of the same root as
 * everything it points at, so root coordinates can go straight to the draw scope untranslated.
 * Window coordinates would additionally depend on where the compose view sits inside the window,
 * which is only the origin because the activity draws edge to edge - an invariant split-screen
 * breaks.
 */
fun Modifier.tourAnchor(key: TourAnchor): Modifier = composed {
    val registry = LocalTourAnchors.current
    if (!registry.active) return@composed this

    // Scoped to the calling composable, so leaving a screen cannot strand its bounds behind and have
    // the overlay cut a hole where the element used to be.
    DisposableEffect(registry, key) {
        onDispose { registry.forget(key) }
    }

    this.onGloballyPositioned { coordinates ->
        registry.report(
            key = key,
            rect = coordinates.boundsInRoot(),
            unclippedRect = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
        )
    }
}

/**
 * As [tourAnchor], but a no-op when [key] is null.
 *
 * For a repeating list where only one instance should carry the anchor - the first story card, say,
 * standing in for "the story shelf" as a whole. Passing null for every non-target instance is what
 * keeps the registry from being overwritten by whichever copy last recomposed, which is what happens
 * if the same key is unconditionally attached to every item.
 */
@JvmName("tourAnchorOrNone")
fun Modifier.tourAnchor(key: TourAnchor?): Modifier = if (key == null) this else tourAnchor(key)
