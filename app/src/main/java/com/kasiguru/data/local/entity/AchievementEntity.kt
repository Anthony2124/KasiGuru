package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a gamification achievement/badge.
 *
 * [metricType] is what makes this scale without a schema change per new badge: unlock logic
 * lives once, in UserProgressRepository.checkAchievements(metricType, currentValue), reading
 * [requiredValue] off the row itself instead of a hardcoded threshold per badge id. Adding a
 * badge is a new seeded row with an existing metricType (free) or a new one plus one place that
 * calls checkAchievements for it (see [MetricType]).
 *
 * [tier] is optional and wires the app's existing TierGold/TierSilver/TierBronze theme tokens to
 * real badges instead of leaving them unused.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,  // e.g. "first_word", "ten_words"
    val name: String,
    val description: String,
    val iconEmoji: String = "🏆",
    val category: String = "General",
    val requiredValue: Int = 1,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null,
    val xpReward: Int = 50,
    val metricType: String = "",
    val tier: String? = null
)

/** The known metric families [AchievementEntity.metricType] can hold. */
object MetricType {
    const val WORDS_LEARNED = "wordsLearned"
    const val STORIES_COMPLETED = "storiesCompleted"
    const val GAMES_PLAYED = "gamesPlayed"
    const val PERFECT_GAME = "perfectGame"
    const val STREAK = "streak"
    const val LEVEL = "level"
    const val SUBMISSIONS_MADE = "submissionsMade"
    const val SUBMISSIONS_APPROVED = "submissionsApproved"
    const val CATEGORY_MASTERED = "categoryMastered"
    const val WEEKLY_TOP_TEN = "weeklyTopTen"
    const val GAME_MODES_PLAYED = "gameModesPlayed"
}
