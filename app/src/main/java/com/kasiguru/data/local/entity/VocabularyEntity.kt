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
    val nextReviewDate: String = "", // ISO format yyyy-MM-dd
    /**
     * Times this word has been forgotten after having been known.
     *
     * SM-2 as implemented had no memory of failure beyond the current easiness factor, so a word
     * failed for the tenth time looked the same as one failed for the first: interval back to a day,
     * and around again. Counting lapses is what lets the app notice a word that is not being learned
     * at all -- see [com.kasiguru.util.srs.Sm2Algorithm.LEECH_LAPSES] -- and treat it differently
     * from one the learner is simply still meeting.
     */
    @ColumnInfo(defaultValue = "0")
    val lapses: Int = 0,
    /**
     * Position on the relearning ladder, or 0 when the word is on its normal schedule.
     *
     * A lapse used to send a word to a one-day interval, after which a single correct answer put it
     * straight back to six days -- the same jump a word gets when it has never been failed at all.
     * The ladder makes the way back proportional to the fall.
     */
    @ColumnInfo(defaultValue = "0")
    val relearningStep: Int = 0
)
