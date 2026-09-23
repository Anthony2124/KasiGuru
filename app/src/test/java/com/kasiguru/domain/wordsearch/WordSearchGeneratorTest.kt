package com.kasiguru.domain.wordsearch

import com.kasiguru.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchGeneratorTest {

    // Real entries from the Animals & Wildlife category.
    private val animals = listOf(
        "hayop", "singët", "ibon", "tulang", "sepsep", "manok", "ipës", "buwaya", "wakwak",
        "ogsa", "aso", "bibi", "lamok", "uwak", "pusit", "kabayo", "pating", "kuwago", "tukak",
        "kili-kile", "bunay ng sida"
    ).mapIndexed { i, w -> WordSearchCandidate(i + 1, w) }

    @Test
    fun `every level from 1 to 30 builds a full puzzle from a real category`() {
        for (level in 1..WordSearchTier.MAX_LEVEL) {
            val tier = WordSearchTier.forLevel(level)
            val generated = WordSearchGenerator.generate(animals, level, seed = 42L + level)
            assertNotNull("level $level", generated)
            val puzzle = generated!!
            assertEquals(tier.gridSize, puzzle.size)
            assertEquals("level $level", tier.wordCount, puzzle.words.size)
            assertTrue(puzzle.grid.all { row -> row.size == tier.gridSize && row.all { it.isNotEmpty() } })
        }
    }

    @Test
    fun `each placed word is spelled by its cells and matched from either end`() {
        val puzzle = WordSearchGenerator.generate(animals, level = 25, seed = 7L)!!
        for (word in puzzle.words) {
            assertEquals(word.letters, word.cells.map(puzzle::letterAt))
            assertEquals(word.id, puzzle.match(word.cells.first(), word.cells.last(), emptySet())?.id)
            assertEquals(word.id, puzzle.match(word.cells.last(), word.cells.first(), emptySet())?.id)
        }
    }

    @Test
    fun `a found word does not match twice`() {
        val puzzle = WordSearchGenerator.generate(animals, level = 1, seed = 3L)!!
        val word = puzzle.words.first()
        assertNull(puzzle.match(word.cells.first(), word.cells.last(), setOf(word.id)))
    }

    @Test
    fun `words only run in the directions their tier allows`() {
        for (level in listOf(1, 12, 28)) {
            val tier = WordSearchTier.forLevel(level)
            val puzzle = WordSearchGenerator.generate(animals, level, seed = 11L)!!
            for (word in puzzle.words) {
                val dRow = Integer.signum(word.cells[1].row - word.cells[0].row)
                val dCol = Integer.signum(word.cells[1].col - word.cells[0].col)
                assertTrue(tier.directions.any { it.dRow == dRow && it.dCol == dCol })
            }
        }
    }

    @Test
    fun `the same seed gives the same puzzle whatever order the words arrive in`() {
        val a = WordSearchGenerator.generate(animals, level = 15, seed = 99L)!!
        val b = WordSearchGenerator.generate(animals.reversed(), level = 15, seed = 99L)!!
        assertEquals(a.grid, b.grid)
        assertEquals(a.words.map { it.id }, b.words.map { it.id })
    }

    @Test
    fun `ë is one tile, hyphens are dropped, and words with spaces are skipped`() {
        assertEquals(listOf("S", "I", "N", "G", "Ë", "T"), WordSearchGenerator.letters("singët"))
        // Decomposed form: e followed by a combining diaeresis.
        assertEquals(listOf("I", "P", "Ë", "S"), WordSearchGenerator.letters("ipës"))
        assertEquals("KILIKILE", WordSearchGenerator.letters("kili-kile")!!.joinToString(""))
        assertNull(WordSearchGenerator.letters("bunay ng sida"))
    }

    @Test
    fun `a line that is not straight matches nothing`() {
        val puzzle = WordSearchGenerator.generate(animals, level = 1, seed = 5L)!!
        assertNull(puzzle.lineBetween(GridCell(0, 0), GridCell(1, 2)))
        assertNull(puzzle.match(GridCell(0, 0), GridCell(1, 2), emptySet()))
    }

    @Test
    fun `too few usable words gives no puzzle rather than a thin one`() {
        val few = listOf("aso", "ibon").mapIndexed { i, w -> WordSearchCandidate(i, w) }
        assertNull(WordSearchGenerator.generate(few, level = 1, seed = 1L))
    }

    @Test
    fun `level keys are route-safe and map back to their category`() {
        for (category in Constants.VocabCategories.ALL) {
            val key = Constants.Games.wordSearchLevelKey(category)
            assertTrue(key, key.matches(Regex("word_search_[a-z0-9_]+")))
            assertTrue(Constants.Games.isWordSearchLevelKey(key))
            assertEquals(category, Constants.Games.categoryForWordSearchKey(key))
        }
        assertEquals(
            Constants.VocabCategories.ALL.size,
            Constants.VocabCategories.ALL.map(Constants.Games::wordSearchLevelKey).toSet().size
        )
    }
}
