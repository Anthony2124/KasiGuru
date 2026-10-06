package com.kasiguru.util.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Guards the nightly reminder against saying something the app cannot back up.
 *
 * A notification is the one surface a learner sees without opening the app, so a claim made here is
 * checked against reality the moment they tap it.
 */
class StreakReminderCopyTest {

    @Test
    fun theBodyNamesTheReviewAndTheGamesLeft() {
        assertEquals(
            "Review 12 words and play 3 games to keep your streak.",
            StreakReminderCopy.body(currentStreak = 4, dueCount = 12, reviewDone = false, gamesLeft = 3)
        )
    }

    @Test
    fun oneWordIsNotOneWords() {
        assertEquals(
            "Review 1 word to keep your streak.",
            StreakReminderCopy.body(currentStreak = 4, dueCount = 1, reviewDone = false, gamesLeft = 0)
        )
    }

    @Test
    fun nothingDueNeverInventsABacklog() {
        // The fresh-install case: no word has a review date, so only the games are left to ask for.
        val body = StreakReminderCopy.body(currentStreak = 0, dueCount = 0, reviewDone = false, gamesLeft = 3)
        assertFalse(body.contains("review", ignoreCase = true))
        assertEquals("Play 3 games to start a streak.", body)
    }

    @Test
    fun aFinishedReviewIsNotAskedForAgain() {
        assertEquals(
            "Play 1 more game to keep your streak.",
            StreakReminderCopy.body(currentStreak = 2, dueCount = 5, reviewDone = true, gamesLeft = 1)
        )
    }

    @Test
    fun neverRecommendsALessonForTheStreak() {
        // A lesson does not count toward the streak; the old copy sent learners to one anyway.
        val body = StreakReminderCopy.body(currentStreak = 3, dueCount = 0, reviewDone = false, gamesLeft = 2)
        assertFalse(body.contains("lesson"))
    }

    @Test
    fun theTitleOnlyClaimsAStreakThatExists() {
        assertEquals("Keep your 5-day streak alive 🔥", StreakReminderCopy.title(5))
        assertEquals("Learn Kasiguranin today 📚", StreakReminderCopy.title(0))
    }
}
