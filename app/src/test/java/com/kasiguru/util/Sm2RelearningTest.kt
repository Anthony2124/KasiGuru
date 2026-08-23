package com.kasiguru.util

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.Sm2Algorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the memory model added in phase 5: lapse counting and the relearning ladder.
 *
 * The behaviour these replace is the one plain SM-2 has and should not: a word failed after months
 * of being known went to a one-day interval and then, on a single correct answer, straight back to
 * six days — the exact gap it had just proved it could not survive.
 */
class Sm2RelearningTest {

    private fun card(
        intervalDays: Int = 0,
        timesReviewed: Int = 0,
        easinessFactor: Double = 2.5,
        isLearned: Boolean = false,
        lapses: Int = 0,
        relearningStep: Int = 0
    ) = VocabularyEntity(
        id = 1,
        kasiguranin = "apak",
        intervalDays = intervalDays,
        timesReviewed = timesReviewed,
        easinessFactor = easinessFactor,
        isLearned = isLearned,
        lapses = lapses,
        relearningStep = relearningStep
    )

    @Test
    fun forgettingAKnownWordCountsAsALapse() {
        val result = Sm2Algorithm.calculateNextReview(
            card(intervalDays = 30, timesReviewed = 8, isLearned = true),
            ReviewRating.AGAIN
        )
        assertEquals(1, result.lapses)
        assertEquals(1, result.relearningStep)
        assertEquals(1, result.intervalDays)
        assertFalse("a forgotten word is not still learned", result.isLearned)
    }

    @Test
    fun missingAWordStillBeingLearnedIsNotALapse() {
        // Otherwise every new word looks like a leech within its first session, and the leech rule
        // would pull ordinary new vocabulary into re-teaching it does not need.
        val result = Sm2Algorithm.calculateNextReview(
            card(intervalDays = 1, timesReviewed = 1),
            ReviewRating.AGAIN
        )
        assertEquals(0, result.lapses)
    }

    @Test
    fun theWayBackIsAOneTwoFourLadderRatherThanOneJump() {
        var current = card(intervalDays = 30, timesReviewed = 8, isLearned = true)

        val lapse = Sm2Algorithm.calculateNextReview(current, ReviewRating.AGAIN)
        assertEquals(1, lapse.intervalDays)

        current = current.copy(
            intervalDays = lapse.intervalDays,
            timesReviewed = lapse.timesReviewed,
            easinessFactor = lapse.easinessFactor,
            isLearned = lapse.isLearned,
            lapses = lapse.lapses,
            relearningStep = lapse.relearningStep
        )
        val firstStep = Sm2Algorithm.calculateNextReview(current, ReviewRating.GOOD)
        assertEquals("the six-day jump is what the ladder exists to prevent", 2, firstStep.intervalDays)
        assertEquals(2, firstStep.relearningStep)

        current = current.copy(
            intervalDays = firstStep.intervalDays,
            timesReviewed = firstStep.timesReviewed,
            easinessFactor = firstStep.easinessFactor,
            relearningStep = firstStep.relearningStep
        )
        val secondStep = Sm2Algorithm.calculateNextReview(current, ReviewRating.GOOD)
        assertEquals(4, secondStep.intervalDays)
        assertEquals(3, secondStep.relearningStep)

        current = current.copy(
            intervalDays = secondStep.intervalDays,
            timesReviewed = secondStep.timesReviewed,
            easinessFactor = secondStep.easinessFactor,
            relearningStep = secondStep.relearningStep
        )
        val graduated = Sm2Algorithm.calculateNextReview(current, ReviewRating.GOOD)
        assertEquals("back on the normal schedule", 0, graduated.relearningStep)
        assertTrue("and past the top of the ladder", graduated.intervalDays > 4)
    }

    @Test
    fun failingDuringRelearningReturnsToTheBottomOfTheLadder() {
        val result = Sm2Algorithm.calculateNextReview(
            card(intervalDays = 4, timesReviewed = 12, lapses = 2, relearningStep = 3),
            ReviewRating.AGAIN
        )
        assertEquals(1, result.relearningStep)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun aWordOnTheLadderCannotBeCountedAsLearnedYet() {
        // Its review count and easiness factor both look healthy — the ladder position is the only
        // thing that knows it was forgotten last week.
        val result = Sm2Algorithm.calculateNextReview(
            card(intervalDays = 2, timesReviewed = 20, lapses = 1, relearningStep = 2),
            ReviewRating.GOOD
        )
        assertFalse(result.isLearned)
    }

    @Test
    fun aWordForgottenFiveTimesIsALeech() {
        val result = Sm2Algorithm.calculateNextReview(
            card(intervalDays = 12, timesReviewed = 20, isLearned = true, lapses = 4),
            ReviewRating.AGAIN
        )
        assertEquals(5, result.lapses)
        assertTrue(result.isLeech)
        assertTrue(Sm2Algorithm.isLeech(card(lapses = 5)))
        assertFalse(Sm2Algorithm.isLeech(card(lapses = 4)))
    }

    @Test
    fun anUnlapsedWordKeepsTheOriginalSchedule() {
        // The ladder must not change what happens to words that never fail.
        val first = Sm2Algorithm.calculateNextReview(card(), ReviewRating.GOOD)
        assertEquals(1, first.intervalDays)
        val second = Sm2Algorithm.calculateNextReview(card(intervalDays = 1, timesReviewed = 1), ReviewRating.GOOD)
        assertEquals(6, second.intervalDays)
        assertEquals(0, second.relearningStep)
        assertEquals(0, second.lapses)
    }
}
