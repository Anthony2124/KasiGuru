package com.kasiguru.domain.wordsearch

import java.text.Normalizer
import kotlin.random.Random

/**
 * Word Search: a grid of letters hiding Kasiguranin words from one dictionary category.
 *
 * Pure Kotlin with no Android or Room types, so the whole puzzle model is unit-testable. The
 * ViewModel supplies the candidate words; this file decides which of them fit, where they go, and
 * whether a tapped line of cells spells one of them.
 */

/** A straight line through the grid. Rows grow downward, columns grow rightward. */
enum class Direction(val dRow: Int, val dCol: Int) {
    EAST(0, 1), SOUTH(1, 0), SOUTH_EAST(1, 1), NORTH_EAST(-1, 1),
    WEST(0, -1), NORTH(-1, 0), NORTH_WEST(-1, -1), SOUTH_WEST(1, -1)
}

/**
 * One of the three difficulty bands every game in the app shares (1-10 Easy, 11-20 Medium, 21-30
 * Hard; see `GameLevelEntity`). Word Search makes each band harder along three axes at once: a bigger
 * grid, more words to find, and more directions a word may run in.
 */
data class WordSearchTier(
    val name: String,
    val gridSize: Int,
    val wordCount: Int,
    val directions: List<Direction>
) {
    companion object {
        const val MAX_LEVEL = 30

        fun forLevel(level: Int): WordSearchTier = when {
            level <= 10 -> WordSearchTier("Easy", 6, 4, listOf(Direction.EAST, Direction.SOUTH))
            level <= 20 -> WordSearchTier(
                "Medium", 8, 6,
                listOf(Direction.EAST, Direction.SOUTH, Direction.SOUTH_EAST, Direction.NORTH_EAST)
            )
            else -> WordSearchTier("Hard", 10, 8, Direction.values().toList())
        }
    }
}

/** A word offered to the generator. [id] is opaque here; the ViewModel uses the vocabulary row id. */
data class WordSearchCandidate(val id: Int, val word: String)

data class GridCell(val row: Int, val col: Int)

data class PlacedWord(
    val id: Int,
    /** The headword as the dictionary spells it, hyphens and all, for display. */
    val word: String,
    /** The tiles the word occupies in the grid, first letter first. */
    val cells: List<GridCell>,
    val letters: List<String>
)

class WordSearchPuzzle(
    val size: Int,
    /** `grid[row][col]`, one uppercase letter per cell. A String, not a Char, so `Ë` is one tile. */
    val grid: List<List<String>>,
    val words: List<PlacedWord>
) {
    fun letterAt(cell: GridCell): String = grid[cell.row][cell.col]

    /**
     * The cells from [from] to [to] inclusive, or null when the two are not on one straight
     * horizontal, vertical or diagonal line.
     */
    fun lineBetween(from: GridCell, to: GridCell): List<GridCell>? {
        val dRow = to.row - from.row
        val dCol = to.col - from.col
        if (dRow != 0 && dCol != 0 && kotlin.math.abs(dRow) != kotlin.math.abs(dCol)) return null
        val steps = maxOf(kotlin.math.abs(dRow), kotlin.math.abs(dCol))
        val stepRow = Integer.signum(dRow)
        val stepCol = Integer.signum(dCol)
        return (0..steps).map { GridCell(from.row + stepRow * it, from.col + stepCol * it) }
    }

    /**
     * The not-yet-found word that the line from [from] to [to] spells, read in either direction.
     *
     * Either direction, because a learner who spots the last letter first has still found the word;
     * making them re-tap it in reading order would test tapping, not reading. Matching is by letters,
     * not by stored cells, so a word that also happens to appear somewhere else by chance still counts.
     */
    fun match(from: GridCell, to: GridCell, foundIds: Set<Int>): PlacedWord? {
        val line = lineBetween(from, to) ?: return null
        val spelled = line.map(::letterAt)
        val reversed = spelled.asReversed()
        return words.firstOrNull { it.id !in foundIds && (it.letters == spelled || it.letters == reversed) }
    }
}

object WordSearchGenerator {

    /** Below this many placeable words a level is not worth playing, so the game says so instead. */
    const val MIN_WORDS = 3

    private const val PLACEMENT_ATTEMPTS = 250

    /**
     * The tiles for [word], or null if it cannot be laid out as a straight run of letters.
     *
     * NFC-normalised first so `ë` typed as `e` plus a combining diaeresis becomes one tile, the way
     * the dictionary shows it. Hyphens are dropped (`kili-kile` hides as KILIKILE); a word with a
     * space, digit or anything else that is not a letter is skipped, since it has no honest tile.
     */
    fun letters(word: String): List<String>? {
        val normalized = Normalizer.normalize(word.trim(), Normalizer.Form.NFC).replace("-", "")
        if (normalized.isEmpty() || normalized.any { !Character.isLetter(it) }) return null
        return normalized.map { it.uppercaseChar().toString() }
    }

    /**
     * Builds the puzzle for [level] from [candidates], or null when too few of them fit.
     *
     * Deterministic for the same [seed], level and candidate set: the ViewModel seeds from the
     * category and level, so replaying a level for more stars replays the same grid, which is what
     * makes a better score on it mean something.
     */
    fun generate(candidates: List<WordSearchCandidate>, level: Int, seed: Long): WordSearchPuzzle? {
        val tier = WordSearchTier.forLevel(level)
        val random = Random(seed)

        // Sorted before shuffling so the database's row order can never change the puzzle.
        val pool = candidates
            .sortedBy { it.id }
            .mapNotNull { c -> letters(c.word)?.let { c to it } }
            .filter { (_, tiles) -> tiles.size in 3..tier.gridSize }
            .distinctBy { (_, tiles) -> tiles.joinToString("") }
            .shuffled(random)

        val grid = Array(tier.gridSize) { arrayOfNulls<String>(tier.gridSize) }
        val placed = mutableListOf<PlacedWord>()

        for ((candidate, tiles) in pool) {
            if (placed.size >= tier.wordCount) break
            // A word that sits wholly inside an already-placed one (or holds one) would make two
            // entries on the list claim the same cells; skip it rather than confuse the learner.
            val text = tiles.joinToString("")
            if (placed.any { p -> p.letters.joinToString("").let { it.contains(text) || text.contains(it) } }) continue
            place(grid, tiles, tier, random)?.let { cells ->
                placed += PlacedWord(candidate.id, candidate.word.trim(), cells, tiles)
            }
        }
        if (placed.size < MIN_WORDS) return null

        // Filler drawn from the hidden words' own letters, so the decoys look like Kasiguranin
        // (including the occasional Ë) instead of an English-weighted alphabet that makes the real
        // words stand out.
        val fillers = placed.flatMap { it.letters }
        val rows = grid.map { row -> row.map { it ?: fillers[random.nextInt(fillers.size)] } }
        return WordSearchPuzzle(tier.gridSize, rows, placed)
    }

    private fun place(
        grid: Array<Array<String?>>,
        tiles: List<String>,
        tier: WordSearchTier,
        random: Random
    ): List<GridCell>? {
        val n = tier.gridSize
        repeat(PLACEMENT_ATTEMPTS) {
            val dir = tier.directions[random.nextInt(tier.directions.size)]
            val row = random.nextInt(n)
            val col = random.nextInt(n)
            val endRow = row + dir.dRow * (tiles.size - 1)
            val endCol = col + dir.dCol * (tiles.size - 1)
            if (endRow !in 0 until n || endCol !in 0 until n) return@repeat
            val cells = tiles.indices.map { GridCell(row + dir.dRow * it, col + dir.dCol * it) }
            // Crossing another word is allowed only where the shared letter agrees.
            if (cells.withIndex().any { (i, c) -> grid[c.row][c.col].let { it != null && it != tiles[i] } }) {
                return@repeat
            }
            cells.forEachIndexed { i, c -> grid[c.row][c.col] = tiles[i] }
            return cells
        }
        return null
    }
}
