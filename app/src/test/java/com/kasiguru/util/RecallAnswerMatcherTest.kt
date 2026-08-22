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
        // No standard phone keyboard produces 'ə'. Requiring it would gate the exercise on the
        // learner's keyboard rather than their memory.
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("kaget", "kagət"))
        assertEquals(RecallMatch.Exact, RecallAnswerMatcher.match("kagət", "kagət"))
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
