package com.kasiguru.data.repository

import com.kasiguru.data.local.entity.UserProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressSyncTest {

    private fun progress(
        totalXp: Int = 0,
        wordsLearned: Int = 0,
        fullName: String = "",
        updatedAt: Long = 0,
        submissionsMade: Int = 0
    ) = UserProgressEntity(
        userName = fullName.ifBlank { "Learner" },
        fullName = fullName,
        totalXp = totalXp,
        wordsLearned = wordsLearned,
        updatedAt = updatedAt,
        submissionsMade = submissionsMade
    )

    /**
     * submissionsMade was absent from toMap() entirely, so it never reached Firestore and
     * toEntity() — which rebuilds the whole entity rather than patching it — restored it as
     * 0. Signing in on a second device, or reinstalling and restoring from cloud, silently
     * wiped the user's contribution count and the "First Contribution" badge progress
     * resting on it.
     *
     * These pin the behaviour that fixes it: a lifetime counter, merged like the others.
     */
    @Test
    fun submissionsMadeTakesTheMaxLikeOtherLifetimeCounters() {
        val local = progress(submissionsMade = 7, updatedAt = 1)
        val remote = progress(submissionsMade = 3, updatedAt = 2)

        // Remote is newer, but a lifetime total must never move backwards just because the
        // other side synced more recently — that is exactly how the count got lost before.
        assertEquals(7, mergeProgress(local, remote).submissionsMade)
        assertEquals(7, mergeProgress(remote, local).submissionsMade)
    }

    @Test
    fun submissionsMadeSurvivesARemoteWithNoSubmissions() {
        // The realistic reinstall case: cloud predates the field, local has the real count.
        val local = progress(submissionsMade = 4, updatedAt = 1)
        val freshRemote = progress(submissionsMade = 0, updatedAt = 99)

        assertEquals(4, mergeProgress(local, freshRemote).submissionsMade)
    }

    @Test
    fun submissionsMadeRoundTripsThroughTheCloudPayload() {
        // toMap -> toEntity is the actual path a second device takes. If the field is
        // missing from either half it silently reads back as 0.
        val original = progress(submissionsMade = 12, updatedAt = 5)

        assertEquals(12, toEntity(toMap(original)).submissionsMade)
    }

    @Test
    fun countersTakeTheMax() {
        val local = progress(totalXp = 100, wordsLearned = 5, updatedAt = 1)
        val remote = progress(totalXp = 250, wordsLearned = 3, updatedAt = 2)

        val merged = mergeProgress(local, remote)

        assertEquals(250, merged.totalXp)
        assertEquals(5, merged.wordsLearned)
        assertEquals(2, merged.updatedAt)
    }

    @Test
    fun profileComesFromNewerSide() {
        val local = progress(fullName = "Adrian", updatedAt = 1)
        val remote = progress(fullName = "Anthony", updatedAt = 5)

        val merged = mergeProgress(local, remote)

        assertEquals("Anthony", merged.fullName)
    }

    @Test
    fun profileStaysLocalWhenLocalIsNewer() {
        val local = progress(fullName = "Adrian", updatedAt = 9)
        val remote = progress(fullName = "Anthony", updatedAt = 5)

        val merged = mergeProgress(local, remote)

        assertEquals("Adrian", merged.fullName)
    }

    @Test
    fun passwordAndEmailNeverSync() {
        val local = progress().copy(password = "secret", email = "a@b.c")
        val remote = progress(updatedAt = 999)

        val merged = mergeProgress(local, remote)

        assertEquals("secret", merged.password)
        assertEquals("a@b.c", merged.email)
    }

    @Test
    fun onboardingFlagSurvivesMerge() {
        val local = progress().copy(isOnboardingCompleted = true)
        val remote = progress()

        assertEquals(true, mergeProgress(local, remote).isOnboardingCompleted)
    }

    // ── Daily-XP ledger ──────────────────────────────────────────────────────
    //
    // These exist because the ledger was being silently erased: mergeProgress builds a new entity
    // field by field, so a field it does not mention reverts to its default on every sync. The write
    // succeeds and nothing is logged, so only a test catches it.

    private fun ledger(date: String, xp: Int, updatedAt: Long = 0) =
        UserProgressEntity(dailyXpDate = date, dailyXpEarned = xp, updatedAt = updatedAt)

    @Test
    fun dailyLedgerSurvivesAMerge() {
        val local = ledger("2026-08-17", 45, updatedAt = 1)
        val remote = UserProgressEntity(updatedAt = 2) // an older client that never wrote the ledger

        val merged = mergeProgress(local, remote)

        assertEquals(45, merged.dailyXpEarned)
        assertEquals("2026-08-17", merged.dailyXpDate)
    }

    @Test
    fun sameDayTakesTheHigherCount() {
        val merged = mergeProgress(ledger("2026-08-17", 30), ledger("2026-08-17", 75))
        assertEquals(75, merged.dailyXpEarned)
        assertEquals("2026-08-17", merged.dailyXpDate)
    }

    @Test
    fun laterDateWinsOutrightRatherThanAccumulating() {
        // Yesterday's 90 XP must not leak into today's goal ring.
        val merged = mergeProgress(ledger("2026-08-17", 20), ledger("2026-08-16", 90))
        assertEquals(20, merged.dailyXpEarned)
        assertEquals("2026-08-17", merged.dailyXpDate)
    }

    @Test
    fun remoteLaterDateWins() {
        val merged = mergeProgress(ledger("2026-08-16", 90), ledger("2026-08-17", 20))
        assertEquals(20, merged.dailyXpEarned)
        assertEquals("2026-08-17", merged.dailyXpDate)
    }

    @Test
    fun dailyStreakQuotaSurvivesMergeAndRoundTrips() {
        val local = UserProgressEntity(
            dailyReviewCompletedDate = "2026-08-28",
            dailyGamesDate = "2026-08-28",
            dailyGamesPlayedCount = 2,
            updatedAt = 1
        )
        val remote = UserProgressEntity(
            dailyReviewCompletedDate = "2026-08-28",
            dailyGamesDate = "2026-08-28",
            dailyGamesPlayedCount = 3,
            updatedAt = 2
        )

        val merged = mergeProgress(local, remote)
        assertEquals("2026-08-28", merged.dailyReviewCompletedDate)
        assertEquals("2026-08-28", merged.dailyGamesDate)
        assertEquals(3, merged.dailyGamesPlayedCount)

        val roundTrip = toEntity(toMap(merged))
        assertEquals("2026-08-28", roundTrip.dailyReviewCompletedDate)
        assertEquals("2026-08-28", roundTrip.dailyGamesDate)
        assertEquals(3, roundTrip.dailyGamesPlayedCount)
    }
}
