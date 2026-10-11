package com.kasiguru.util.update

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the update pop-up says when Android's installer turns an update down.
 *
 * The installer on its own shows only "App not installed", whatever the reason, and learners took
 * that to mean they had to uninstall KasiGuru. Asked to return its result, it hands back a code
 * instead, and each code the pop-up can act on gets its own explanation.
 */
class UpdateInstallResultTest {

    @Test
    fun `a cancelled or successful install has nothing to explain`() {
        assertNull(installProblemFor(Activity.RESULT_CANCELED, null))
        assertNull(installProblemFor(Activity.RESULT_OK, INSTALL_SUCCEEDED))
        assertNull(installProblemFor(Activity.RESULT_FIRST_USER, INSTALL_SUCCEEDED))
        assertNull(installProblemFor(Activity.RESULT_FIRST_USER, INSTALL_FAILED_ABORTED))
    }

    @Test
    fun `a full phone is told to free space`() {
        assertEquals(InstallProblem.NoSpace(null), installProblemFor(Activity.RESULT_FIRST_USER, INSTALL_FAILED_INSUFFICIENT_STORAGE))
    }

    @Test
    fun `a differently signed copy gets the test build explanation`() {
        assertEquals(InstallProblem.SignedDifferently, installProblemFor(Activity.RESULT_FIRST_USER, INSTALL_FAILED_UPDATE_INCOMPATIBLE))
    }

    @Test
    fun `a file Android cannot read is downloaded again`() {
        assertEquals(InstallProblem.Damaged, installProblemFor(Activity.RESULT_FIRST_USER, INSTALL_FAILED_INVALID_APK))
        assertEquals(InstallProblem.Damaged, installProblemFor(Activity.RESULT_FIRST_USER, INSTALL_FAILED_INVALID_URI))
        assertEquals(InstallProblem.Damaged, installProblemFor(Activity.RESULT_FIRST_USER, -100)) // not an APK
        assertEquals(InstallProblem.Damaged, installProblemFor(Activity.RESULT_FIRST_USER, -103)) // no certificates
    }

    @Test
    fun `any other refusal keeps its code for a bug report`() {
        assertEquals(InstallProblem.Refused(-25), installProblemFor(Activity.RESULT_FIRST_USER, -25)) // version downgrade
        assertEquals(InstallProblem.Refused(-111), installProblemFor(Activity.RESULT_FIRST_USER, -111)) // user restricted
        assertEquals(InstallProblem.Refused(INSTALL_FAILED_INTERNAL_ERROR), installProblemFor(Activity.RESULT_FIRST_USER, null))
    }

    @Test
    fun `an update needs room for the installer's copy and the installed app`() {
        val apk = 41_491_673L // v1.27.0
        assertEquals(0L, storageShortfall(apk, allocatableBytes = 500_000_000L))
        assertEquals(0L, storageShortfall(apk, allocatableBytes = apk * 2))
        assertEquals(apk * 2 - 50_000_000L, storageShortfall(apk, allocatableBytes = 50_000_000L))
        assertEquals(apk * 2, storageShortfall(apk, allocatableBytes = 0L))
    }

    @Test
    fun `the space to free is rounded up to ten megabytes`() {
        assertEquals(10L, megabytesToFree(1L))
        assertEquals(10L, megabytesToFree(10_000_000L))
        assertEquals(40L, megabytesToFree(32_983_346L))
        assertEquals(90L, megabytesToFree(82_983_346L))
    }
}
