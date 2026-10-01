package com.kasiguru.data.repository

import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.domain.gamification.BadgeCatalog
import org.junit.Assert.*
import org.junit.Test

class NormalizedProgressSyncTest {
    @Test fun freshInstallRestoresEarnedLegacyBadgeWithOriginalNameAndDate() {
        val restored = mergeLegacyAchievementRows(BadgeCatalog.rows(), mapOf(
            "first_word" to AchievementState(true, 1, "2025-01-05")))
        assertEquals(1, restored.size)
        assertEquals("Unáng Salitâ", restored.single().name)
        assertEquals("2025-01-05", restored.single().unlockedDate)
        assertTrue(restored.single().isUnlocked)
        assertEquals(0, restored.single().xpReward)
    }

    @Test fun cloudBadgeFlagsCannotGrantActiveTiersOrCreateUnearnedLegacyRows() {
        val restored = mergeLegacyAchievementRows(BadgeCatalog.rows(), mapOf(
            "badge:word_explorer:6" to AchievementState(true, 800, "2025-01-05"),
            "ten_words" to AchievementState(false, 4)))
        assertTrue(restored.isEmpty())
    }

    @Test fun legacyRestoreKeepsLocalMetadataAndEarliestEarnedDate() {
        val original = AchievementEntity("custom_badge", "Original name", "Original description",
            iconEmoji = "", isUnlocked = true, unlockedDate = "2025-01-05")
        val restored = mergeLegacyAchievementRows(listOf(original), mapOf(
            "custom_badge" to AchievementState(true, 5, "2025-02-05"),
            "older_unknown_badge" to AchievementState(true, 1, "2025-03-05")))
        assertEquals("Original name", restored.first { it.id == original.id }.name)
        assertEquals("2025-01-05", restored.first { it.id == original.id }.unlockedDate)
        assertEquals("2025-03-05", restored.first { it.id == "older_unknown_badge" }.unlockedDate)
    }

    @Test fun newerLegacyCloudTotalsCannotUndoNormalization() {
        val local = UserProgressEntity(totalXp = 140,level = 2,xpPolicyVersion = 2,activityXp = 20,
            badgeBonusXp = 120,dailyXpDate = "2026-10-01",dailyXpEarned = 20)
        val remote = UserProgressEntity(totalXp = 50000,level = 10,updatedAt = 9999999999999,
            dailyXpDate = "2026-10-01",dailyXpEarned = 2000)
        val merged = mergeProgress(local,remote)
        assertEquals(140,merged.totalXp)
        assertEquals(2,merged.level)
        assertEquals(20,merged.dailyXpEarned)
        assertEquals(2,merged.xpPolicyVersion)
    }
    @Test fun normalizedFieldsRoundTripIncludingProfilePins() {
        val row = UserProgressEntity(xpPolicyVersion = 2,activityXp = 60,badgeBonusXp = 40,
            pinnedBadgeIds = "word_explorer,lesson_pathfinder")
        val result = toEntity(toMap(row))
        assertEquals(row.xpPolicyVersion,result.xpPolicyVersion)
        assertEquals(row.activityXp,result.activityXp)
        assertEquals(row.badgeBonusXp,result.badgeBonusXp)
        assertEquals(row.pinnedBadgeIds,result.pinnedBadgeIds)
    }
}
