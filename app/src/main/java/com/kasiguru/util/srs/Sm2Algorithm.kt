package com.kasiguru.util.srs

import com.kasiguru.data.local.entity.VocabularyEntity
import java.time.LocalDate

enum class ReviewRating(val quality: Int) {
    AGAIN(0),
    HARD(3),
    GOOD(4),
    EASY(5)
}

data class Sm2Result(
    val easinessFactor: Double,
    val intervalDays: Int,
    val nextReviewDate: String,
    val timesReviewed: Int,
    val isLearned: Boolean
)

object Sm2Algorithm {

    /**
     * Calculates the next SuperMemo-2 (SM-2) review schedule.
     */
    fun calculateNextReview(
        card: VocabularyEntity,
        rating: ReviewRating,
        currentDate: LocalDate = LocalDate.now()
    ): Sm2Result {
        val q = rating.quality
        
        // 1. Calculate new Easiness Factor (EF)
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        var newEf = card.easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        if (newEf < 1.3) newEf = 1.3

        // 2. Calculate new interval in days
        val newInterval = when {
            q < 3 -> 1 // Again (Failed recall) -> reset interval
            card.intervalDays <= 0 -> 1 // 1st successful recall
            card.intervalDays == 1 -> 6 // 2nd successful recall
            else -> (card.intervalDays * newEf).toInt() // Subsequent recalls
        }

        // 3. Compute next review date
        val nextDate = currentDate.plusDays(newInterval.toLong()).toString()
        val timesReviewed = card.timesReviewed + 1
        val isLearned = q >= 3 && timesReviewed >= 2

        return Sm2Result(
            easinessFactor = newEf,
            intervalDays = newInterval,
            nextReviewDate = nextDate,
            timesReviewed = timesReviewed,
            isLearned = isLearned
        )
    }
}
