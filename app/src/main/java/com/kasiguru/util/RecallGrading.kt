package com.kasiguru.util

import com.kasiguru.util.srs.ReviewRating

/**
 * Turns a typed-recall answer into the three things every surface needs from it: whether it counts,
 * what it pays, and what it tells spaced repetition.
 *
 * This is separate from [com.kasiguru.util.srs.ReviewRatingMapper] on purpose. That mapper reads
 * hesitation from the clock — under 1.5s is EASY, over 5s is HARD — which is a fair reading of a
 * *tap*: the options are on screen, so the delay is all deliberation. Typing is not that. Producing
 * `magandang` on a phone keyboard costs several seconds of pure motor work no matter how well the
 * word is known, so the tap thresholds would file almost every correct recall under HARD and
 * schedule the strongest evidence in the app as though it were the weakest.
 */
object RecallGrading {

    /** Thinking time allowed before the keyboard work is assumed to have started. */
    private const val THINKING_BUDGET_MS = 1_500L

    /** Roughly one two-and-a-half-character-per-second thumb, which is average mobile typing. */
    private const val MS_PER_CHARACTER = 400L

    /**
     * A near miss still counts.
     *
     * The learner produced the word; they were wrong about its orthography, not its meaning. Marking
     * that failure would reset the schedule of a word they demonstrably retrieved — and would teach
     * them to distrust a correct memory because of a character their keyboard cannot type.
     */
    fun isCorrect(match: RecallMatch): Boolean = match != RecallMatch.Wrong

    /**
     * What the answer says about memory.
     *
     * An exact answer is never rated HARD: recall is a stronger retrieval than recognition, so the
     * floor for producing the word unaided sits at GOOD, and speed only decides whether it earns the
     * longer EASY interval. A close answer is capped at HARD instead, which keeps the word coming
     * back soon enough for the spelling to correct itself before it sets.
     */
    fun ratingFor(match: RecallMatch, expected: String, responseTimeMs: Long): ReviewRating =
        when (match) {
            RecallMatch.Exact ->
                if (responseTimeMs < fastThresholdMs(expected)) ReviewRating.EASY else ReviewRating.GOOD
            RecallMatch.Close -> ReviewRating.HARD
            RecallMatch.Wrong -> ReviewRating.AGAIN
        }

    /**
     * XP for one answer.
     *
     * A close answer pays half rather than nothing, because paying nothing for a one-character slip
     * reads as "wrong" no matter what the feedback panel says, and the whole point of tolerating the
     * slip is that it was not wrong.
     */
    fun xpFor(match: RecallMatch): Int = when (match) {
        RecallMatch.Exact -> Constants.XP_PER_GAME_CORRECT
        RecallMatch.Close -> Constants.XP_PER_GAME_CORRECT / 2
        RecallMatch.Wrong -> 0
    }

    /** Time under which an exact answer reads as fluent, scaled to how much there was to type. */
    private fun fastThresholdMs(expected: String): Long =
        THINKING_BUDGET_MS + MS_PER_CHARACTER * expected.trim().length
}
