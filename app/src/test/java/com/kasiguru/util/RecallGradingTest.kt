package com.kasiguru.util

import com.kasiguru.util.srs.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards how a typed answer is paid and scheduled.
 *
 * The rules here are the ones a stopwatch would get wrong: recall is slower than recognition
 * because typing is slow, so latency alone must not be allowed to file a correct recall as a
 * struggling one.
 */
class RecallGradingTest {

    @Test
    fun anExactAnswerIsNeverRatedHardHoweverLongTheTypingTook() {
        val rating = RecallGrading.ratingFor(RecallMatch.Exact, "magandang", responseTimeMs = 60_000)
        assertEquals(ReviewRating.GOOD, rating)
    }

    @Test
    fun aQuickExactAnswerEarnsEasy() {
        // Well inside 1.5s thinking + 400ms per character.
        val rating = RecallGrading.ratingFor(RecallMatch.Exact, "apak", responseTimeMs = 2_000)
        assertEquals(ReviewRating.EASY, rating)
    }

    @Test
    fun theFastThresholdScalesWithWordLength() {
        // 4s is slow for a four-letter word and fast for a nine-letter one.
        assertEquals(ReviewRating.GOOD, RecallGrading.ratingFor(RecallMatch.Exact, "apak", 4_000))
        assertEquals(ReviewRating.EASY, RecallGrading.ratingFor(RecallMatch.Exact, "magandang", 4_000))
    }

    @Test
    fun aCloseAnswerIsCappedAtHardSoTheSpellingComesBackSoon() {
        assertEquals(ReviewRating.HARD, RecallGrading.ratingFor(RecallMatch.Close, "kagət", 1_000))
    }

    @Test
    fun aWrongAnswerResetsTheSchedule() {
        assertEquals(ReviewRating.AGAIN, RecallGrading.ratingFor(RecallMatch.Wrong, "kagət", 1_000))
    }

    @Test
    fun closeCountsAsCorrectAndPaysHalf() {
        assertTrue(RecallGrading.isCorrect(RecallMatch.Close))
        assertEquals(Constants.XP_PER_GAME_CORRECT / 2, RecallGrading.xpFor(RecallMatch.Close))
    }

    @Test
    fun exactPaysFullAndWrongPaysNothing() {
        assertTrue(RecallGrading.isCorrect(RecallMatch.Exact))
        assertEquals(Constants.XP_PER_GAME_CORRECT, RecallGrading.xpFor(RecallMatch.Exact))
        assertFalse(RecallGrading.isCorrect(RecallMatch.Wrong))
        assertEquals(0, RecallGrading.xpFor(RecallMatch.Wrong))
    }
}
