package com.kasiguru.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which copy of a pronunciation clip plays. A bundled take is only trusted at the exact version the
 * word points at: an admin re-recording must reach learners, with the old take kept as the fallback
 * for when the new one cannot be downloaded (offline, or the day's Firestore reads spent).
 */
class WordAudioPlanTest {

    private val take = BundledClip(version = 1_790_749_180_406L, file = "dalaga__young_woman.1790749180406.m4a")

    @Test
    fun `the bundled take plays when it is the version the word points at`() {
        assertEquals(ClipPlan.Bundled(take), planClip(take, take.version))
    }

    @Test
    fun `a re-recording is downloaded with the bundled take as fallback`() {
        assertEquals(ClipPlan.Download(fallback = take), planClip(take, take.version + 1))
    }

    @Test
    fun `a clip the APK does not hold is downloaded with no fallback`() {
        assertEquals(ClipPlan.Download(fallback = null), planClip(null, take.version))
    }
}
