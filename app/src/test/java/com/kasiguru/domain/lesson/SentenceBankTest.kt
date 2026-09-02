package com.kasiguru.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the app's only running Kasiguranin.
 *
 * The vocabulary corpus carries five example sentences across 1,246 entries and every seeded story
 * page leaves its Kasiguranin blank on purpose, so these fifteen authored sentences are the whole of
 * it. Anything that asks a learner to arrange Kasiguranin words draws from here — which makes it
 * worth asserting that the bank stays intact and stays matched to real headwords.
 */
class SentenceBankTest {

    @Test
    fun theBankIsPresentAndEverySentenceIsUsable() {
        assertTrue("the sentence bank must not be empty", SentenceBank.sentences.isNotEmpty())

        SentenceBank.sentences.forEach { sentence ->
            assertTrue(
                "every sentence needs its English meaning: ${sentence.text}",
                sentence.english.isNotBlank()
            )
            assertTrue(
                "a sentence of one word cannot be arranged: ${sentence.text}",
                sentence.kasiguranin.size >= 2
            )
        }
    }

    @Test
    fun aWordIsFoundEvenWhenTheSentenceEndsOnIt() {
        // "Tinumáknəg ang anák." stores the last word with its full stop attached. A learner's
        // headword never carries one, so matching without normalising would miss every sentence-final
        // word -- which is a third of the bank.
        val found = SentenceBank.sentenceUsing("anák")
        assertNotNull("a headword should match a sentence-final token", found)
        assertTrue(found!!.kasiguranin.any { SentenceBank.normalise(it) == "anák" })
    }

    @Test
    fun matchingIgnoresCase() {
        assertNotNull(SentenceBank.sentenceUsing("KUMUSTA"))
    }

    @Test
    fun aWordInNoSentenceReturnsNothingRatherThanSomethingClose() {
        // The rule the whole feature rests on: no sentence is assembled for a word that has none.
        assertNull(SentenceBank.sentenceUsing("zzzznotaword"))
        assertNull(SentenceBank.sentenceUsing(""))
    }

    @Test
    fun normaliseStripsOnlyTheEdges() {
        assertEquals("anák", SentenceBank.normalise("Anák."))
        assertEquals("mag-uden", SentenceBank.normalise("Mag-uden"))
        assertEquals("ttanan", SentenceBank.normalise("'ttanan!"))
    }
}
