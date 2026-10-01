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
    fun xpProgressIsBoundedWithinLevel() {
        assertEquals(0.0f, GamificationEngine.getXpProgressInLevel(0), 0.0001f)
        val progress = GamificationEngine.getXpProgressInLevel(150)
        assertTrue(progress > 0.0f && progress < 1.0f)
        assertEquals(1.0f, GamificationEngine.getXpProgressInLevel(1_000_000), 0.0001f)
    }

    @Test
    fun freshInstallsSeedAllSixtySixTiersWithoutGrantingWelcomeXp() {
        val rows = DatabaseSeeder.getInitialAchievements()
        assertEquals(66,rows.size)
        assertTrue(rows.none { it.isUnlocked })
        assertTrue(rows.filter { it.metricType == "level" }.all { it.xpReward == 0 })
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
