package com.kasiguru.util

import com.kasiguru.util.gamification.GamificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the game-unlock rules (level + accuracy gates) and the badge
 * thresholds presented in the UI.
 */
class GamificationEngineTest {

    @Test
    fun wordMatchIsUnlockedFromLevelOne() {
        assertTrue(GamificationEngine.isGameUnlocked("word_match", userLevel = 1))
        assertTrue(GamificationEngine.isGameUnlocked("word_match", userLevel = 3))
    }

    @Test
    fun reverseMatchUnlocksAtLevelTwo() {
        assertFalse(GamificationEngine.isGameUnlocked("reverse_match", userLevel = 1))
        assertTrue(GamificationEngine.isGameUnlocked("reverse_match", userLevel = 2, accuracyRate = 0.8f))
        assertEquals(2, GamificationEngine.getRequiredLevelForGame("reverse_match"))
    }

    @Test
    fun lockedGamesRequireLevelAndAccuracy() {
        assertFalse(GamificationEngine.isGameUnlocked("fill_blank", userLevel = 1))
        assertFalse(GamificationEngine.isGameUnlocked("fill_blank", userLevel = 2, accuracyRate = 0.5f))
        assertTrue(GamificationEngine.isGameUnlocked("fill_blank", userLevel = 2, accuracyRate = 0.8f))
        assertFalse(GamificationEngine.isGameUnlocked("aspect_builder", userLevel = 3, accuracyRate = 0.9f))
        assertTrue(GamificationEngine.isGameUnlocked("aspect_builder", userLevel = 4, accuracyRate = 0.7f))
        assertTrue(GamificationEngine.isGameUnlocked("sentence_order", userLevel = 5, accuracyRate = 1.0f))
    }

    @Test
    fun accuracyGateIsExactlySeventyPercent() {
        assertFalse(GamificationEngine.isGameUnlocked("sentence_order", userLevel = 5, accuracyRate = 0.69f))
        assertTrue(GamificationEngine.isGameUnlocked("sentence_order", userLevel = 5, accuracyRate = 0.70f))
    }

    @Test
    fun requiredLevelsMatchUnlockRules() {
        assertEquals(1, GamificationEngine.getRequiredLevelForGame("word_match"))
        assertEquals(2, GamificationEngine.getRequiredLevelForGame("fill_blank"))
        assertEquals(3, GamificationEngine.getRequiredLevelForGame("audio_quiz"))
        assertEquals(4, GamificationEngine.getRequiredLevelForGame("aspect_builder"))
        assertEquals(5, GamificationEngine.getRequiredLevelForGame("sentence_order"))
        assertEquals(1, GamificationEngine.getRequiredLevelForGame("unknown_game"))
    }

    @Test
    fun xpProgressIsBoundedWithinLevel() {
        assertEquals(0.0f, GamificationEngine.getXpProgressInLevel(0), 0.0001f)
        val progress = GamificationEngine.getXpProgressInLevel(150)
        assertTrue(progress > 0.0f && progress < 1.0f)
        assertEquals(1.0f, GamificationEngine.getXpProgressInLevel(1_000_000), 0.0001f)
    }

    @Test
    fun badgesUnlockAtTheirThresholds() {
        val badgesAt1 = GamificationEngine.getAllBadges(userLevel = 1, wordsLearned = 0, streak = 0)
        assertTrue(badgesAt1.any { it.id == "level_1" && it.isUnlocked })
        assertFalse(badgesAt1.any { it.id == "level_2" && it.isUnlocked })

        val badgesAt2 = GamificationEngine.getAllBadges(userLevel = 2, wordsLearned = 0, streak = 0)
        assertTrue(badgesAt2.any { it.id == "level_2" && it.isUnlocked })
    }
}
