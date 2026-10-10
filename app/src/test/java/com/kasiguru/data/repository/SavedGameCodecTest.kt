package com.kasiguru.data.repository

import com.kasiguru.domain.games.SavedAnswer
import com.kasiguru.domain.games.SavedRound
import com.kasiguru.domain.games.SavedWordSearch
import com.kasiguru.domain.games.SavedWordWheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedGameCodecTest {

    private val round = SavedRound(
        questionKeys = listOf("12", "40", "7"),
        answers = listOf(
            SavedAnswer("Bëbbi ya \"dalaga\".", "bëbbi\nya", "Bëbbi ya dalaga.", isCorrect = false, subPrompt = "Tagalog: babae"),
            SavedAnswer("aso", "aso", "aso", isCorrect = true)
        ),
        score = 1,
        totalQuestions = 5,
        usedHint = true,
        hintRevealed = true
    )

    @Test
    fun `a question round comes back as it was saved`() {
        assertEquals(round, SavedGameCodec.decode(SavedGameCodec.encode(round)))
        assertEquals(2, round.nextIndex)
    }

    @Test
    fun `a missing sub-prompt stays missing rather than becoming empty text`() {
        val decoded = SavedGameCodec.decode(SavedGameCodec.encode(round)) as SavedRound
        assertNull(decoded.answers[1].subPrompt)
    }

    @Test
    fun `a word search grid comes back as it was saved`() {
        val grid = SavedWordSearch(puzzleWordIds = listOf(3, 9, 14), foundIds = listOf(14, 3), misses = 2, hintRevealed = false)
        assertEquals(grid, SavedGameCodec.decode(SavedGameCodec.encode(grid)))
    }

    @Test
    fun `a word wheel board comes back as it was saved, with or without a clue showing`() {
        val board = SavedWordWheel(
            boardWords = listOf("BALE", "ALË"),
            foundSlots = listOf(1),
            bonusWords = listOf("LA"),
            hintsUsed = 2,
            cluedSlot = 0
        )
        assertEquals(board, SavedGameCodec.decode(SavedGameCodec.encode(board)))
        val noClue = board.copy(cluedSlot = null)
        assertEquals(noClue, SavedGameCodec.decode(SavedGameCodec.encode(noClue)))
    }

    @Test
    fun `unreadable text is no saved game`() {
        assertNull(SavedGameCodec.decode(""))
        assertNull(SavedGameCodec.decode("not json"))
        assertNull(SavedGameCodec.decode("""{"format":1,"kind":"chess"}"""))
        assertNull(SavedGameCodec.decode("""{"format":1,"kind":"word_search","words":[1]}"""))
    }

    @Test
    fun `a save in another format version is not read`() {
        val text = SavedGameCodec.encode(round).replace("\"format\":1", "\"format\":2")
        assertNull(SavedGameCodec.decode(text))
    }

    @Test
    fun `a round that contradicts itself is not resumed`() {
        // More answers than questions, a score above the answers, a total below the questions.
        listOf(
            round.copy(questionKeys = listOf("12")),
            round.copy(score = 3),
            round.copy(totalQuestions = 2)
        ).forEach { bad -> assertNull(SavedGameCodec.decode(SavedGameCodec.encode(bad))) }
    }
}
