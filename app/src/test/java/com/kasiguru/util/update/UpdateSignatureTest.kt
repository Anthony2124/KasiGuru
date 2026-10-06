package com.kasiguru.util.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the update banner stops short of the installer.
 *
 * A build from Android Studio is signed with the debug key, every release with the release key, and
 * Android refuses to install one over the other with "App not installed as package conflicts with
 * an existing package". The banner explains that instead, but only when it is sure: a signature it
 * could not read must never stand between a learner and an update.
 */
class UpdateSignatureTest {

    private val releaseKey = "f11e934f5da83c70e61183d5bd1e041221fa4bda6dad768df46e1b638d53f007"
    private val debugKey = "0a1b2c3d"
    private val rotatedFrom = "9f8e7d6c"

    @Test
    fun `an official update over an official install goes to the installer`() {
        assertFalse(signersDiffer(setOf(releaseKey), setOf(releaseKey)))
    }

    @Test
    fun `an official update over a debug build is caught`() {
        assertTrue(signersDiffer(setOf(debugKey), setOf(releaseKey)))
    }

    @Test
    fun `an update whose key was rotated from the installed one still installs`() {
        assertFalse(signersDiffer(setOf(rotatedFrom), setOf(releaseKey, rotatedFrom)))
    }

    @Test
    fun `a signature that could not be read never blocks the update`() {
        assertFalse(signersDiffer(null, setOf(releaseKey)))
        assertFalse(signersDiffer(setOf(releaseKey), null))
        assertFalse(signersDiffer(emptySet(), setOf(releaseKey)))
    }
}
