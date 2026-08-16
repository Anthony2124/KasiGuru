package com.kasiguru.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reinstall/second-device guarantee: a device that knows nothing must never
 * erase progress that exists elsewhere.
 */
class LearningStateMergeTest {

    @Test
    fun freshInstallKeepsCloudAchievements() {
        val local = mapOf("first_word" to AchievementState(isUnlocked = false, currentValue = 0))
        val remote = mapOf("first_word" to AchievementState(isUnlocked = true, currentValue = 1, unlockedDate = "2026-01-05"))

        val merged = mergeAchievements(local, remote)

        assertTrue(merged.getValue("first_word").isUnlocked)
        assertEquals(1, merged.getValue("first_word").currentValue)
        assertEquals("2026-01-05", merged.getValue("first_word").unlockedDate)
    }

    @Test
    fun achievementUnlockedOnEitherSideStaysUnlocked() {
        val local = mapOf("ten_words" to AchievementState(isUnlocked = true, currentValue = 10, unlockedDate = "2026-02-01"))
        val remote = mapOf("ten_words" to AchievementState(isUnlocked = false, currentValue = 4))

        assertTrue(mergeAchievements(local, remote).getValue("ten_words").isUnlocked)
    }

    @Test
    fun achievementKeepsEarliestUnlockDate() {
        val local = mapOf("a" to AchievementState(isUnlocked = true, unlockedDate = "2026-03-09"))
        val remote = mapOf("a" to AchievementState(isUnlocked = true, unlockedDate = "2026-01-02"))

        assertEquals("2026-01-02", mergeAchievements(local, remote).getValue("a").unlockedDate)
    }

    @Test
    fun achievementsPresentOnOnlyOneSideSurvive() {
        val local = mapOf("only_local" to AchievementState(isUnlocked = true))
        val remote = mapOf("only_remote" to AchievementState(isUnlocked = true))

        val merged = mergeAchievements(local, remote)

        assertEquals(setOf("only_local", "only_remote"), merged.keys)
    }

    @Test
    fun gameLevelStarsTakeTheBest() {
        val local = mapOf("word_match_3" to GameLevelState(starsEarned = 1, isUnlocked = true))
        val remote = mapOf("word_match_3" to GameLevelState(starsEarned = 3, isUnlocked = true))

        assertEquals(3, mergeGameLevels(local, remote).getValue("word_match_3").starsEarned)
    }

    @Test
    fun unlockedLevelIsNeverRelocked() {
        val local = mapOf("fill_blank_7" to GameLevelState(starsEarned = 0, isUnlocked = false))
        val remote = mapOf("fill_blank_7" to GameLevelState(starsEarned = 2, isUnlocked = true))

        val merged = mergeGameLevels(local, remote).getValue("fill_blank_7")

        assertTrue(merged.isUnlocked)
        assertEquals(2, merged.starsEarned)
    }

    @Test
    fun matureReviewScheduleSurvivesAFreshInstall() {
        // Fresh install: word never reviewed here.
        val local = mapOf("aldew" to WordState())
        val remote = mapOf(
            "aldew" to WordState(
                isLearned = true,
                timesReviewed = 9,
                easinessFactor = 2.9,
                intervalDays = 30,
                nextReviewDate = "2026-09-01"
            )
        )

        val merged = mergeWordStates(local, remote).getValue("aldew")

        assertTrue(merged.isLearned)
        assertEquals(9, merged.timesReviewed)
        assertEquals(2.9, merged.easinessFactor, 0.0001)
        assertEquals(30, merged.intervalDays)
        assertEquals("2026-09-01", merged.nextReviewDate)
    }

    @Test
    fun moreReviewedSideOwnsTheSchedule() {
        val local = mapOf("bahay" to WordState(isLearned = true, timesReviewed = 12, easinessFactor = 2.1, intervalDays = 20, nextReviewDate = "2026-08-20"))
        val remote = mapOf("bahay" to WordState(isLearned = true, timesReviewed = 3, easinessFactor = 2.7, intervalDays = 5, nextReviewDate = "2026-08-01"))

        val merged = mergeWordStates(local, remote).getValue("bahay")

        assertEquals(12, merged.timesReviewed)
        assertEquals(2.1, merged.easinessFactor, 0.0001)
        assertEquals("2026-08-20", merged.nextReviewDate)
    }

    @Test
    fun learnedFlagIsNeverLost() {
        val local = mapOf("kasi" to WordState(isLearned = true, timesReviewed = 1))
        val remote = mapOf("kasi" to WordState(isLearned = false, timesReviewed = 8))

        assertTrue(mergeWordStates(local, remote).getValue("kasi").isLearned)
    }

    @Test
    fun emptyCloudLeavesLocalProgressIntact() {
        val local = mapOf(
            "a" to WordState(isLearned = true, timesReviewed = 4, intervalDays = 10, nextReviewDate = "2026-07-07")
        )

        val merged = mergeWordStates(local, emptyMap())

        assertEquals(local, merged)
    }
}
