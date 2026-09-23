package com.kasiguru.domain.wordwheel

import com.kasiguru.data.local.DatabaseSeeder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordWheelGeneratorTest {

    /** The corpus a fresh install ships with, so these tests fail if it can no longer fill 30 levels. */
    private val corpus = DatabaseSeeder.getInitialVocabulary()
        .map { WordWheelCandidate(it.id, it.kasiguranin) }
        .let { list -> list.mapIndexed { i, c -> if (c.id == 0) c.copy(id = i + 1) else c } }

    private val dictionaryKeys = WordWheelGenerator.dictionaryOf(corpus).map { it.key }.toSet()

    private val puzzles = (1..WordWheelTier.MAX_LEVEL).associateWith { WordWheelGenerator.buildLevel(corpus, it) }

    @Test
    fun `the seeded corpus fills all 30 levels`() {
        for ((level, puzzle) in puzzles) {
            assertNotNull("level $level", puzzle)
            val tier = WordWheelTier.forLevel(level)
            assertEquals("level $level wheel", tier.letterCount, puzzle!!.wheel.size)
            assertTrue("level $level words", puzzle.slots.size in tier.minBoardWords..tier.maxBoardWords)
        }
    }

    @Test
    fun `every board and bonus word is a real dictionary word spelled from the wheel`() {
        for ((level, puzzle) in puzzles) {
            val words = puzzle!!.slots.map { it.word } + puzzle.bonusWords
            for (word in words) {
                assertTrue("level $level: ${word.word}", word.key in dictionaryKeys)
                assertTrue("level $level: ${word.word}", WordWheelGenerator.spells(puzzle.wheel, word.letters))
            }
        }
    }

    @Test
    fun `the thirty levels use thirty different wheels`() {
        val wheels = puzzles.values.map { it!!.wheel.sorted().joinToString("") }
        assertEquals(wheels.size, wheels.toSet().size)
    }

    @Test
    fun `board words cross on shared letters and never run into each other`() {
        for ((level, puzzle) in puzzles) {
            val board = puzzle!!
            val owners = HashMap<BoardCell, MutableList<BoardSlot>>()
            for (slot in board.slots) {
                slot.cells.forEachIndexed { i, cell ->
                    assertEquals("level $level", slot.word.letters[i], board.letterAt[cell])
                    owners.getOrPut(cell) { mutableListOf() } += slot
                }
            }
            // Shared cells are crossings of one across and one down word, never an overlap.
            for ((cell, slots) in owners) {
                assertTrue("level $level $cell", slots.size <= 2)
                if (slots.size == 2) assertNotEquals("level $level $cell", slots[0].across, slots[1].across)
            }
            // Reading every row and column of the board finds no letter run that is not a slot.
            val slotRuns = board.slots.map { it.cells }.toSet()
            for (run in runsOf(board)) assertTrue("level $level stray run $run", run in slotRuns)
            assertTrue(board.rows <= WordWheelGenerator.MAX_BOARD_SPAN && board.cols <= WordWheelGenerator.MAX_BOARD_SPAN)
        }
    }

    @Test
    fun `the wheel never starts spelling the base word`() {
        for ((level, puzzle) in puzzles) {
            val base = puzzle!!.slots.first().word
            assertFalse("level $level", puzzle.wheel == base.letters)
        }
    }

    @Test
    fun `the same dictionary gives the same level whatever order it arrives in`() {
        val again = WordWheelGenerator.buildLevel(corpus.reversed(), 17)!!
        val first = puzzles.getValue(17)!!
        assertEquals(first.wheel, again.wheel)
        assertEquals(first.slots, again.slots)
    }

    @Test
    fun `an attempt matches its slot or bonus word and nothing else`() {
        val puzzle = puzzles.getValue(1)!!
        val slot = puzzle.slots.last()
        assertEquals(puzzle.slots.indexOf(slot), puzzle.slotFor(slot.word.letters))
        assertNull(puzzle.slotFor(listOf("Q", "Q", "Q")))
        puzzle.bonusWords.firstOrNull()?.let { assertEquals(it, puzzle.bonusFor(it.letters)) }
    }

    @Test
    fun `spells respects how many times each letter appears`() {
        assertTrue(WordWheelGenerator.spells(listOf("T", "A", "B", "I"), listOf("B", "A", "T")))
        assertFalse(WordWheelGenerator.spells(listOf("T", "A", "B", "I"), listOf("T", "A", "T")))
    }

    @Test
    fun `too small a dictionary gives no puzzle rather than a thin one`() {
        val tiny = listOf("tabi", "iba", "bait").mapIndexed { i, w -> WordWheelCandidate(i, w) }
        assertNull(WordWheelGenerator.buildLevel(tiny, 1))
    }

    /** Every maximal horizontal and vertical run of two or more filled cells. */
    private fun runsOf(puzzle: WordWheelPuzzle): List<List<BoardCell>> {
        val filled = puzzle.letterAt.keys
        val runs = mutableListOf<List<BoardCell>>()
        for (across in listOf(true, false)) {
            val outer = if (across) puzzle.rows else puzzle.cols
            val inner = if (across) puzzle.cols else puzzle.rows
            for (o in 0 until outer) {
                var run = mutableListOf<BoardCell>()
                for (i in 0..inner) {
                    val cell = if (across) BoardCell(o, i) else BoardCell(i, o)
                    if (i < inner && cell in filled) {
                        run += cell
                    } else {
                        if (run.size >= 2) runs += run
                        run = mutableListOf()
                    }
                }
            }
        }
        return runs
    }
}
