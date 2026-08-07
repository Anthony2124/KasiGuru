package com.kasiguru.data.local.entity

import androidx.room.Entity

/**
 * Tracks per-level progress for each mini-game.
 * Each game has 30 levels (1-10 Easy, 11-20 Medium, 21-30 Hard).
 */
@Entity(
    tableName = "game_levels",
    primaryKeys = ["gameType", "levelNumber"]
)
data class GameLevelEntity(
    val gameType: String,        // "word_match", "fill_blank", etc.
    val levelNumber: Int,        // 1..30
    val difficulty: String,      // "Easy", "Medium", "Hard"
    val starsEarned: Int = 0,    // 0..3
    val isUnlocked: Boolean = false,
    val questionsCount: Int = 5  // Easy=5, Medium=10, Hard=15
)
