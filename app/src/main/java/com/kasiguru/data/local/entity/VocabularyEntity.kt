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
    /**
     * Which learning-tree section claims this word, or blank.
     *
     * A second, journey-facing home that leaves [category] alone: the dictionary keeps filing words
     * the way a dictionary should, while the tree can group them by situation. It exists because the
     * situational sections cannot be derived from category at all - the travel words are scattered
     * across all thirteen categories, and so are the coastal ones.
     *
     * A tagged word is claimed by its theme section and leaves the category section, so tagging moves
     * a word rather than duplicating it. Blank means untagged, which is the overwhelming majority and
     * simply falls through to the category-backed section.
     */
    @ColumnInfo(defaultValue = "")
    val theme: String = "",
    /**
     * Noun, Verb, Adjective and so on, as chosen in the admin portal.
     *
     * Collected in three places long before it was stored in any of them: the admin add/edit forms,
     * the admin word table, and the in-app contribution form all wrote it to Firestore, and nothing
     * on the device had a column to receive it. A word's part of speech is the first thing a
     * dictionary entry states, so it is worth the column.
     */
    @ColumnInfo(defaultValue = "")
    val partOfSpeech: String = "",
    /**
     * A one-sentence English definition of the sense [english] names.
     *
     * The corpus glosses a word and stops: `apak` says "adze". A learner who does not already know
     * what an adze is has learned nothing, and the six games can offer no hint beyond re-showing the
     * translation they are asking about. These two fields say what the word *means*.
     *
     * They are editorial English/Tagalog prose written for this app from the existing translations
     * -- see docs/DICTIONARY_MEANINGS.md. They assert nothing about Kasiguranin that the corpus does
     * not already record.
     */
    @ColumnInfo(defaultValue = "")
    val meaningEnglish: String = "",
    /** The same definition in plain Filipino. See [meaningEnglish]. */
    @ColumnInfo(defaultValue = "")
    val meaningTagalog: String = "",
    @ColumnInfo(name = "audioResName", defaultValue = "")
    val audioFileName: String = "",
    @ColumnInfo(defaultValue = "")
    val exampleSentence: String = "",
    @ColumnInfo(defaultValue = "")
    val exampleTranslation: String = "",
    /**
     * A second example sentence, so a meaning is illustrated twice rather than once. Flat
     * columns rather than a related table: the requirement is fixed at exactly two, and every
     * consumer reads exampleSentence/exampleTranslation as plain fields.
     *
     * These shipped as write-only for several versions: the admin form wrote them, the sync parsed
     * them, Room stored them, and no screen ever read them back, so a second sentence recorded in
     * the portal never reached a learner. The word detail screen and the word bottom sheet render
     * them now; the games and the lesson player still show only the first pair.
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
