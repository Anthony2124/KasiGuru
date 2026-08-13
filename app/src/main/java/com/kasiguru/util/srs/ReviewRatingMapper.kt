package com.kasiguru.util.srs

/**
 * Maps a game answer into the SM-2 rating used by the flashcard scheduler.
 *
 * Fast, fluent recall earns EASY (longest interval), normal recall earns
 * GOOD, slow/hesitant recall earns HARD, and a wrong answer earns AGAIN
 * (interval resets to 1 day). This is the single source of truth for all
 * game ViewModels so the thresholds can't drift apart again.
 */
object ReviewRatingMapper {

    private const val EASY_THRESHOLD_MS = 1_500L
    private const val GOOD_THRESHOLD_MS = 5_000L

    fun ratingForAnswer(isCorrect: Boolean, responseTimeMs: Long): ReviewRating {
        if (!isCorrect) return ReviewRating.AGAIN
        return when {
            responseTimeMs < EASY_THRESHOLD_MS -> ReviewRating.EASY
            responseTimeMs < GOOD_THRESHOLD_MS -> ReviewRating.GOOD
            else -> ReviewRating.HARD
        }
    }
}
