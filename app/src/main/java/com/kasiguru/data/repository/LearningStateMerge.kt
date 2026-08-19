package com.kasiguru.data.repository

/**
 * Merge rules for the progress that lives outside `user_progress` (achievements,
 * per-game level stars, per-lesson completion, per-word review state).
 *
 * Every rule is additive — a device that is behind can never erase progress
 * earned elsewhere. That is what makes a reinstall or a second device safe:
 * the empty side contributes nothing instead of overwriting.
 */

data class AchievementState(
    val isUnlocked: Boolean = false,
    val currentValue: Int = 0,
    val unlockedDate: String? = null
)

data class GameLevelState(
    val starsEarned: Int = 0,
    val isUnlocked: Boolean = false
)

data class LessonState(
    val isComplete: Boolean = false,
    val bestAccuracy: Float = 0f,
    val timesCompleted: Int = 0,
    val lastCompletedAt: Long = 0
)

data class WordState(
    val isLearned: Boolean = false,
    val timesReviewed: Int = 0,
    val easinessFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val nextReviewDate: String = ""
)

internal fun mergeAchievements(
    local: Map<String, AchievementState>,
    remote: Map<String, AchievementState>
): Map<String, AchievementState> =
    (local.keys + remote.keys).associateWith { id ->
        val l = local[id]
        val r = remote[id]
        when {
            l == null -> r!!
            r == null -> l
            else -> AchievementState(
                isUnlocked = l.isUnlocked || r.isUnlocked,
                currentValue = maxOf(l.currentValue, r.currentValue),
                // Keep the earliest unlock date; it is when the badge was actually earned.
                unlockedDate = listOfNotNull(l.unlockedDate, r.unlockedDate).minOrNull()
            )
        }
    }

internal fun mergeGameLevels(
    local: Map<String, GameLevelState>,
    remote: Map<String, GameLevelState>
): Map<String, GameLevelState> =
    (local.keys + remote.keys).associateWith { key ->
        val l = local[key]
        val r = remote[key]
        when {
            l == null -> r!!
            r == null -> l
            else -> GameLevelState(
                starsEarned = maxOf(l.starsEarned, r.starsEarned),
                isUnlocked = l.isUnlocked || r.isUnlocked
            )
        }
    }

internal fun mergeLessonProgress(
    local: Map<String, LessonState>,
    remote: Map<String, LessonState>
): Map<String, LessonState> =
    (local.keys + remote.keys).associateWith { key ->
        val l = local[key]
        val r = remote[key]
        when {
            l == null -> r!!
            r == null -> l
            else -> LessonState(
                isComplete = l.isComplete || r.isComplete,
                bestAccuracy = maxOf(l.bestAccuracy, r.bestAccuracy),
                timesCompleted = maxOf(l.timesCompleted, r.timesCompleted),
                lastCompletedAt = maxOf(l.lastCompletedAt, r.lastCompletedAt)
            )
        }
    }

/**
 * Word review state. The side that has reviewed the word more often is treated as
 * the authority on the SM-2 scheduling fields, so a half-forgotten copy on a fresh
 * install cannot reset a mature review interval.
 */
internal fun mergeWordStates(
    local: Map<String, WordState>,
    remote: Map<String, WordState>
): Map<String, WordState> =
    (local.keys + remote.keys).associateWith { key ->
        val l = local[key]
        val r = remote[key]
        when {
            l == null -> r!!
            r == null -> l
            else -> {
                val authority = if (r.timesReviewed >= l.timesReviewed) r else l
                WordState(
                    isLearned = l.isLearned || r.isLearned,
                    timesReviewed = maxOf(l.timesReviewed, r.timesReviewed),
                    easinessFactor = authority.easinessFactor,
                    intervalDays = maxOf(l.intervalDays, r.intervalDays),
                    nextReviewDate = authority.nextReviewDate
                )
            }
        }
    }
