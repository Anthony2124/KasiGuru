package com.kasiguru.util

/**
 * Chooses the meaning to show when the learner has to produce the Kasiguranin word.
 *
 * Tagalog first, because that is the language the audience reads most fluently — but not when the
 * Tagalog gloss *is* the headword. Borrowed and shared vocabulary makes that common (`kuwan`,
 * `buhay`, `sayaw`), and printing it turns "write this in Kasiguranin" into "copy the word above",
 * which tests nothing and still pays XP. English is the fallback, and a word whose glosses are all
 * blank or all identical to the headword cannot be asked for at all.
 *
 * [Exercise-generating code][com.kasiguru.domain.lesson.ExerciseGenerator] already applies this rule
 * to multiple-choice options; typed recall needs it more, since there are no other options on screen
 * to make the giveaway obvious.
 */
object RecallPrompt {

    /** The usable meaning, or null when every gloss would hand over the answer. */
    fun meaningFor(kasiguranin: String, tagalog: String, english: String): String? {
        val headword = RecallAnswerMatcher.normalise(kasiguranin)
        return listOf(tagalog, english).firstOrNull {
            it.isNotBlank() && RecallAnswerMatcher.normalise(it) != headword
        }
    }
}
