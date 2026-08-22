package com.kasiguru.util

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.Sm2Algorithm
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the SuperMemo-2 spaced-repetition scheduler used by the flashcard
 * deck: easiness-factor floor, interval progression, and the learned flag.
 */
class Sm2AlgorithmTest {

    private val today = LocalDate.of(2026, 8, 14)

    private fun card(
        ef: Double = 2.5,
        intervalDays: Int = 0,
        timesReviewed: Int = 0
    ) = VocabularyEntity(
        kasiguranin = "abben",
        tagalog = "bahing",
        english = "sneeze",
        rootForm = "abben",
        category = "General",
        easinessFactor = ef,
        intervalDays = intervalDays,
        timesReviewed = timesReviewed
    )

    @Test
    fun failedRecallResetsIntervalAndNeverLearns() {
        val result = Sm2Algorithm.calculateNextReview(card(intervalDays = 6, timesReviewed = 5), ReviewRating.AGAIN, today)
        assertEquals(1, result.intervalDays)
        assertFalse(result.isLearned)
        assertEquals(today.plusDays(1).toString(), result.nextReviewDate)
    }

    @Test
    fun firstSuccessfulRecallStartsAtOneDay() {
        val result = Sm2Algorithm.calculateNextReview(card(), ReviewRating.GOOD, today)
        assertEquals(1, result.intervalDays)
        assertEquals(1, result.timesReviewed)
        assertFalse(result.isLearned)
    }

    @Test
    fun secondSuccessfulRecallMovesToSixDaysButIsNotYetLearned() {
        // This asserted `isLearned == true` until the mastery bar was raised. Two correct answers
        // could both happen inside one sitting, and on a four-option question two lucky guesses
        // land often enough to matter — so it certified words the learner could not produce.
        val result = Sm2Algorithm.calculateNextReview(card(intervalDays = 1, timesReviewed = 1), ReviewRating.GOOD, today)
        assertEquals(6, result.intervalDays)
        assertFalse(result.isLearned)
        assertEquals(today.plusDays(6).toString(), result.nextReviewDate)
    }

    @Test
    fun thirdSuccessfulRecallAcrossARealGapLearns() {
        // Three retrievals AND an interval past six days: the word survived a gap long enough for
        // forgetting to have been a real possibility.
        val result = Sm2Algorithm.calculateNextReview(card(ef = 2.5, intervalDays = 6, timesReviewed = 2), ReviewRating.GOOD, today)
        assertEquals(15, result.intervalDays) // 6 * 2.5
        assertTrue(result.isLearned)
        assertEquals(today.plusDays(15).toString(), result.nextReviewDate)
    }

    @Test
    fun repeatedAnswersInOneSittingCannotCertifyAWord() {
        // The exploit the old rule allowed: hammer the same word without any time passing. Review
        // count climbs, but the interval ladder cannot get past 6 in a single session, so mastery
        // stays out of reach until a real gap has been survived.
        var current = card()
        repeat(2) {
            val step = Sm2Algorithm.calculateNextReview(current, ReviewRating.EASY, today)
            assertFalse("no amount of same-day repetition should certify a word", step.isLearned)
            current = current.copy(
                easinessFactor = step.easinessFactor,
                intervalDays = step.intervalDays,
                timesReviewed = step.timesReviewed
            )
        }
    }

    @Test
    fun aLapseRevokesLearnedStatus() {
        // Mastery is not a trophy. A word that has been forgotten is not learned, and the interval
        // resetting to 1 puts it below the threshold on its own.
        val mastered = card(ef = 2.5, intervalDays = 15, timesReviewed = 5)
        val result = Sm2Algorithm.calculateNextReview(mastered, ReviewRating.AGAIN, today)

        assertFalse(result.isLearned)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun aWordSeededAtTheThresholdStaysLearnedOnItsNextReview() {
        // VocabularyRepository.markAsLearned ("I already know this one") seeds SM-2 to the mastery
        // thresholds instead of just setting the flag. If those seeded values did not actually
        // satisfy the rule, the next genuine review would recompute isLearned as false and the
        // learner's manual mark would vanish with no explanation. This pins the two together.
        val seeded = card(
            ef = 2.5,
            intervalDays = Sm2Algorithm.MIN_LEARNED_INTERVAL_DAYS,
            timesReviewed = Sm2Algorithm.MIN_LEARNED_REVIEWS
        )

        val result = Sm2Algorithm.calculateNextReview(seeded, ReviewRating.GOOD, today)

        assertTrue("a seeded-as-known word must survive its next review", result.isLearned)
    }

    @Test
    fun easyRatingRaisesEasinessFactor() {
        val result = Sm2Algorithm.calculateNextReview(card(ef = 2.5), ReviewRating.EASY, today)
        assertEquals(2.6, result.easinessFactor, 0.0001) // +0.1
    }

    @Test
    fun easinessFactorNeverDropsBelowFloor() {
        val result = Sm2Algorithm.calculateNextReview(card(ef = 1.3), ReviewRating.AGAIN, today)
        assertEquals(1.3, result.easinessFactor, 0.0001)
    }
}
