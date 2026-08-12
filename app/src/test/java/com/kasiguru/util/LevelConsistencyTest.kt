package com.kasiguru.util

import com.kasiguru.util.gamification.GamificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the level-system consolidation: the UI level shown by
 * GamificationEngine must agree with the stored level used for achievements,
 * and both must derive from the single source (Constants.LEVEL_THRESHOLDS).
 */
class LevelConsistencyTest {

    @Test
    fun gamificationLevelsMatchStoredLevels() {
        // Boundaries of Constants.LEVEL_THRESHOLDS (0,100,300,600,1000,...)
        val samples = listOf(0, 99, 100, 250, 299, 300, 500, 599, 600, 999, 1000, 1499)
        for (xp in samples) {
            val stored = calculateLevel(xp)
            val shown = GamificationEngine.getLevelInfo(xp).level
            assertEquals("level mismatch at XP=$xp", stored, shown)
        }
    }

    @Test
    fun gamificationLevelsCapAtLegendForHighXp() {
        // Stored level can reach 10, but the display rank caps at 5.
        for (xp in listOf(1000, 1500, 3000, 5000, 100000)) {
            assertEquals(5, GamificationEngine.getLevelInfo(xp).level)
            assertTrue(calculateLevel(xp) >= 5)
        }
    }

    @Test
    fun boundariesDerivedFromConstants() {
        val thresholds = Constants.LEVEL_THRESHOLDS
        GamificationEngine.LEVELS.forEachIndexed { i, level ->
            assertEquals(thresholds[i], level.minXp)
            val expectedMax = if (i == GamificationEngine.LEVELS.size - 1) {
                Int.MAX_VALUE
            } else {
                thresholds[i + 1] - 1
            }
            assertEquals(expectedMax, level.maxXp)
        }
    }
}
