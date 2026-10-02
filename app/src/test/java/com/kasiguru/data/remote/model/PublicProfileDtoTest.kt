package com.kasiguru.data.remote.model

import org.junit.Assert.*
import org.junit.Test

class PublicProfileDtoTest {
    @Test fun privateFieldsAndUnknownAchievementsCannotRoundTrip() {
        val data = mapOf<String, Any>("displayName" to "Learner name", "email" to "private@example.test",
            "fullName" to "Private name", "age" to 20, "address" to "Private address", "password" to "secret",
            "recoveryCode" to "secret", "profileBackgroundId" to "invalid", "level" to 900,
            "badgeIds" to listOf("badge:word_explorer:1", "private@example.test"),
            "sections" to mapOf("pagbati" to 2, "private@example.test" to 3))
        val result = PublicProfileDto.fromMap(data).toMap()
        assertEquals("forest", result["profileBackgroundId"])
        assertEquals(30, result["level"])
        assertEquals(listOf("badge:word_explorer:1"), result["badgeIds"])
        assertEquals(mapOf("pagbati" to 2), result["sections"])
        assertTrue(result.keys.intersect(setOf("email", "fullName", "age", "address", "password", "recoveryCode")).isEmpty())
        assertFalse(result.toString().contains("private@example.test"))
    }
    @Test fun emailIsNeverUsedAsPublicDisplayName() {
        assertEquals("Learner", PublicProfileDto.displayName("account@example.test"))
        assertEquals("Learner", PublicProfileDto.displayName("  "))
        assertEquals("Kiko", PublicProfileDto.displayName("  Kiko  "))
    }
    @Test fun aLearnerWhoNeverSetANicknameIsRankedByFullName() {
        // Signed in from the first onboarding screen: the nickname is still the placeholder.
        assertEquals("Ana Cruz", PublicProfileDto.displayName("Learner", "Ana Cruz"))
        assertEquals("Ana Cruz", PublicProfileDto.displayName("", "  Ana Cruz "))
        // A chosen nickname still wins over the full name.
        assertEquals("Kiko", PublicProfileDto.displayName("Kiko", "Francisco Reyes"))
        // Neither an email nor nothing at all is ever published.
        assertEquals("Learner", PublicProfileDto.displayName("Learner", "ana@example.test"))
        assertEquals("Learner", PublicProfileDto.displayName("Learner", ""))
    }
}
