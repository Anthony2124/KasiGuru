package com.kasiguru.data.repository

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards how a practice round is assembled from due and fresh words.
 *
 * The games used to select purely by `timesReviewed ASC`, so they wrote an SM-2 schedule on every
 * answer and never read it back — a word due today was no likelier to be asked than any other.
 * These pin the replacement: review leads, but never to the point of crowding out new material.
 */
class PracticeRoundTest {

    private fun word(id: Int) = VocabularyEntity(
        id = id,
        kasiguranin = "word$id",
        tagalog = "t$id",
        english = "e$id"
    )

    private fun words(range: IntRange) = range.map { word(it) }

    @Test
    fun dueWordsLeadTheRound() {
        val due = words(1..10)
        val fresh = words(101..110)

        val round = buildPracticeRound(due, fresh, count = 10)
        val dueCount = round.count { it.id <= 10 }

        // 70% of 10, so seven of the ten slots go to review.
        assertEquals(7, dueCount)
        assertEquals(10, round.size)
    }

    @Test
    fun freshMaterialIsAlwaysReserved() {
        // The backlog case: far more due words than fit. A learner who missed a week must still
        // meet new words, or practice becomes pure penance and the dictionary stops growing.
        val due = words(1..100)
        val fresh = words(101..110)

        val round = buildPracticeRound(due, fresh, count = 10)

        assertTrue("expected some new material", round.any { it.id > 100 })
        assertEquals(3, round.count { it.id > 100 })
    }

    @Test
    fun anEmptyScheduleGivesAnAllFreshRound() {
        // A new learner has nothing due. The round must still be full.
        val round = buildPracticeRound(due = emptyList(), fresh = words(101..110), count = 10)

        assertEquals(10, round.size)
        assertTrue(round.all { it.id > 100 })
    }

    @Test
    fun noFreshMaterialFallsBackToReview() {
        // A learner who has seen the whole corpus: every slot should go to review rather than
        // returning a short round.
        val round = buildPracticeRound(due = words(1..10), fresh = emptyList(), count = 10)

        assertEquals(10, round.size)
        assertTrue(round.all { it.id <= 10 })
    }

    @Test
    fun aWordThatIsBothDueAndFreshAppearsOnce() {
        // getFreshWords orders by timesReviewed and will return words that are also due, so the
        // two candidate lists overlap. A duplicate would ask the same question twice in a round.
        val overlapping = words(1..5)
        val round = buildPracticeRound(due = overlapping, fresh = overlapping, count = 5)

        assertEquals(5, round.size)
        assertEquals(5, round.map { it.id }.distinct().size)
    }

    @Test
    fun neverReturnsMoreThanAsked() {
        val round = buildPracticeRound(words(1..50), words(101..150), count = 8)
        assertEquals(8, round.size)
    }

    @Test
    fun handlesACorpusSmallerThanTheRound() {
        // Nothing to pad with; a short round is correct here rather than repeating words.
        val round = buildPracticeRound(words(1..2), words(101..102), count = 10)

        assertEquals(4, round.size)
        assertEquals(4, round.map { it.id }.distinct().size)
    }

    @Test
    fun zeroOrNegativeCountYieldsNothing() {
        assertTrue(buildPracticeRound(words(1..5), words(101..105), count = 0).isEmpty())
        assertTrue(buildPracticeRound(words(1..5), words(101..105), count = -3).isEmpty())
    }
}
