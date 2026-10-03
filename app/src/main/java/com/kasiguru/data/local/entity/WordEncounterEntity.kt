package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A dictionary word the learner has met somewhere in the app: in a lesson, a flashcard review or a
 * game. One row per word, so the Library's My words list is every word met, most recent first.
 *
 * Local only, like the rest of the practice history this is built from. Words met before the table
 * existed are filled in once from lesson progress and SM-2 state (see WordEncounterRepository).
 */
@Entity(tableName = "word_encounters")
data class WordEncounterEntity(
    @PrimaryKey val wordId: Int,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val timesSeen: Int,
    /** Where it was last met: "lesson", "review", or a game mode such as "word_match". */
    val lastSource: String
)
