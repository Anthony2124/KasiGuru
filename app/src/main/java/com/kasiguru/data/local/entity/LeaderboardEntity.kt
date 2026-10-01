package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a entry in the Leaderboard.
 * Supports both local Room SQLite caching and cloud sync.
 */
@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val totalXp: Int,
    val currentStreak: Int,
    val avatarIconId: Int = 1,
    val levelTitle: String = "Baguhan",
    val isCurrentUser: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "''") val firebaseUid: String = "",
    @androidx.room.ColumnInfo(defaultValue = "0") val weeklyXp: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "''") val weekId: String = "",
    @androidx.room.ColumnInfo(defaultValue = "1") val level: Int = 1,
    @androidx.room.ColumnInfo(defaultValue = "'alltime'") val boardId: String = "alltime",
    @androidx.room.ColumnInfo(defaultValue = "0") val rank: Int = 0
)
