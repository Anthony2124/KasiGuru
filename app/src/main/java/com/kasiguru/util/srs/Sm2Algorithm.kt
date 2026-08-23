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
    val isLearned: Boolean,
    /** Running count of times this word has been forgotten after having been known. */
    val lapses: Int = 0,
    /** Rung of the relearning ladder the word is now on; 0 once it is back on its normal schedule. */
    val relearningStep: Int = 0
) {
    /** True when the word has been forgotten often enough to need re-teaching, not more drilling. */
    val isLeech: Boolean get() = lapses >= Sm2Algorithm.LEECH_LAPSES
}

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
     * Days between reviews while a lapsed word climbs back.
     *
     * Plain SM-2 sends a failed word to a one-day interval and then, on the very next correct
     * answer, to six days -- the same jump it gives a word that has never been failed. That is the
     * step where forgetting actually happens, so a word that was just forgotten is the last one that
     * should be given it. The ladder makes the way back gradual: a day, two days, four, and only
     * then back onto the multiplying schedule.
     */
    val RELEARNING_STEPS = listOf(1, 2, 4)

    /**
     * Lapses after which a word is treated as a leech.
     *
     * Anki uses eight, tuned for decks of thousands reviewed daily for years. This corpus is 400
     * words and the audience is a class, so failure five times means the word is not being learned
     * and the deck is the wrong tool for it -- it needs teaching again, with the meaning and an
     * example in front of the learner rather than four options and a timer.
     */
    const val LEECH_LAPSES = 5

    /** Whether this word has been forgotten often enough to need re-teaching rather than testing. */
    fun isLeech(card: VocabularyEntity): Boolean = card.lapses >= LEECH_LAPSES

    /**
     * Calculates the next SuperMemo-2 (SM-2) review schedule.
     *
     * Two things happen here that plain SM-2 does not describe: a failed word that had reached a
     * real interval is recorded as a lapse, and the way back up is the [RELEARNING_STEPS] ladder
     * rather than a single jump.
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

        // 2. Interval, lapse bookkeeping and the relearning ladder.
        val failed = q < 3
        // Only a word that had actually been retained can be forgotten. A word still being met for
        // the first times is not lapsing, it is simply being learned, and counting those would make
        // every new word look like a leech.
        val hadBeenRetained = card.isLearned || card.intervalDays >= MIN_LEARNED_INTERVAL_DAYS
        val lapses = card.lapses + if (failed && hadBeenRetained) 1 else 0

        val relearningStep: Int
        val newInterval: Int
        when {
            failed -> {
                // Back to the bottom of the ladder, whether or not it was already on it.
                relearningStep = 1
                newInterval = RELEARNING_STEPS.first()
            }

            card.relearningStep > 0 -> {
                val nextStep = card.relearningStep + 1
                if (nextStep <= RELEARNING_STEPS.size) {
                    relearningStep = nextStep
                    newInterval = RELEARNING_STEPS[nextStep - 1]
                } else {
                    // Graduated: rejoin the normal multiplying schedule from where the ladder left
                    // it, rather than from the interval it had before it was forgotten.
                    relearningStep = 0
                    newInterval = (card.intervalDays * newEf).toInt().coerceAtLeast(RELEARNING_STEPS.last() + 1)
                }
            }

            card.intervalDays <= 0 -> {
                relearningStep = 0
                newInterval = 1 // 1st successful recall
            }

            card.intervalDays == 1 -> {
                relearningStep = 0
                newInterval = 6 // 2nd successful recall
            }

            else -> {
                relearningStep = 0
                newInterval = (card.intervalDays * newEf).toInt() // Subsequent recalls
            }
        }

        // 3. Compute next review date
        val nextDate = currentDate.plusDays(newInterval.toLong()).toString()
        val timesReviewed = card.timesReviewed + 1

        // Was `q >= 3 && timesReviewed >= 2`, which meant two correct multiple-choice answers
        // marked a word learned — and with four options, two lucky guesses lands about 6% of the
        // time. That is recognition under prompting, not recall, and it made the headline "words
        // learned" number describe something the learner could not actually do.
        val isLearned = q >= 3 &&
            // A word part-way up the ladder has not been retained yet, whatever its history says.
            relearningStep == 0 &&
            timesReviewed >= MIN_LEARNED_REVIEWS &&
            newInterval >= MIN_LEARNED_INTERVAL_DAYS

        return Sm2Result(
            easinessFactor = newEf,
            intervalDays = newInterval,
            nextReviewDate = nextDate,
            timesReviewed = timesReviewed,
            isLearned = isLearned,
            lapses = lapses,
            relearningStep = relearningStep
        )
    }
}
