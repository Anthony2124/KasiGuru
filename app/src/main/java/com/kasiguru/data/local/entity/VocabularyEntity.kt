package com.kasiguru.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a Kasiguranin vocabulary word with its translations,
 * four aspectual verb forms, and SuperMemo-2 (SM-2) Spaced Repetition parameters.
 */
@Entity(
    tableName = "vocabulary",
    indices = [
        Index(value = ["category"], name = "index_vocabulary_category"),
        Index(value = ["isLearned"], name = "index_vocabulary_isLearned"),
        Index(value = ["nextReviewDate"], name = "index_vocabulary_nextReviewDate"),
        Index(value = ["timesReviewed"], name = "index_vocabulary_timesReviewed")
    ]
)
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // Defaults on every field so Firestore can deserialize this class
    // (toObjects needs a no-argument constructor). Room is unaffected:
    // Kotlin defaults do not change the generated SQL schema.
    val kasiguranin: String = "",
    val tagalog: String = "",
    val english: String = "",
    val rootForm: String = "",
    @ColumnInfo(defaultValue = "")
    val neutralForm: String = "",
    @ColumnInfo(defaultValue = "")
    val imperfectiveForm: String = "",
    @ColumnInfo(defaultValue = "")
    val perfectiveForm: String = "",
    @ColumnInfo(defaultValue = "")
    val contemplativeForm: String = "",
    val category: String = "",
    @ColumnInfo(name = "audioResName", defaultValue = "")
    val audioFileName: String = "",
    @ColumnInfo(defaultValue = "")
    val exampleSentence: String = "",
    @ColumnInfo(defaultValue = "")
    val exampleTranslation: String = "",
    /**
     * A second example sentence, so a meaning is illustrated twice rather than once. Flat
     * columns rather than a related table: the requirement is fixed at exactly two, and every
     * consumer (GameAnswerFeedback, the lesson player, the admin word-edit form) already reads
     * exampleSentence/exampleTranslation as plain fields.
     */
    @ColumnInfo(defaultValue = "")
    val exampleSentence2: String = "",
    @ColumnInfo(defaultValue = "")
    val exampleTranslation2: String = "",
    @ColumnInfo(defaultValue = "0")
    val phoneticGlottal: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val phoneticVowelLength: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val ipaNotation: String = "",
    @ColumnInfo(defaultValue = "0")
    val isLearned: Boolean = false,
    val timesReviewed: Int = 0,
    // SuperMemo-2 (SM-2) SRS Fields
    val easinessFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val nextReviewDate: String = "" // ISO format yyyy-MM-dd
)
