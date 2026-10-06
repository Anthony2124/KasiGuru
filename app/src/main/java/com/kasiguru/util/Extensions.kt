package com.kasiguru.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Extension functions used throughout the app.
 */

fun LocalDate.toIsoString(): String =
    this.format(DateTimeFormatter.ISO_LOCAL_DATE)

fun LocalDateTime.toIsoString(): String =
    this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun String.toLocalDate(): LocalDate =
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)

fun Int.toXpString(): String = when {
    this >= 1000 -> "${this / 1000}.${(this % 1000) / 100}k XP"
    else -> "$this XP"
}

fun Int.toPercentString(): String = "$this%"

fun Float.toPercentString(): String = "${(this * 100).toInt()}%"

/**
 * Calculate the user's level from total XP.
 */
fun calculateLevel(totalXp: Int): Int {
    val thresholds = com.kasiguru.util.Constants.LEVEL_THRESHOLDS
    for (i in thresholds.indices.reversed()) {
        if (totalXp >= thresholds[i]) return i + 1
    }
    return 1
}

/**
 * Calculate XP progress toward the next level (0.0 to 1.0).
 */
fun calculateLevelProgress(totalXp: Int): Float {
    val thresholds = Constants.LEVEL_THRESHOLDS
    val level = calculateLevel(totalXp)
    if (level >= thresholds.size) return 1f

    val currentThreshold = thresholds[level - 1]
    val nextThreshold = thresholds[level]
    val xpInLevel = totalXp - currentThreshold
    val xpNeeded = nextThreshold - currentThreshold

    return (xpInLevel.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)
}

/**
 * Get the level title from Constants.
 */
fun getLevelTitle(level: Int): String {
    return "Level ${level.coerceIn(1, 30)}"
}

/**
 * Today's date now, and again at each midnight while collected.
 *
 * A screen that captured `LocalDate.now()` once kept showing yesterday's streak ticks after midnight
 * until it was rebuilt. Anything keyed to "today" should follow this instead.
 */
fun todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now()
        emit(today)
        val untilMidnight = Duration.between(
            LocalDateTime.now(), today.plusDays(1).atStartOfDay()
        ).toMillis()
        // A second past midnight, so the next read is surely the new day.
        delay(untilMidnight.coerceAtLeast(0) + 1_000)
    }
}.distinctUntilChanged()
