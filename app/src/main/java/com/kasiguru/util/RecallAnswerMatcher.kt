package com.kasiguru.util

import kotlin.math.min

/**
 * How a typed answer compared to the expected word.
 *
 * Three outcomes rather than two, because "almost right" is genuinely different from both. A
 * learner who types `kagat` for `kagət` retrieved the word — they are wrong about the orthography,
 * not about the vocabulary — and marking that a failure would reset the review schedule of a word
 * they actually knew, while marking it a clean success would let the spelling error harden.
 */
enum class RecallMatch {
    /** Exactly the expected word, once case and spacing are normalised. */
    Exact,

    /** Recognisably the word, with a small slip: a typo, a missing accent, or `e` for `ə`. */
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
     * Vowels that appear accented in the corpus, mapped to their bare forms.
     *
     * Accents mark stress, and stress is not what a recall exercise is testing. Requiring them
     * would fail learners on a diacritic most phone keyboards bury three long-presses deep.
     */
    private val ACCENT_FOLDING = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u'
    )

    /**
     * The schwa, folded to `e`.
     *
     * `ə` is a real Kasiguranin letter, not an accent, so folding it is a genuine concession rather
     * than a normalisation — but no standard phone keyboard produces it. Demanding a character the
     * learner physically cannot type is a barrier, not a desirable difficulty. Callers show the
     * correct spelling in feedback so the orthography is still taught; it just is not the gate.
     */
    private const val SCHWA = 'ə'

    fun match(typed: String, expected: String): RecallMatch {
        val a = normalise(typed)
        val b = normalise(expected)

        if (a.isEmpty()) return RecallMatch.Wrong
        if (a == b) return RecallMatch.Exact

        return if (levenshtein(a, b) <= allowedSlips(b)) RecallMatch.Close else RecallMatch.Wrong
    }

    /**
     * Lowercase, trimmed, internal whitespace collapsed, accents and schwa folded, and hyphens
     * dropped — hyphenation in the corpus is inconsistent enough that it cannot be a correctness
     * test.
     */
    fun normalise(raw: String): String = buildString {
        for (ch in raw.trim().lowercase()) {
            when {
                ch == '-' -> {}
                ch.isWhitespace() -> if (isNotEmpty() && last() != ' ') append(' ')
                ch == SCHWA -> append('e')
                ACCENT_FOLDING.containsKey(ch) -> append(ACCENT_FOLDING[ch])
                else -> append(ch)
            }
        }
    }.trim()

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
