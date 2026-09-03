package com.kasiguru.ui.tour

/**
 * The guided tour, in chapters.
 *
 * One chapter runs automatically: [TourChapterId.Core], once, when a learner finishes the onboarding
 * wizard. Every other chapter is optional and launched from the help page.
 *
 * The split exists because coverage and completion pull against each other. Explaining every control
 * on every screen is fifty-odd stops, and nobody finishes a fifty-step tour - they skip at step four
 * and learn less than the nine-stop version teaches. Chapters let the app cover everything while only
 * ever *asking* for ninety seconds.
 */
enum class TourChapterId {
    Core,
    Dictionary,
    Contribute,
    Lessons,
    Progress,
    ProfileSettings,
    Inbox
}

/**
 * A destination a stop needs to be on.
 *
 * Most stops name a fixed route. A few describe something that only exists at runtime - a category
 * that depends on the corpus, a story the learner has unlocked - and those are resolved once when
 * the chapter starts. See [TourRouteKind].
 */
sealed interface TourTarget {
    data class Fixed(val route: String) : TourTarget

    data class Resolved(val kind: TourRouteKind) : TourTarget
}

/**
 * Things a stop can ask for by description rather than by route.
 *
 * A tour cannot hardcode `vocabulary/17` or `story/3`: the ids depend on what is in the database and
 * on what this learner has unlocked. Each kind is answered by a single read at chapter start, and a
 * stop whose kind comes back empty is dropped before the chapter runs rather than left pointing at
 * nothing.
 */
enum class TourRouteKind {
    FirstVocabularyCategory,
    AnyVocabularyWord,
    FirstUnlockedStory,
    FirstUnlockedGameLevels
}

/**
 * @param version bumped whenever this chapter's content materially changes. The help page reads it to
 *   mark a chapter as updated, which is what lets a release add or revise one chapter without
 *   replaying the whole tutorial.
 */
data class TourChapter(
    val id: TourChapterId,
    val title: String,
    val subtitle: String,
    val version: Int,
    val stops: List<TourStop>
)

/**
 * Anchors that must never appear in a stop.
 *
 * These sit inside `androidx.compose.ui.window.Dialog`s, which are separate windows. The spotlight is
 * the last child of the navigation root in the *activity's* window, so it draws underneath them no
 * matter what composition order says, and a dialog's `boundsInRoot` is measured from the dialog's own
 * origin - a hole cut from those bounds lands in the top-left corner of the app.
 *
 * Worse, the overlay consumes every pointer event and holds the back handler, so a learner sent into
 * a dialog by the tour could neither dismiss it nor back out of it. Dialogs are explained in prose on
 * the help page, and the tour spotlights the control that opens them instead.
 *
 * `TourChaptersTest` asserts no stop uses one of these.
 */
val DialogAnchors: Set<TourAnchor> = emptySet()

/** Every chapter, in the order the help page lists them. */
val tourChapters: List<TourChapter> = listOf(
    coreChapter,
    dictionaryChapter,
    contributeChapter,
    lessonsChapter,
    progressChapter,
    profileSettingsChapter,
    inboxChapter
)

fun chapterById(id: TourChapterId): TourChapter? = tourChapters.firstOrNull { it.id == id }
