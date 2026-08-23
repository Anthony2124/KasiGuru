package com.kasiguru.util

import java.text.Normalizer
import kotlin.math.min

/**
 * How a typed answer compared to the expected word.
 *
 * Three outcomes rather than two, because "almost right" is genuinely different from both. A
 * learner who types `adeg` for `adëg` retrieved the word — they are wrong about the orthography,
 * not about the vocabulary — and marking that a failure would reset the review schedule of a word
 * they actually knew, while marking it a clean success would let the spelling error harden.
 */
enum class RecallMatch {
    /** Exactly the expected word, once case and spacing are normalised. */
    Exact,

    /** Recognisably the word, with a small slip: a typo, or a missing accent. */
    Close,

    /** Not the word. */
    Wrong
}

/**
 * Grades a typed recall answer.
 *
 * Recall is the point of the exercise: producing a word from memory is a far stronger test than
 * picking it out of four options, and it is the only thing in the app that asks for production.
 * That value disappears if the input is fussy, so the comparison forgives everything that is not
 * about knowing the word.
 */
object RecallAnswerMatcher {

    /**
     * The schwa, in every spelling the corpus uses, folded to `e`.
     *
     * This is a concession rather than a normalisation — the schwa is a real Kasiguranin letter, not
     * an accent — but no standard phone keyboard produces any of these characters, and demanding one
     * the learner physically cannot type is a barrier, not a desirable difficulty. Callers show the
     * correct spelling in feedback so the orthography is still taught; it just is not the gate.
     *
     * All three appear: the corpus overwhelmingly writes the schwa as `ë` (218 of its entries), with
     * a handful of `ǝ` (turned e) entries, while `ə` is the IPA character the notation field uses.
     * They are separate codepoints, so folding one is not folding the others.
     */
    private val SCHWA_FORMS = setOf('ë', 'ə', 'ǝ')

    /** Matches the combining marks left behind by NFD decomposition. */
    private val COMBINING_MARKS = Regex("""\p{Mn}+""")

    /**
     * Separators the corpus uses to record two names for one thing.
     *
     * Forty entries carry alternates (`buto/bungaw`, `koloran, balimbing`), and both names are the
     * word — the thesis recorded variation between speakers, not a preferred form and a rejected
     * one. Grading against the whole string would fail a learner who typed one of the answers the
     * dictionary itself gives them.
     */
    private val ALTERNATE_SEPARATORS = Regex("[/,]")

    fun match(typed: String, expected: String): RecallMatch {
        val a = normalise(typed)
        if (a.isEmpty()) return RecallMatch.Wrong

        val alternatives = expected.split(ALTERNATE_SEPARATORS)
            .map(::normalise)
            .filter { it.isNotEmpty() }

        var best = RecallMatch.Wrong
        for (b in alternatives) {
            if (a == b) return RecallMatch.Exact
            if (levenshtein(a, b) <= allowedSlips(b)) best = RecallMatch.Close
        }
        return best
    }

    /**
     * Lowercase, trimmed, internal whitespace collapsed, hyphens dropped, the schwa folded to `e`,
     * and every remaining diacritic decomposed away.
     *
     * Hyphenation in the corpus is inconsistent enough that it cannot be a correctness test
     * (`tël-lën` and `tëllën` are both recorded), and the accents mark stress, which is not what a
     * recall exercise is testing. Stripping marks generically rather than listing the accented
     * vowels matters: the corpus carries `á â é ë í ó ý ś`, and a hand-written list will always be
     * one letter behind the linguists.
     */
    fun normalise(raw: String): String {
        val folded = buildString {
            for (ch in raw.trim().lowercase()) {
                when {
                    ch == '-' -> {}
                    ch.isWhitespace() -> if (isNotEmpty() && last() != ' ') append(' ')
                    ch in SCHWA_FORMS -> append('e')
                    else -> append(ch)
                }
            }
        }
        val decomposed = Normalizer.normalize(folded, Normalizer.Form.NFD)
        return COMBINING_MARKS.replace(decomposed, "").trim()
    }

    /**
     * Edit distance tolerated, scaled to length.
     *
     * A fixed budget is wrong at both ends: one edit on a three-letter word can turn it into a
     * different word entirely, while one slip in an eleven-letter word is obviously a typo. Short
     * words therefore get no slack at all.
     */
    private fun allowedSlips(expected: String): Int = when {
        expected.length <= 3 -> 0
        expected.length <= 7 -> 1
        else -> 2
    }

    /** Standard Levenshtein distance, two-row variant. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(min(current[j - 1] + 1, previous[j] + 1), substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
