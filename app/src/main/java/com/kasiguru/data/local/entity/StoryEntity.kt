package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a Kasiguranin digital story.
 * Pages are stored as JSON for flexibility.
 */
@Entity(
    tableName = "stories",
    indices = [Index(value = ["requiredXp"], name = "index_stories_requiredXp")]
)
data class StoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // Defaults so Firestore's toObjects can construct this (needs a no-arg
    // constructor); Room's schema is unchanged by Kotlin defaults.
    val title: String = "",
    val titleKasiguranin: String = "",
    val description: String = "",
    val category: String = "",
    val iconEmoji: String = "📖",
    val pagesJson: String = "[]",  // JSON array of StoryPage objects
    val totalPages: Int = 0,
    val requiredXp: Int = 0,
    val isUnlocked: Boolean = true,
    val isCompleted: Boolean = false,
    val currentPage: Int = 0
)

/**
 * Data class for a single page within a story (serialized to/from JSON).
 */
data class StoryPage(
    val pageNumber: Int,
    val kasiguranin: String,
    val tagalog: String,
    val english: String,
    val audioFileName: String = "",
    val illustrationDesc: String = ""
)
