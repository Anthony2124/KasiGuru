package com.kasiguru.util

import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.ReviewRatingMapper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the answer-to-SM2-rating mapping used by every game ViewModel:
 * fast fluent recall = EASY, normal = GOOD, slow = HARD, wrong = AGAIN.
 */
class ReviewRatingMapperTest {

    @Test
    fun fastCorrectAnswerIsEasy() {
        assertEquals(ReviewRating.EASY, ReviewRatingMapper.ratingForAnswer(true, 800))
        assertEquals(ReviewRating.EASY, ReviewRatingMapper.ratingForAnswer(true, 1499))
    }

    @Test
    fun normalCorrectAnswerIsGood() {
        assertEquals(ReviewRating.GOOD, ReviewRatingMapper.ratingForAnswer(true, 1500))
        assertEquals(ReviewRating.GOOD, ReviewRatingMapper.ratingForAnswer(true, 4999))
    }

    @Test
    fun slowCorrectAnswerIsHard() {
        assertEquals(ReviewRating.HARD, ReviewRatingMapper.ratingForAnswer(true, 5000))
        assertEquals(ReviewRating.HARD, ReviewRatingMapper.ratingForAnswer(true, 30_000))
    }

    @Test
    fun wrongAnswerIsAlwaysAgainRegardlessOfSpeed() {
        assertEquals(ReviewRating.AGAIN, ReviewRatingMapper.ratingForAnswer(false, 100))
        assertEquals(ReviewRating.AGAIN, ReviewRatingMapper.ratingForAnswer(false, 60_000))
    }
}
