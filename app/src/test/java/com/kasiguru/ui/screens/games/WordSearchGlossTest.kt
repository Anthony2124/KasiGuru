package com.kasiguru.ui.screens.games

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSearchGlossTest {

    private fun word(kasiguranin: String, tagalog: String, english: String = "") =
        VocabularyEntity(kasiguranin = kasiguranin, tagalog = tagalog, english = english)

    @Test
    fun `shows the Tagalog translation`() {
        assertEquals("pato", tagalogGloss(word("bibi", "pato", "duck")))
        assertEquals("langgam", tagalogGloss(word("singët", "langgam", "ant")))
    }

    @Test
    fun `hides a translation spelled the same as the headword`() {
        assertEquals("", tagalogGloss(word("manok", "manok", "chicken")))
        assertEquals("", tagalogGloss(word("Kabayo", "kabayo", "horse")))
        assertEquals("", tagalogGloss(word("singët", "singet")))
        assertEquals("", tagalogGloss(word("kili-kile", "kilikile")))
    }

    @Test
    fun `falls back to English only when there is no Tagalog`() {
        assertEquals("adze", tagalogGloss(word("apak", "", "adze")))
    }
}
