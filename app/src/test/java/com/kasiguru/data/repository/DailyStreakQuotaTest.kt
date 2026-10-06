package com.kasiguru.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The day's streak quota: the due review, then three games.
 *
 * With nothing scheduled there is no review to finish. Requiring one anyway meant a new learner, or
 * one who was caught up, could never keep a streak at all.
 */
class DailyStreakQuotaTest {

    @Test
    fun nothingDueAndThreeGamesKeepsTheStreak() {
        assertTrue(DailyStreakQuota(reviewCompleted = false, gamesPlayed = 3, reviewDue = false).isQuotaMet)
    }

    @Test
    fun aDueReviewStillHasToBeFinished() {
        assertFalse(DailyStreakQuota(reviewCompleted = false, gamesPlayed = 3, reviewDue = true).isQuotaMet)
        assertTrue(DailyStreakQuota(reviewCompleted = true, gamesPlayed = 3, reviewDue = true).isQuotaMet)
    }

    @Test
    fun gamesAreAlwaysRequired() {
        assertFalse(DailyStreakQuota(reviewCompleted = true, gamesPlayed = 2, reviewDue = false).isQuotaMet)
        assertEquals(1, DailyStreakQuota(gamesPlayed = 2).gamesRemaining)
        assertEquals(0, DailyStreakQuota(gamesPlayed = 5).gamesRemaining)
    }
}
