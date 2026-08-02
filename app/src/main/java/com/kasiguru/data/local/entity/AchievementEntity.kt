package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a gamification achievement/badge.
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
    val xpReward: Int = 50
)
