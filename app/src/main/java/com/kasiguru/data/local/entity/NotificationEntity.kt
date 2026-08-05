package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an inbox notification message in KasiGuru.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: String,
    val category: String, // "Streak", "WordOfDay", "Leaderboard", "Achievement"
    val isRead: Boolean = false,
    val deepLinkRoute: String = ""
)
