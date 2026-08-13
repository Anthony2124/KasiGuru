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
    fun secondSuccessfulRecallMovesToSixDaysAndLearns() {
        val result = Sm2Algorithm.calculateNextReview(card(intervalDays = 1, timesReviewed = 1), ReviewRating.GOOD, today)
        assertEquals(6, result.intervalDays)
        assertTrue(result.isLearned)
        assertEquals(today.plusDays(6).toString(), result.nextReviewDate)
    }

    @Test
    fun subsequentRecallsMultiplyByEasinessFactor() {
        val result = Sm2Algorithm.calculateNextReview(card(ef = 2.5, intervalDays = 6, timesReviewed = 2), ReviewRating.GOOD, today)
        assertEquals(15, result.intervalDays) // 6 * 2.5
        assertEquals(today.plusDays(15).toString(), result.nextReviewDate)
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
