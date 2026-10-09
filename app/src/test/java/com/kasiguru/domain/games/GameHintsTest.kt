package com.kasiguru.domain.games

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameHintsTest {

    private fun word(kasiguranin: String, tagalog: String, english: String) =
        VocabularyEntity(kasiguranin = kasiguranin, tagalog = tagalog, english = english)

    @Test
    fun `word search lists each word with the first line of its meaning`() {
        val hint = wordSearchHint(listOf("bale" to "A building people live in.\nGusali na tinitirhan.", "aso" to null))
        assertEquals("bale — A building people live in.", hint)
    }

    @Test
    fun `word search has no hint when no word has a meaning`() {
        assertNull(wordSearchHint(listOf("bale" to null, "aso" to " ")))
    }

    @Test
    fun `word wheel gives meanings and lengths but never the hidden word`() {
        val hint = wordWheelHint(listOf(4 to "A building people live in.", 3 to null))
        assertEquals("4 letters — A building people live in.", hint)
        assertNull(wordWheelHint(listOf(3 to null)))
    }

    @Test
    fun `sentence hint glosses known words in order and skips the rest`() {
        val dictionary = listOf(word("bëbbi", "babae", "woman"), word("dalaga", "dalaga", "young woman"))
            .associateBy { it.kasiguranin }
        val hint = sentenceHint(listOf("Bëbbi", "ya", "dalaga.")) { dictionary[it] }
        // Tagalog first; English where the Tagalog is spelled like the Kasiguranin.
        assertEquals("Bëbbi — babae\ndalaga — young woman", hint)
    }

    @Test
    fun `sentence hint is null when no word is in the dictionary`() {
        assertNull(sentenceHint(listOf("ya", "ti")) { null })
    }

    @Test
    fun `sentence tokens keep the schwa and hyphens but lose punctuation`() {
        assertEquals("bëbbi", sentenceToken("Bëbbi,"))
        assertEquals("sagik-rom", sentenceToken("\"sagik-rom\""))
    }
}
