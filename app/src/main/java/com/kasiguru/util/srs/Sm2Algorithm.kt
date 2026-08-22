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
     * Successful retrievals before a word may count as learned.
     *
     * Was effectively two. Combined with [MIN_LEARNED_INTERVAL_DAYS] this now means a word has been
     * recalled on three separate occasions with real time in between, rather than answered
     * correctly twice in one sitting.
     */
    const val MIN_LEARNED_REVIEWS = 3

    /**
     * Interval a word must have reached before it counts as learned.
     *
     * This is the part that makes "learned" mean something. Review count alone can be run up inside
     * a single session, but the interval ladder only reaches 6 once a word has survived the gap
     * between the second and third review — so this is a proxy for "remembered after forgetting had
     * a chance to set in", which is the only evidence that actually predicts retention.
     */
    const val MIN_LEARNED_INTERVAL_DAYS = 6

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

        // Was `q >= 3 && timesReviewed >= 2`, which meant two correct multiple-choice answers
        // marked a word learned — and with four options, two lucky guesses lands about 6% of the
        // time. That is recognition under prompting, not recall, and it made the headline "words
        // learned" number describe something the learner could not actually do.
        val isLearned = q >= 3 &&
            timesReviewed >= MIN_LEARNED_REVIEWS &&
            newInterval >= MIN_LEARNED_INTERVAL_DAYS

        return Sm2Result(
            easinessFactor = newEf,
            intervalDays = newInterval,
            nextReviewDate = nextDate,
            timesReviewed = timesReviewed,
            isLearned = isLearned
        )
    }
}
