package com.kasiguru.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the one way a recall prompt can be worthless: showing the answer.
 */
class RecallPromptTest {

    @Test
    fun tagalogIsPreferredWhenItDiffersFromTheHeadword() {
        assertEquals("daras", RecallPrompt.meaningFor("apak", "daras", "adze"))
    }

    @Test
    fun englishTakesOverWhenTheTagalogGlossIsTheHeadword() {
        assertEquals(
            "what-you-may-call-it",
            RecallPrompt.meaningFor("kuwan", "kuwan", "what-you-may-call-it")
        )
    }

    @Test
    fun aGiveawayIsCaughtThroughTheSameNormalisationTheAnswerUses() {
        // Case, accents and the schwa all fold, so none of them can smuggle the headword through.
        assertNull(RecallPrompt.meaningFor("kagət", "Kagét", ""))
    }

    @Test
    fun blanksAreSkipped() {
        assertEquals("alive", RecallPrompt.meaningFor("buhay", "", "alive"))
        assertNull(RecallPrompt.meaningFor("buhay", "", ""))
    }

    @Test
    fun aWordThatIsItsOwnOnlyGlossCannotBeAsked() {
        assertNull(RecallPrompt.meaningFor("buhay", "buhay", "buhay"))
    }
}
