package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the single user's overall progress in the app.
 * Designed for a single-user offline app (id always = 1).
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "Learner",
    val password: String = "",
    val email: String = "",
    val fullName: String = "",
    val age: Int? = null,
    val address: String = "",
    val profileIconId: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",  // ISO date yyyy-MM-dd
    val wordsLearned: Int = 0,
    val storiesCompleted: Int = 0,
    val gamesPlayed: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val lessonsCompleted: Int = 0,
    val isOnboardingCompleted: Boolean = false,
    val dailyGoalXp: Int = 100,
    val titleBadge: String = "Kasiguranin Apprentice",
    /** Last-modified marker for cross-device sync (Phase 6). */
    val updatedAt: Long = 0
)
