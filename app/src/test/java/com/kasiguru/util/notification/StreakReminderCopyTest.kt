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
    fun theBodyNamesHowMuchIsDue() {
        assertEquals("12 words are due for review tonight.", StreakReminderCopy.body(12))
    }

    @Test
    fun oneWordIsNotOneWords() {
        assertEquals("1 word is due for review tonight.", StreakReminderCopy.body(1))
    }

    @Test
    fun nothingDueNeverInventsABacklog() {
        // The fresh-install case: every word has an empty review date, and none of them are due.
        val body = StreakReminderCopy.body(0)
        assertFalse(body.contains("due"))
        assertEquals("A short lesson keeps the streak going.", body)
    }

    @Test
    fun aNegativeCountIsTreatedAsNothingDue() {
        assertEquals(StreakReminderCopy.body(0), StreakReminderCopy.body(-3))
    }

    @Test
    fun theTitleOnlyClaimsAStreakThatExists() {
        assertEquals("Keep your 5-day streak alive 🔥", StreakReminderCopy.title(5))
        assertEquals("Learn Kasiguranin today 📚", StreakReminderCopy.title(0))
    }
}
