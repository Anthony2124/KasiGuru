package com.kasiguru.domain.gamification

import org.junit.Assert.*
import org.junit.Test

class ProfilePresentationTest {
    @Test fun backgroundsUnlockOnTheExactLevelOrPermanentStreakMilestone() {
        assertEquals(2, ProfileBackgroundCatalog.backgrounds.count { it.isUnlocked(1, 0) })
        assertFalse(ProfileBackgroundCatalog.find("farm")!!.isUnlocked(1, 0))
        assertTrue(ProfileBackgroundCatalog.find("farm")!!.isUnlocked(2, 0))
        val lighthouse = ProfileBackgroundCatalog.find("ontok_lighthouse")!!
        assertFalse(lighthouse.isUnlocked(4, 6)); assertTrue(lighthouse.isUnlocked(5, 0)); assertTrue(lighthouse.isUnlocked(1, 7))
        val tidal = ProfileBackgroundCatalog.find("tibu_tidal_pool")!!
        assertFalse(tidal.isUnlocked(30, 29)); assertTrue(tidal.isUnlocked(1, 30))
    }
    @Test fun showcasePrefersTierThenRarityAndOnlyOneBadgePerFamily() {
        val ids = listOf("badge:word_explorer:1", "badge:word_explorer:6", "badge:review_keeper:6",
            "badge:lesson_pathfinder:6", "badge:consistent_learner:6", "badge:precision_player:5")
        val rarity = mapOf("badge:word_explorer:6" to 10L, "badge:review_keeper:6" to 5L,
            "badge:lesson_pathfinder:6" to 1L, "badge:consistent_learner:6" to 20L, "badge:precision_player:5" to 0L)
        assertEquals(listOf("badge:lesson_pathfinder:6", "badge:review_keeper:6", "badge:word_explorer:6"), BadgeShowcase.choose(ids, rarity).map { it.id })
        assertEquals(5, BadgeShowcase.candidates(ids).size)
    }
}
