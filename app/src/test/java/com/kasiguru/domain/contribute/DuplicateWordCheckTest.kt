package com.kasiguru.domain.contribute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The corpus this exercises against is a handful of real entries, chosen because each one is a case
 * the check has to get right: the three senses of `baga`, a schwa word no phone keyboard can type,
 * an entry recording two names for one thing, and a short word with near neighbours.
 */
class DuplicateWordCheckTest {

    private val corpus = listOf(
        ExistingEntry("baga", "baga", "lungs", "Body & Health"),
        ExistingEntry("baga", "namamaga", "swollen", "Body & Health"),
        ExistingEntry("baga", "baga", "ember", "Nature & Environment"),
        ExistingEntry("singët", "amoy", "smell", "Body & Health"),
        ExistingEntry("buto/bungaw", "buto", "bone", "Body & Health"),
        ExistingEntry("apak", "daras", "adze", "Tools & Objects"),
        ExistingEntry("apat", "apat", "four", "Numbers"),
        ExistingEntry("magalakaw", "maglakad", "to walk", "Actions & Verbs")
    )

    @Test
    fun `a word with no counterpart in the dictionary raises nothing`() {
        assertTrue(DuplicateWordCheck.find("dalampasigan", "shoreline", corpus).isEmpty())
    }

    @Test
    fun `a blank word raises nothing`() {
        assertTrue(DuplicateWordCheck.find("   ", "lungs", corpus).isEmpty())
    }

    @Test
    fun `an identical headword and gloss is flagged as the same sense`() {
        val matches = DuplicateWordCheck.find("baga", "lungs", corpus)
        assertEquals(DuplicateLevel.SameSense, matches.first().level)
        assertEquals("lungs", matches.first().entry.english)
    }

    @Test
    fun `the matching sense is listed first, ahead of the word's homonyms`() {
        val matches = DuplicateWordCheck.find("baga", "ember", corpus)
        assertEquals("ember", matches.first().entry.english)
        assertEquals(DuplicateLevel.SameSense, matches.first().level)
        assertTrue(matches.drop(1).all { it.level == DuplicateLevel.SameWord })
    }

    @Test
    fun `a new sense of a recorded word is flagged as the same word, not the same sense`() {
        val matches = DuplicateWordCheck.find("baga", "hot coal in a fire", corpus)
        assertEquals(3, matches.size)
        assertTrue(matches.all { it.level == DuplicateLevel.SameWord })
    }

    @Test
    fun `a word typed without its schwa still matches the recorded spelling`() {
        // No phone keyboard produces `ë`, so this is how the duplicate would actually be typed.
        val matches = DuplicateWordCheck.find("singet", "smell", corpus)
        assertEquals(1, matches.size)
        assertEquals(DuplicateLevel.SameSense, matches.first().level)
    }

    @Test
    fun `either name of an entry recording two of them matches that entry`() {
        val matches = DuplicateWordCheck.find("bungaw", "bone", corpus)
        assertEquals(1, matches.size)
        assertEquals(DuplicateLevel.SameSense, matches.first().level)
        assertEquals("buto/bungaw", matches.first().entry.kasiguranin)
    }

    @Test
    fun `a near neighbour of a short word is not reported as similar`() {
        // `apak` and `apat` are one edit apart and completely unrelated. Fuzzy matching that short
        // would fill the notice with words the contributor is plainly not duplicating.
        val matches = DuplicateWordCheck.find("apak", "adze", corpus)
        assertEquals(1, matches.size)
        assertEquals("apak", matches.first().entry.kasiguranin)
    }

    @Test
    fun `a near-miss spelling of a longer word is reported as similar`() {
        val matches = DuplicateWordCheck.find("magalakau", "to walk", corpus)
        assertEquals(1, matches.size)
        assertEquals(DuplicateLevel.SimilarSpelling, matches.first().level)
    }

    @Test
    fun `a blank gloss leaves every headword match at the same word level`() {
        val matches = DuplicateWordCheck.find("baga", "", corpus)
        assertEquals(3, matches.size)
        assertTrue(matches.all { it.level == DuplicateLevel.SameWord })
    }

    @Test
    fun `only an identical headword gates the submit button`() {
        assertTrue(DuplicateWordCheck.needsConfirmation(DuplicateWordCheck.find("baga", "lungs", corpus)))
        assertTrue(DuplicateWordCheck.needsConfirmation(DuplicateWordCheck.find("baga", "a new sense", corpus)))
        assertFalse(DuplicateWordCheck.needsConfirmation(DuplicateWordCheck.find("magalakau", "to walk", corpus)))
        assertFalse(DuplicateWordCheck.needsConfirmation(emptyList()))
    }
}
