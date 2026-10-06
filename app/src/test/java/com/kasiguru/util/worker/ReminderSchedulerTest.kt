package com.kasiguru.util.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

/** The reminder fires at the chosen time of day, not a fixed interval after the app was opened. */
class ReminderSchedulerTest {

    private val sevenPm = 19 * 60

    @Test
    fun laterTodayWaitsUntilThatTime() {
        val now = LocalDateTime.of(2026, 10, 6, 14, 30)
        assertEquals(Duration.ofMinutes(4 * 60 + 30), ReminderScheduler.delayUntil(sevenPm, now))
    }

    @Test
    fun aTimeAlreadyPassedMeansTomorrow() {
        val now = LocalDateTime.of(2026, 10, 6, 20, 0)
        assertEquals(Duration.ofHours(23), ReminderScheduler.delayUntil(sevenPm, now))
    }

    @Test
    fun exactlyNowMeansTomorrowNotImmediately() {
        val now = LocalDateTime.of(2026, 10, 6, 19, 0)
        assertEquals(Duration.ofHours(24), ReminderScheduler.delayUntil(sevenPm, now))
    }
}
