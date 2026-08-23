package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity

/**
 * Turns a wrong answer into something the learner can learn from.
 *
 * Until now a miss produced one red option and the correct answer — a verdict, not an explanation.
 * But a wrong choice in this app is almost always another real word, and the reason it was chosen is
 * that its meaning was not distinguished from the answer's. Naming what the learner actually picked
 * closes exactly that gap, and it is the cheapest teaching moment the app has: the confusion has
 * already been demonstrated, in the learner's own answer.
 */
object Remediation {

    /**
     * A line contrasting the chosen word with the answer, or null when there is nothing to say.
     *
     * Null covers the cases where a contrast would mislead rather than teach: choosing another form
     * of the same word (the aspect exercises are built from one entry), or typing something that is
     * not a word at all.
     */
    fun contrastLine(chosen: String, chosenEntry: VocabularyEntity?, target: VocabularyEntity): String? {
        if (chosenEntry == null || chosenEntry.id == target.id) return null

        val meaning = glossOf(chosenEntry)
        val choseTheHeadword = chosen.trim().equals(chosenEntry.kasiguranin, ignoreCase = true)

        return when {
            choseTheHeadword && meaning.isNotBlank() ->
                "You chose ${chosenEntry.kasiguranin}, which means $meaning."

            !choseTheHeadword && chosenEntry.kasiguranin.isNotBlank() ->
                "You chose ${chosen.trim()}, which is ${chosenEntry.kasiguranin}."

            else -> null
        }
    }

    /** Tagalog and English together where both exist, since the audience reads both. */
    private fun glossOf(word: VocabularyEntity): String = when {
        word.tagalog.isNotBlank() && word.english.isNotBlank() &&
            !word.tagalog.equals(word.kasiguranin, ignoreCase = true) ->
            "${word.tagalog} · ${word.english}"

        word.english.isNotBlank() -> word.english
        else -> word.tagalog
    }
}
