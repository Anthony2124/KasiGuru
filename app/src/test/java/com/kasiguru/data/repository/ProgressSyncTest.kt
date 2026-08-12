package com.kasiguru.data.repository

import com.kasiguru.data.local.entity.UserProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressSyncTest {

    private fun progress(
        totalXp: Int = 0,
        wordsLearned: Int = 0,
        fullName: String = "",
        updatedAt: Long = 0
    ) = UserProgressEntity(
        userName = fullName.ifBlank { "Learner" },
        fullName = fullName,
        totalXp = totalXp,
        wordsLearned = wordsLearned,
        updatedAt = updatedAt
    )

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
}
