package com.kasiguru.data.repository

import com.kasiguru.data.local.entity.UserProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards against the exact bug class documented on [mergeProgress]: a field added to
 * [UserProgressEntity] that's missed in `toMap`/`toEntity` doesn't fail loudly — it just
 * silently reverts to its default on the next sync. This already happened once in
 * production (the daily-XP ledger). Plain JVM reflection is enough here; no need for
 * `kotlin-reflect` just to list a data class's declared fields.
 */
class ProgressSyncFieldParityTest {

    /** Every entity field toMap() is allowed to leave out, and why. */
    private val intentionallyOmittedFromMap = setOf(
        "password" // never leaves the device, by design — see toMap()'s own comment.
    )

    @Test
    fun everyEntityFieldHasAMapKey() {
        val entityFields = UserProgressEntity::class.java.declaredFields
            // The Compose compiler plugin stamps a `$stable` bookkeeping field onto every
            // class in the module, Composable or not — not a real data field.
            .filterNot { it.isSynthetic || it.name == "\$stable" }
            .map { it.name }
            .toSet()
        val mapKeys = toMap(UserProgressEntity()).keys

        val missing = entityFields - mapKeys - intentionallyOmittedFromMap
        assertTrue(
            "UserProgressEntity field(s) $missing have no entry in toMap() — they will " +
                "silently revert to their default on the next sync. Add them to toMap() " +
                "(and toEntity(), and mergeProgress() if they should be preserved across " +
                "devices) or add them to intentionallyOmittedFromMap above with a reason.",
            missing.isEmpty()
        )
    }

    @Test
    fun everyMapKeyRoundTripsThroughToEntity() {
        val original = UserProgressEntity(
            id = 7, userName = "Roundtrip", email = "a@b.c", fullName = "Round Trip",
            age = 21, address = "Casiguran", profileIconId = 3, totalXp = 4200, level = 6,
            currentStreak = 12, longestStreak = 30, lastActiveDate = "2026-08-18",
            wordsLearned = 88, storiesCompleted = 3, gamesPlayed = 40, totalCorrectAnswers = 300,
            totalQuestionsAnswered = 350, lessonsCompleted = 9, isOnboardingCompleted = true,
            dailyGoalXp = 150, dailyXpEarned = 60, dailyXpDate = "2026-08-18",
            titleBadge = "Kasiguranin Legend", updatedAt = 999
        )

        // toMap() always stamps a fresh updatedAt, so compare everything else field by field
        // rather than the whole object — that one intentional difference isn't a bug.
        val roundTripped = toEntity(toMap(original))

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.userName, roundTripped.userName)
        assertEquals(original.email, roundTripped.email)
        assertEquals(original.fullName, roundTripped.fullName)
        assertEquals(original.age, roundTripped.age)
        assertEquals(original.address, roundTripped.address)
        assertEquals(original.profileIconId, roundTripped.profileIconId)
        assertEquals(original.totalXp, roundTripped.totalXp)
        assertEquals(original.level, roundTripped.level)
        assertEquals(original.currentStreak, roundTripped.currentStreak)
        assertEquals(original.longestStreak, roundTripped.longestStreak)
        assertEquals(original.lastActiveDate, roundTripped.lastActiveDate)
        assertEquals(original.wordsLearned, roundTripped.wordsLearned)
        assertEquals(original.storiesCompleted, roundTripped.storiesCompleted)
        assertEquals(original.gamesPlayed, roundTripped.gamesPlayed)
        assertEquals(original.totalCorrectAnswers, roundTripped.totalCorrectAnswers)
        assertEquals(original.totalQuestionsAnswered, roundTripped.totalQuestionsAnswered)
        assertEquals(original.lessonsCompleted, roundTripped.lessonsCompleted)
        assertEquals(original.isOnboardingCompleted, roundTripped.isOnboardingCompleted)
        assertEquals(original.dailyGoalXp, roundTripped.dailyGoalXp)
        assertEquals(original.dailyXpEarned, roundTripped.dailyXpEarned)
        assertEquals(original.dailyXpDate, roundTripped.dailyXpDate)
        assertEquals(original.titleBadge, roundTripped.titleBadge)
        // updatedAt deliberately not compared — toMap() always overwrites it with "now".
    }
}
