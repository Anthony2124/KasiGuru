package com.kasiguru.util

import com.kasiguru.data.local.DatabaseSeeder
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

    /**
     * Badges are no longer computed by [GamificationEngine]. They are seeded rows whose
     * `requiredValue` drives UserProgressRepository.checkAchievements(metricType, value),
     * so the thresholds to guard now live in the seed rather than in a function.
     *
     * This test previously called a `GamificationEngine.getAllBadges(...)` that the move to
     * seeded achievements deleted, which meant the whole unit-test source set failed to
     * compile — taking every other test, and CI's rules-deploy job, down with it.
     */
    @Test
    fun levelBadgesAreSeededWithAscendingThresholds() {
        val levelBadges = DatabaseSeeder.getInitialAchievements()
            .filter { it.metricType == "level" }
            .sortedBy { it.requiredValue }

        assertTrue("expected seeded level badges", levelBadges.size >= 2)

        val first = levelBadges.first()
        assertEquals("level_1", first.id)
        assertEquals(1, first.requiredValue)
        // Level 1 is where every player starts, so it is the one badge seeded already earned.
        assertTrue("level_1 should start unlocked", first.isUnlocked)

        val second = levelBadges[1]
        assertEquals("level_2", second.id)
        assertEquals(2, second.requiredValue)
        assertFalse("level_2 should start locked", second.isUnlocked)

        // Non-decreasing, not strictly increasing: two separate badge families deliberately
        // overlap at level 5 ("Kasiguranin Legend" and "Sumusulong"), and both unlocking on
        // the same milestone is fine. What would not be fine is a threshold going backwards.
        val thresholds = levelBadges.map { it.requiredValue }
        assertEquals(thresholds.sorted(), thresholds)
    }

    @Test
    fun everySeededAchievementHasAUsableThreshold() {
        val achievements = DatabaseSeeder.getInitialAchievements()
        assertTrue("expected seeded achievements", achievements.isNotEmpty())

        // Duplicate ids would collide on the Room primary key and drop rows at seed time.
        val ids = achievements.map { it.id }
        assertEquals(ids.distinct().size, ids.size)

        achievements.forEach { achievement ->
            assertTrue(
                "${achievement.id} needs a positive requiredValue",
                achievement.requiredValue > 0
            )
            assertTrue(
                "${achievement.id} needs a metricType for checkAchievements to match on",
                achievement.metricType.isNotBlank()
            )
        }
    }
}
