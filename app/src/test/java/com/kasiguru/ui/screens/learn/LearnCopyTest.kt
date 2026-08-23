package com.kasiguru.ui.screens.learn

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the review count's grammar.
 *
 * "1 words due" shipped on the Learn screen and in the goal ring's spoken description, which is the
 * first line of the app a learner reads each morning. The plural rule lives in one function so the
 * card and the description cannot drift apart again.
 */
class LearnCopyTest {

    @Test
    fun oneWordIsSingular() {
        assertEquals("1 word", wordsToReview(1))
    }

    @Test
    fun everyOtherCountIsPlural() {
        assertEquals("0 words", wordsToReview(0))
        assertEquals("2 words", wordsToReview(2))
        assertEquals("20 words", wordsToReview(20))
    }
}
