package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records individual game session scores for analytics.
 */
@Entity(tableName = "game_scores")
data class GameScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gameType: String,  // "word_match", "fill_blank", "audio_quiz"
    val score: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val playedAt: String = System.currentTimeMillis().toString()
)
