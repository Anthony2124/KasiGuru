package com.kasiguru.domain.gamification

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The day-gap rules behind the practice streak, in one place.
 *
 * They used to be written out three times — when the streak advanced, when an expired one was reset
 * on startup, and when two devices' progress was merged — and the three only agreed by care. The
 * streak is a run of consecutive calendar days ending on `lastActiveDate`; a gap of two or more days
 * breaks it.
 */
object StreakRules {

    /** Days from [lastActive] to [today], or null when no valid date was ever recorded. */
    private fun gap(lastActive: String, today: LocalDate): Long? =
        lastActive.takeIf { it.isNotEmpty() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { ChronoUnit.DAYS.between(it, today) }

    /**
     * The streak after today counts. Already counted today: unchanged. Yesterday counted: one more.
     * Anything older, or never: a fresh run of 1. A date after today (the clock moved back, or a
     * device ahead synced first) counts as already counted, rather than wiping the run.
     */
    fun advancedStreak(current: Int, lastActive: String, today: LocalDate): Int {
        val days = gap(lastActive, today) ?: return 1
        return when {
            days <= 0L -> current.coerceAtLeast(1)
            days == 1L -> current + 1
            else -> 1
        }
    }

    /** True when a live streak has already missed a whole day and must show as 0. */
    fun isExpired(current: Int, lastActive: String, today: LocalDate): Boolean {
        if (current <= 0) return false
        val days = gap(lastActive, today) ?: return false
        return days > 1
    }
}
