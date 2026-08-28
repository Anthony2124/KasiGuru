package com.kasiguru.domain.contribute

import com.kasiguru.util.RecallAnswerMatcher
import com.kasiguru.util.RecallMatch

/**
 * One dictionary entry a contributed word might be repeating.
 *
 * Deliberately not a [com.kasiguru.data.local.entity.VocabularyEntity]: nothing the notice shows
 * needs the SM-2 schedule, and keeping the check over a plain record is what lets it be tested
 * without a database.
 */
data class ExistingEntry(
    val kasiguranin: String,
    val tagalog: String,
    val english: String,
    val category: String
)

/**
 * How closely a contributed word already exists in the dictionary.
 *
 * Three levels rather than a boolean, because "already there" is not one situation. A word whose
 * headword *and* gloss are already recorded is a plain repeat. A word whose headword is recorded
 * under a different meaning may well be a new sense the dictionary is missing — Kasiguranin has
 * real homonyms, and [com.kasiguru.data.local.dao.VocabularyDao.deleteDuplicateWords] carries the
 * scar of a de-duplication that ignored this and silently destroyed fourteen of them. And a word
 * that merely resembles an existing spelling is worth showing without asserting anything at all.
 */
enum class DuplicateLevel {
    /** Same headword, same English gloss. Almost certainly already in the dictionary. */
    SameSense,

    /** Same headword, a different meaning. Plausibly a new sense worth submitting. */
    SameWord,

    /** Not the same headword, but close enough in spelling to be worth a look. */
    SimilarSpelling
}

/** An existing entry the submission may be duplicating, and how strong the resemblance is. */
data class DuplicateMatch(
    val entry: ExistingEntry,
    val level: DuplicateLevel
)

/**
 * Finds dictionary entries a contributed word may be repeating.
 *
 * Contributions arrive faster than an admin can compare them against roughly twelve hundred
 * existing entries by hand, so the same handful of common words were being submitted, reviewed and
 * rejected over and over. The form itself is the cheapest place to catch that: the contributor
 * knows what they meant, and can tell in a glance whether the entry already shown to them is their
 * word or a homonym of it.
 *
 * Matching is done on [RecallAnswerMatcher.normalise] rather than on raw text. The schwa alone has
 * three spellings in the corpus (`ë`, `ə`, `ǝ`) and no phone keyboard produces any of them, so a
 * contributor typing `singet` for `singët` would otherwise sail past an exact copy of their word —
 * which is precisely the duplicate this exists to catch. That normalisation also folds case,
 * hyphens and stress accents, all of which the corpus is inconsistent about.
 */
object DuplicateWordCheck {

    /** At most this many merely-similar spellings are surfaced; exact matches are never trimmed. */
    private const val MAX_SIMILAR = 3

    /**
     * Below this many characters, no similarity search.
     *
     * [RecallAnswerMatcher] allows one edit on a four-letter word, which is right when grading a
     * typed answer against one known target but wrong when sweeping the whole corpus: `apak` is one
     * edit from `apat`, `anak` and `alak`, none of which the contributor is duplicating. Short words
     * are still checked for exact matches, which is where the real duplicates are.
     */
    private const val MIN_SIMILARITY_LENGTH = 5

    /**
     * @param word the Kasiguranin headword as typed.
     * @param english the English gloss as typed; used only to tell a repeat from a new sense, so a
     *   blank one simply leaves every headword match at [DuplicateLevel.SameWord].
     * @param corpus every entry currently in the on-device dictionary.
     */
    fun find(word: String, english: String, corpus: List<ExistingEntry>): List<DuplicateMatch> {
        val typed = RecallAnswerMatcher.normalise(word)
        if (typed.isEmpty()) return emptyList()
        val typedGloss = RecallAnswerMatcher.normalise(english)

        val exact = mutableListOf<DuplicateMatch>()
        val similar = mutableListOf<DuplicateMatch>()

        for (entry in corpus) {
            // Graded by the same matcher the recall exercises use, which splits an entry recording
            // two names for one thing (`buto/bungaw`) and treats either as the headword. Forty
            // entries carry such alternates, and a contributor submitting one of them is repeating
            // that entry as surely as if they had typed the whole string.
            when (RecallAnswerMatcher.match(word, entry.kasiguranin)) {
                RecallMatch.Exact -> {
                    val sameSense = typedGloss.isNotEmpty() &&
                        RecallAnswerMatcher.normalise(entry.english) == typedGloss
                    exact += DuplicateMatch(
                        entry,
                        if (sameSense) DuplicateLevel.SameSense else DuplicateLevel.SameWord
                    )
                }
                RecallMatch.Close -> {
                    if (typed.length >= MIN_SIMILARITY_LENGTH) {
                        similar += DuplicateMatch(entry, DuplicateLevel.SimilarSpelling)
                    }
                }
                RecallMatch.Wrong -> {}
            }
        }

        // Same-sense repeats lead: if the contributor's exact entry is already recorded, that is the
        // one sentence they need to read, not the third homonym down the list.
        exact.sortBy { it.level.ordinal }
        return exact + similar.take(MAX_SIMILAR)
    }

    /**
     * Whether [matches] warrant asking the contributor to confirm before the submission is sent.
     *
     * Only an identical headword does. A similar spelling is shown but never gates the button — the
     * form must not stand between a speaker and a word the dictionary does not have, and a false
     * positive on a fuzzy match would do exactly that.
     */
    fun needsConfirmation(matches: List<DuplicateMatch>): Boolean =
        matches.any { it.level != DuplicateLevel.SimilarSpelling }
}
