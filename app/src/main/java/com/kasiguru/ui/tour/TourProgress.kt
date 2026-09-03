package com.kasiguru.ui.tour

/**
 * What the learner has and has not seen of the tutorial, and what the help page should say about it.
 *
 * Pure functions over plain values. Nothing here touches Android, DataStore or Compose, so all of it
 * is covered by ordinary JVM tests - which matters, because the rules below are the kind that go
 * quietly wrong on an app update and are then very hard to notice.
 */

/** Highest chapter version currently shipped. Used to stamp the baseline on an existing install. */
val CurrentTutorialVersion: Int get() = tourChapters.maxOfOrNull { it.version } ?: 1

/**
 * How a completed chapter is recorded: `"Dictionary:2"`.
 *
 * The version travels *inside* the stamp on purpose. A per-chapter integer or one global version
 * number can only say "something changed" and so must replay everything or nothing; a set of stamps
 * lets a release add one chapter, or revise one chapter, and have exactly that chapter come back as
 * new while every other stays done.
 *
 * Stale stamps are never pruned. They cost a few bytes and they are the record of what someone
 * actually saw.
 */
fun stamp(id: TourChapterId, version: Int): String = "${id.name}:$version"

fun stamp(chapter: TourChapter): String = stamp(chapter.id, chapter.version)

/** Null rather than throwing: a stamp written by a build that has since renamed a chapter is data, not a crash. */
fun parseStamp(raw: String): Pair<TourChapterId, Int>? {
    val id = raw.substringBefore(':', "")
    val version = raw.substringAfter(':', "").toIntOrNull() ?: return null
    val chapterId = TourChapterId.entries.firstOrNull { it.name == id } ?: return null
    return chapterId to version
}

/** Where a chapter was left. */
data class TourResumePoint(val chapterId: TourChapterId, val step: Int)

/** What the help page shows against a chapter. */
enum class TourChapterState {
    /** Left part-way through; the row offers to continue rather than restart. */
    InProgress,

    /** Never seen, and added after this install's baseline. Badged. */
    New,

    /** Seen at an older version and revised since. Badged. */
    Updated,

    /** Seen at its current version. */
    Done,

    /** Started and skipped. Offered again, not badged - they already said no once. */
    Skipped,

    /** Never seen, but present at install time. Launchable, not badged. */
    Available
}

/**
 * Resolves what the help page should say about one chapter.
 *
 * The [baseline] is what stops an existing learner seeing six New badges the moment they update. It
 * is written once per install: for someone who had already finished onboarding when this feature
 * arrived it is the current version, so every chapter shipping today counts as pre-existing and
 * launchable-but-quiet; anything added later is genuinely new to them and does badge. A fresh install
 * gets zero, so every chapter badges as new, which is correct for someone who has seen none of it.
 *
 * The rejected alternative was to seed the completed set with every current chapter on update. That
 * would have the help page read "Done" for six chapters nobody has opened, which suppresses the exact
 * affordance the chapters exist to offer.
 */
fun chapterState(
    chapter: TourChapter,
    completed: Set<String>,
    skipped: Set<String>,
    baseline: Int,
    resume: TourResumePoint?
): TourChapterState {
    if (resume?.chapterId == chapter.id) return TourChapterState.InProgress

    val seenVersions = completed.mapNotNull(::parseStamp)
        .filter { it.first == chapter.id }
        .map { it.second }

    if (chapter.version in seenVersions) return TourChapterState.Done
    if (chapter.id.name in skipped) return TourChapterState.Skipped

    // Revised since they saw it: they have a stamp, just an older one.
    if (seenVersions.isNotEmpty()) return TourChapterState.Updated

    return if (chapter.version > baseline) TourChapterState.New else TourChapterState.Available
}

/**
 * Discards a stored resume point that no longer makes sense.
 *
 * Two ways it can rot: the chapter was renamed or removed, or a release shortened it so the stored
 * step is past the end. Both start the chapter from the beginning rather than reading off the end of
 * the list, which is the difference between a slightly worse tour and a crash on launch.
 */
fun sanitizeResume(chapterId: String?, step: Int?): TourResumePoint? {
    if (chapterId.isNullOrBlank() || step == null || step < 0) return null
    val id = TourChapterId.entries.firstOrNull { it.name == chapterId } ?: return null
    val chapter = chapterById(id) ?: return null
    if (step >= chapter.stops.size) return null
    return TourResumePoint(id, step)
}
