package com.kasiguru.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakRulesTest {

    private val today = LocalDate.of(2026, 10, 6)

    @Test
    fun aDayAfterTheLastCountedDayExtendsTheRun() {
        assertEquals(5, StreakRules.advancedStreak(4, "2026-10-05", today))
    }

    @Test
    fun aMissedDayStartsAFreshRun() {
        assertEquals(1, StreakRules.advancedStreak(9, "2026-10-04", today))
    }

    @Test
    fun theFirstCountedDayIsOne() {
        assertEquals(1, StreakRules.advancedStreak(0, "", today))
        assertEquals(1, StreakRules.advancedStreak(0, "not a date", today))
    }

    @Test
    fun countingTheSameDayTwiceChangesNothing() {
        assertEquals(4, StreakRules.advancedStreak(4, "2026-10-06", today))
    }

    @Test
    fun aDateAheadOfTodayDoesNotWipeTheRun() {
        assertEquals(4, StreakRules.advancedStreak(4, "2026-10-07", today))
    }

    @Test
    fun aStreakExpiresOnlyAfterAWholeMissedDay() {
        assertFalse(StreakRules.isExpired(3, "2026-10-06", today))
        assertFalse(StreakRules.isExpired(3, "2026-10-05", today))
        assertTrue(StreakRules.isExpired(3, "2026-10-04", today))
    }

    @Test
    fun noStreakOrNoDateNeverExpires() {
        assertFalse(StreakRules.isExpired(0, "2026-01-01", today))
        assertFalse(StreakRules.isExpired(3, "", today))
    }
}
