package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Home, the Library and the morning notification must feature the same word on the same day. */
class WordOfDayTest {

    private fun word(id: Int, form: String = "w$id", gloss: String = "g$id") =
        VocabularyEntity(id = id, kasiguranin = form, tagalog = gloss)

    @Test
    fun theSameDayAlwaysPicksTheSameWordWhateverTheListOrder() {
        val words = listOf(word(3), word(1), word(2))
        val day = LocalDate.of(2026, 10, 6)
        assertEquals(WordOfDay.pick(words, day), WordOfDay.pick(words.reversed(), day))
    }

    @Test
    fun theNextDayMovesOnToTheNextWord() {
        val words = listOf(word(1), word(2), word(3))
        val day = LocalDate.of(2026, 10, 6)
        val today = WordOfDay.pick(words, day)!!.id
        val tomorrow = WordOfDay.pick(words, day.plusDays(1))!!.id
        assertEquals(today % 3 + 1, tomorrow)
    }

    @Test
    fun wordsWithoutAFormOrAGlossAreNeverFeatured() {
        val words = listOf(word(1, form = ""), word(2, gloss = ""), word(3))
        repeat(5) { offset ->
            assertEquals(3, WordOfDay.pick(words, LocalDate.of(2026, 10, 1).plusDays(offset.toLong()))!!.id)
        }
    }

    @Test
    fun anEmptyCorpusHasNoWord() {
        assertNull(WordOfDay.pick(emptyList()))
    }
}
