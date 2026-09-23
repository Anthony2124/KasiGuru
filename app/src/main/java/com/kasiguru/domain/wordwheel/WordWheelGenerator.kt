package com.kasiguru.domain.wordwheel

import com.kasiguru.domain.wordsearch.WordSearchGenerator
import kotlin.random.Random

/**
 * Word Wheel: the letters of one Kasiguranin word arranged on a wheel, and a small crossword of the
 * other dictionary words those letters spell.
 *
 * Every word on the board, and every bonus word, is an entry in the dictionary. Nothing is generated
 * letter by letter; PRODUCT.md is explicit that Kasiguranin is never invented to fill a gap. That is
 * why the game has one 30-level track over the whole dictionary rather than one per category: most
 * categories do not hold thirty letter sets that each spell several real words.
 *
 * Pure Kotlin, no Android or Room types, so the puzzle model is unit-tested against the real corpus.
 */

data class WordWheelTier(
    val name: String,
    /** Letters on the wheel, which is also the length of the level's base word. */
    val letterCount: Int,
    val minBoardWords: Int,
    val maxBoardWords: Int
) {
    companion object {
        const val MAX_LEVEL = 30
        const val LEVELS_PER_TIER = 10

        fun forLevel(level: Int): WordWheelTier = when {
            level <= 10 -> WordWheelTier("Easy", 5, 4, 5)
            level <= 20 -> WordWheelTier("Medium", 6, 5, 6)
            else -> WordWheelTier("Hard", 7, 6, 8)
        }
    }
}

/** A dictionary word offered to the generator; [id] is the vocabulary row id. */
data class WordWheelCandidate(val id: Int, val word: String)

data class WheelWord(
    val id: Int,
    /** As the dictionary spells it, for display. */
    val word: String,
    /** One uppercase tile per letter (`Ë` is one tile), hyphens dropped. */
    val letters: List<String>
) {
    val key: String get() = letters.joinToString("")
}

data class BoardCell(val row: Int, val col: Int)

data class BoardSlot(val word: WheelWord, val row: Int, val col: Int, val across: Boolean) {
    val cells: List<BoardCell>
        get() = word.letters.indices.map { if (across) BoardCell(row, col + it) else BoardCell(row + it, col) }
}

class WordWheelPuzzle(
    /** The wheel's letters in their starting order. */
    val wheel: List<String>,
    val slots: List<BoardSlot>,
    val rows: Int,
    val cols: Int,
    /** Dictionary words the wheel also spells that did not fit on the board. */
    val bonusWords: List<WheelWord>
) {
    val letterAt: Map<BoardCell, String> =
        slots.flatMap { slot -> slot.cells.zip(slot.word.letters) }.toMap()

    /** Index of the board slot spelled by [attempt], or null. */
    fun slotFor(attempt: List<String>): Int? =
        slots.indexOfFirst { it.word.letters == attempt }.takeIf { it >= 0 }

    fun bonusFor(attempt: List<String>): WheelWord? =
        bonusWords.firstOrNull { it.letters == attempt }
}

object WordWheelGenerator {

    const val MIN_WORD_LENGTH = 3

    /** Rows and columns the board may span; beyond this the tiles get too small to read on a phone. */
    const val MAX_BOARD_SPAN = 10

    private const val ACROSS = 1
    private const val DOWN = 2

    /**
     * Builds the puzzle for [level] from the whole dictionary, or null when the dictionary cannot
     * supply one (a tiny or partly-synced corpus).
     *
     * Deterministic: bases are ordered by a hash of their letters rather than by row id or insertion
     * order, so the same dictionary always gives the same level, and a replay is the same puzzle.
     * Level n takes the n-th base in its tier that yields a valid board, so the ten levels of a tier
     * are ten different words.
     */
    fun buildLevel(candidates: List<WordWheelCandidate>, level: Int): WordWheelPuzzle? {
        val tier = WordWheelTier.forLevel(level)
        val dictionary = dictionaryOf(candidates)

        val bases = dictionary
            .filter { it.letters.size == tier.letterCount }
            // Anagrams share one wheel; keep one so two levels are never the same letters.
            .distinctBy { it.letters.sorted().joinToString("") }
            .sortedWith(compareBy<WheelWord> { stableHash(it.key) }.thenBy { it.key })

        val wanted = (level - 1) % WordWheelTier.LEVELS_PER_TIER
        var valid = 0
        for (base in bases) {
            val spelled = dictionary.filter { spells(base.letters, it.letters) }
            if (spelled.size < tier.minBoardWords) continue
            val slots = layout(base, spelled, tier) ?: continue
            if (valid++ < wanted) continue

            val placedKeys = slots.map { it.word.key }.toSet()
            return WordWheelPuzzle(
                wheel = shuffledWheel(base),
                slots = slots,
                rows = slots.maxOf { s -> s.cells.maxOf { it.row } } + 1,
                cols = slots.maxOf { s -> s.cells.maxOf { it.col } } + 1,
                bonusWords = spelled.filter { it.key !in placedKeys }
            )
        }
        return null
    }

    /** Usable dictionary words, one per spelling, lowest id first so meanings come from a stable row. */
    fun dictionaryOf(candidates: List<WordWheelCandidate>): List<WheelWord> =
        candidates
            .sortedBy { it.id }
            .mapNotNull { c ->
                WordSearchGenerator.letters(c.word)
                    ?.takeIf { it.size >= MIN_WORD_LENGTH }
                    ?.let { WheelWord(c.id, c.word.trim(), it) }
            }
            .distinctBy { it.key }

    /** True when [word] can be spelled from [wheel], each wheel letter used at most once. */
    fun spells(wheel: List<String>, word: List<String>): Boolean {
        if (word.size > wheel.size) return false
        val available = wheel.groupingBy { it }.eachCount().toMutableMap()
        for (letter in word) {
            val left = available[letter] ?: 0
            if (left == 0) return false
            available[letter] = left - 1
        }
        return true
    }

    /**
     * Greedy crossword layout: the base word across, then the longest remaining words, each placed
     * where it crosses the board on a shared letter and touches nothing else. Returns null if fewer
     * than the tier's minimum fit; words that do not fit become bonus words.
     */
    private fun layout(base: WheelWord, spelled: List<WheelWord>, tier: WordWheelTier): List<BoardSlot>? {
        val letters = HashMap<BoardCell, String>()
        val directions = HashMap<BoardCell, Int>()
        val placed = mutableListOf<BoardSlot>()

        fun put(slot: BoardSlot) {
            val bit = if (slot.across) ACROSS else DOWN
            slot.cells.forEachIndexed { i, cell ->
                letters[cell] = slot.word.letters[i]
                directions[cell] = (directions[cell] ?: 0) or bit
            }
            placed += slot
        }

        fun fits(slot: BoardSlot): Boolean {
            val bit = if (slot.across) ACROSS else DOWN
            val cells = slot.cells
            val before = if (slot.across) BoardCell(slot.row, slot.col - 1) else BoardCell(slot.row - 1, slot.col)
            val after = if (slot.across) BoardCell(slot.row, slot.col + cells.size) else BoardCell(slot.row + cells.size, slot.col)
            if (before in letters || after in letters) return false
            var crossings = 0
            cells.forEachIndexed { i, cell ->
                val existing = letters[cell]
                if (existing != null) {
                    if (existing != slot.word.letters[i]) return false
                    if (((directions[cell] ?: 0) and bit) != 0) return false
                    crossings++
                } else {
                    // A new letter may not sit beside another word's letter, or the two would read
                    // as an unintended word running the other way.
                    val sides = if (slot.across) {
                        listOf(BoardCell(cell.row - 1, cell.col), BoardCell(cell.row + 1, cell.col))
                    } else {
                        listOf(BoardCell(cell.row, cell.col - 1), BoardCell(cell.row, cell.col + 1))
                    }
                    if (sides.any { it in letters }) return false
                }
            }
            if (crossings == 0) return false
            val all = letters.keys + cells
            return all.maxOf { it.row } - all.minOf { it.row } < MAX_BOARD_SPAN &&
                all.maxOf { it.col } - all.minOf { it.col } < MAX_BOARD_SPAN
        }

        fun area(slot: BoardSlot): Int {
            val all = letters.keys + slot.cells
            return (all.maxOf { it.row } - all.minOf { it.row } + 1) * (all.maxOf { it.col } - all.minOf { it.col } + 1)
        }

        put(BoardSlot(base, 0, 0, across = true))
        val rest = spelled
            .filter { it.key != base.key }
            .sortedWith(compareByDescending<WheelWord> { it.letters.size }.thenBy { it.key })

        for (word in rest) {
            if (placed.size >= tier.maxBoardWords) break
            val options = mutableListOf<BoardSlot>()
            for ((cell, letter) in letters) {
                val dirs = directions[cell] ?: 0
                word.letters.forEachIndexed { i, l ->
                    if (l != letter) return@forEachIndexed
                    if (dirs == ACROSS) options += BoardSlot(word, cell.row - i, cell.col, across = false)
                    if (dirs == DOWN) options += BoardSlot(word, cell.row, cell.col - i, across = true)
                }
            }
            options
                .filter(::fits)
                .minWithOrNull(compareBy<BoardSlot> { area(it) }.thenBy { it.row }.thenBy { it.col }.thenBy { it.across })
                ?.let(::put)
        }
        if (placed.size < tier.minBoardWords) return null

        val minRow = placed.minOf { s -> s.cells.minOf { it.row } }
        val minCol = placed.minOf { s -> s.cells.minOf { it.col } }
        return placed.map { it.copy(row = it.row - minRow, col = it.col - minCol) }
    }

    /** The wheel never starts in the base word's own order, which would give the longest word away. */
    private fun shuffledWheel(base: WheelWord): List<String> {
        val shuffled = base.letters.shuffled(Random(stableHash(base.key).toLong()))
        return if (shuffled != base.letters) shuffled else base.letters.drop(1) + base.letters.first()
    }

    /** FNV-1a: stable across runs and JVMs, unlike relying on collection iteration order. */
    private fun stableHash(text: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (ch in text) {
            hash = hash xor ch.code
            hash *= 0x01000193
        }
        return hash
    }
}
