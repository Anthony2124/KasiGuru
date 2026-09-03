package com.kasiguru.ui.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that decide what the help page says about each chapter.
 *
 * These are worth pinning because every one of them fails silently. A wrong answer here does not
 * crash; it shows a learner six "New" badges they did not earn, or tells them a chapter is done when
 * they have never opened it, and nobody notices until someone complains months later.
 */
class TourProgressTest {

    private val core = coreChapter
    private fun stampOf(id: TourChapterId, v: Int) = stamp(id, v)

    // ── stamps ────────────────────────────────────────────────────────────────

    @Test
    fun `a stamp round-trips`() {
        val parsed = parseStamp(stampOf(TourChapterId.Dictionary, 3))
        assertEquals(TourChapterId.Dictionary to 3, parsed)
    }

    @Test
    fun `a malformed stamp is ignored rather than thrown on`() {
        // These are values read back off disk, possibly written by an older build. Data, not a crash.
        assertNull(parseStamp(""))
        assertNull(parseStamp("Dictionary"))
        assertNull(parseStamp("Dictionary:"))
        assertNull(parseStamp("Dictionary:notanumber"))
        assertNull(parseStamp("ChapterThatWasRenamed:1"))
    }

    // ── chapter state ─────────────────────────────────────────────────────────

    @Test
    fun `a chapter seen at its current version reads as done`() {
        val state = chapterState(
            chapter = core,
            completed = setOf(stampOf(core.id, core.version)),
            skipped = emptySet(),
            baseline = 0,
            resume = null
        )
        assertEquals(TourChapterState.Done, state)
    }

    @Test
    fun `bumping a chapter version makes exactly that chapter updated`() {
        // Seen at v1; the shipped chapter is now v2.
        val revised = core.copy(version = core.version + 1)
        val completed = setOf(stampOf(core.id, core.version))

        assertEquals(
            TourChapterState.Updated,
            chapterState(revised, completed, emptySet(), baseline = 0, resume = null)
        )
        // And nothing else moves: the unrevised chapter is still done.
        assertEquals(
            TourChapterState.Done,
            chapterState(core, completed, emptySet(), baseline = 0, resume = null)
        )
    }

    @Test
    fun `an existing install sees no new badges`() {
        // The case the baseline exists for: someone who finished onboarding before chapters shipped.
        tourChapters.forEach { chapter ->
            val state = chapterState(
                chapter = chapter,
                completed = emptySet(),
                skipped = emptySet(),
                baseline = CurrentTutorialVersion,
                resume = null
            )
            assertEquals(
                "Chapter ${chapter.id} badged as new for an existing install",
                TourChapterState.Available,
                state
            )
        }
    }

    @Test
    fun `a fresh install sees every chapter as new`() {
        tourChapters.forEach { chapter ->
            val state = chapterState(chapter, emptySet(), emptySet(), baseline = 0, resume = null)
            assertEquals(
                "Chapter ${chapter.id} should badge as new on a fresh install",
                TourChapterState.New,
                state
            )
        }
    }

    @Test
    fun `a chapter added after the baseline still badges`() {
        val laterChapter = core.copy(version = CurrentTutorialVersion + 1)
        val state = chapterState(
            chapter = laterChapter,
            completed = emptySet(),
            skipped = emptySet(),
            baseline = CurrentTutorialVersion,
            resume = null
        )
        assertEquals(TourChapterState.New, state)
    }

    @Test
    fun `skipping is offered again but never badged`() {
        val state = chapterState(
            chapter = core,
            completed = emptySet(),
            skipped = setOf(core.id.name),
            baseline = 0,
            resume = null
        )
        assertEquals(TourChapterState.Skipped, state)
    }

    @Test
    fun `an in-flight chapter outranks every other state`() {
        val state = chapterState(
            chapter = core,
            completed = setOf(stampOf(core.id, core.version)),
            skipped = setOf(core.id.name),
            baseline = 0,
            resume = TourResumePoint(core.id, 2)
        )
        assertEquals(TourChapterState.InProgress, state)
    }

    // ── resume sanitisation ───────────────────────────────────────────────────

    @Test
    fun `a resume point past the end of a shortened chapter is discarded`() {
        assertNull(sanitizeResume(core.id.name, core.stops.size))
        assertNull(sanitizeResume(core.id.name, core.stops.size + 5))
    }

    @Test
    fun `a resume point naming an unknown chapter is discarded`() {
        assertNull(sanitizeResume("ChapterThatWasRenamed", 0))
    }

    @Test
    fun `a nonsense resume point is discarded rather than thrown on`() {
        assertNull(sanitizeResume(null, 1))
        assertNull(sanitizeResume("", 1))
        assertNull(sanitizeResume(core.id.name, null))
        assertNull(sanitizeResume(core.id.name, -1))
    }

    @Test
    fun `a valid resume point survives`() {
        val point = sanitizeResume(core.id.name, 1)
        assertEquals(TourResumePoint(core.id, 1), point)
    }

    @Test
    fun `the shipped tutorial version is the highest chapter version`() {
        assertTrue(CurrentTutorialVersion >= 1)
        assertEquals(tourChapters.maxOf { it.version }, CurrentTutorialVersion)
    }
}
