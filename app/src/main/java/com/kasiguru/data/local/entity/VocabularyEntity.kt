package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Kasiguranin vocabulary word with its translations,
 * four aspectual verb forms, and SuperMemo-2 (SM-2) Spaced Repetition parameters.
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kasiguranin: String,
    val tagalog: String,
    val english: String,
    val rootForm: String,
    val neutralForm: String = "",
    val imperfectiveForm: String = "",
    val perfectiveForm: String = "",
    val contemplativeForm: String = "",
    val category: String,
    val audioFileName: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val phoneticGlottal: Boolean = false,
    val phoneticVowelLength: Boolean = false,
    val ipaNotation: String = "",
    val isLearned: Boolean = false,
    val timesReviewed: Int = 0,
    // SuperMemo-2 (SM-2) SRS Fields
    val easinessFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val nextReviewDate: String = "" // ISO format yyyy-MM-dd
)
