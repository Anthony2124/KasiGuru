package com.kasiguru.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards typed-recall grading.
 *
 * Recall is the only exercise that asks a learner to produce a word rather than recognise one, so
 * it carries most of the pedagogical weight — and all of it is lost if the input is fussy enough
 * that people stop typing. These pin the line between "forgave a keyboard problem" and "accepted a
 * wrong answer".
 */
class RecallAnswerMatcherTest {

    @Test
    fun exactAnswerIsExact() {
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("abben", "abben"))
    }

    @Test
    fun caseAndSurroundingSpaceAreIgnored() {
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("  AbBeN ", "abben"))
    }

    @Test
    fun schwaMayBeTypedAsE() {
        // No standard phone keyboard produces a schwa. Requiring it would gate the exercise on the
        // learner's keyboard rather than their memory.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("adeg", "adëg"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("adëg", "adëg"))
    }

    @Test
    fun everySchwaSpellingInTheCorpusFolds() {
        // The corpus writes the schwa three ways and they are three separate codepoints: ë in the
        // headwords, ǝ in a handful of entries, ə in the IPA notation. Folding one is not folding
        // the others, and the first version of this matcher only knew about ə — a character that
        // appears in no headword at all.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("telompulu", "tëlompulu"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("telompulu", "tǝlompulu"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("telompulu", "təlompulu"))
    }

    @Test
    fun aWordWithTwoSchwasIsStillExactWhenTypedWithoutThem() {
        // Reduplicated stems like bëdbëd carry two, which is more than the typo budget for a word
        // that short — so treating the schwa as a typo rather than folding it marked a perfectly
        // recalled word wrong. Twenty entries in the corpus have this shape.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("bedbed", "bëdbëd"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("ennem", "ënnëm"))
    }

    @Test
    fun accentsAreStrippedGenericallyRatherThanFromAList() {
        // Short words get no typo slack at all, so an accent that survives normalisation is fatal
        // rather than merely close: uló is three letters.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("ulo", "uló"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("saan", "saân"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("kasi", "kaśi"))
    }

    @Test
    fun missingAccentsAreAccepted() {
        // Accents mark stress, which a vocabulary recall exercise is not testing.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("bahing", "báhing"))
    }

    @Test
    fun hyphenationIsNotACorrectnessTest() {
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("magaral", "mag-aral"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("mag-aral", "magaral"))
    }

    @Test
    fun aSingleTypoInAMediumWordIsClose() {
        // Retrieved the word, slipped on a key. Correct, but rated conservatively by the caller.
        assertEquals(RecallMatch.Close, RecallAnswerMatcher.match("abbem", "abben"))
    }

    @Test
    fun longWordsTolerateTwoSlips() {
        assertEquals(RecallMatch.Close, RecallAnswerMatcher.match("kasinungalingam", "kasinungalingan"))
    }

    @Test
    fun shortWordsGetNoSlack() {
        // One edit on a three-letter word can land on a different word entirely, so there is no
        // safe budget here — "aso" and "ako" are both real.
        assertEquals(RecallMatch.Wrong, RecallAnswerMatcher.match("ako", "aso"))
    }

    @Test
    fun eitherOfTwoRecordedVariantsCounts() {
        // The corpus records speaker variation in one field: buto/bungaw is two names for one thing,
        // and the dictionary shows the learner both.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("buto", "buto/bungaw"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("bungaw", "buto/bungaw"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("balimbing", "koloran, balimbing"))
    }

    @Test
    fun aTypoInOneVariantIsStillClose() {
        assertEquals(RecallMatch.Close, RecallAnswerMatcher.match("bungow", "buto/bungaw"))
    }

    @Test
    fun aWordThatIsNeitherVariantIsWrong() {
        assertEquals(RecallMatch.Wrong, RecallAnswerMatcher.match("magaral", "buto/bungaw"))
    }

    @Test
    fun aDifferentWordIsWrong() {
        assertEquals(RecallMatch.Wrong, RecallAnswerMatcher.match("bahay", "abben"))
    }

    @Test
    fun emptyInputIsWrongRatherThanClose() {
        // Guards a real edge: without this, the empty string is within edit distance 2 of any
        // one- or two-letter answer, so submitting nothing would count as nearly right.
        assertEquals(RecallMatch.Wrong, RecallAnswerMatcher.match("", "ay"))
        assertEquals(RecallMatch.Wrong, RecallAnswerMatcher.match("   ", "abben"))
    }

    @Test
    fun internalWhitespaceIsCollapsedNotStripped() {
        // Multi-word entries exist in the corpus ("anak anak"), and the space is meaningful.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("anak   anak", "anak anak"))
        assertEquals(RecallMatch.Wrong, RecallAnswerMatcher.match("anak anak", "bahay bahay"))
    }
}
