package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards how hard a multiple choice is allowed to be.
 *
 * Both directions are real failures: three near-identical options beside a word met once turns a
 * lesson into a coin toss, and three unrelated options beside a mastered word asks nothing at all.
 */
class DistractorSelectorTest {

    private fun word(
        id: Int,
        headword: String = "word$id",
        category: String = "Tools",
        reviews: Int = 5,
        factor: Double = 2.3,
        learned: Boolean = false
    ) = VocabularyEntity(
        id = id,
        kasiguranin = headword,
        tagalog = "t$id",
        english = "e$id",
        category = category,
        timesReviewed = reviews,
        easinessFactor = factor,
        isLearned = learned
    )

    @Test
    fun aWordMetOnceGetsTheGentlestOptions() {
        assertEquals(
            DistractorDifficulty.GENTLE,
            DistractorSelector.difficultyFor(word(1, reviews = 1))
        )
    }

    @Test
    fun aWordBeingMissedRepeatedlyGetsTheGentlestOptions() {
        // A low easiness factor is the app's own record that this word keeps failing.
        assertEquals(
            DistractorDifficulty.GENTLE,
            DistractorSelector.difficultyFor(word(1, reviews = 9, factor = 1.7))
        )
    }

    @Test
    fun anEstablishedWordGetsSameFieldOptions() {
        assertEquals(
            DistractorDifficulty.STANDARD,
            DistractorSelector.difficultyFor(word(1, reviews = 4, factor = 2.3))
        )
    }

    @Test
    fun aMasteredWordGetsTheTightestOptions() {
        assertEquals(
            DistractorDifficulty.TIGHT,
            DistractorSelector.difficultyFor(word(1, reviews = 6, factor = 2.6))
        )
        assertEquals(
            DistractorDifficulty.TIGHT,
            DistractorSelector.difficultyFor(word(1, reviews = 6, factor = 2.2, learned = true))
        )
    }

    @Test
    fun tightOptionsPreferTheLookalikeFromTheSameField() {
        val target = word(1, headword = "apak")
        val lookalike = word(2, headword = "adak")
        val sameFieldOnly = word(3, headword = "pinggan")

        val chosen = DistractorSelector.choose(
            target = target,
            sameCategory = listOf(sameFieldOnly, lookalike),
            otherCategory = listOf(word(4, headword = "buhay", category = "Greetings")),
            difficulty = DistractorDifficulty.TIGHT,
            count = 1
        )

        assertEquals(listOf(2), chosen.map { it.id })
    }

    @Test
    fun gentleOptionsComeFromOutsideTheFieldFirst() {
        val target = word(1, headword = "apak")
        val chosen = DistractorSelector.choose(
            target = target,
            sameCategory = listOf(word(2, headword = "adak")),
            otherCategory = listOf(word(3, headword = "magandang aldew", category = "Greetings")),
            difficulty = DistractorDifficulty.GENTLE,
            count = 1
        )

        assertEquals(listOf(3), chosen.map { it.id })
    }

    @Test
    fun aThinCategoryFallsBackRatherThanReturningTooFewOptions() {
        // A three-option question with only one distractor available is worse than an easy third.
        val target = word(1, category = "Tiny")
        val chosen = DistractorSelector.choose(
            target = target,
            sameCategory = listOf(word(2, category = "Tiny")),
            otherCategory = (3..8).map { word(it, category = "Other") },
            difficulty = DistractorDifficulty.TIGHT,
            count = 3
        )

        assertEquals(3, chosen.size)
        assertTrue(chosen.any { it.id == 2 })
    }

    @Test
    fun theAnswerIsNeverOfferedAsItsOwnDistractor() {
        val target = word(1)
        val chosen = DistractorSelector.choose(
            target = target,
            sameCategory = listOf(target, word(2), word(3)),
            otherCategory = emptyList(),
            difficulty = DistractorDifficulty.STANDARD,
            count = 3
        )

        assertFalse(chosen.any { it.id == target.id })
    }

    @Test
    fun blankHeadwordsAreNotOfferedAsOptions() {
        val chosen = DistractorSelector.choose(
            target = word(1),
            sameCategory = listOf(word(2, headword = ""), word(3)),
            otherCategory = emptyList(),
            difficulty = DistractorDifficulty.STANDARD,
            count = 2
        )

        assertEquals(listOf(3), chosen.map { it.id })
    }
}
