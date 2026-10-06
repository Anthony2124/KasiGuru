package com.kasiguru.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Guards the interval logic behind content syncing.
 *
 * This decision is what stands between the app and the Spark plan's 50,000 reads/day, a
 * ceiling shared project-wide by every user. The dictionary is ~1,100 documents, so a full
 * pull costs ~1,100 reads and roughly forty-five of them exhausts the day for everyone.
 * Getting this wrong is not a cosmetic bug: too eager and the whole backend stops answering;
 * too lazy and dictionary edits never reach anyone.
 *
 * The awkward cases here are the point. A plain `elapsed >= interval` would be wrong on a
 * fresh install, on a first run, and on a device whose clock moved backwards.
 */
class ContentSyncPolicyTest {

    private val sixHours = TimeUnit.HOURS.toMillis(6)
    private val oneWeek = TimeUnit.DAYS.toMillis(7)
    private val now = 1_700_000_000_000L

    @Test
    fun syncsWhenThereIsNoLocalContent() {
        // Fresh install or cleared data: nothing to read offline, so the interval must not
        // apply even though a sync ran seconds ago.
        assertTrue(
            isDueForSync(
                hasLocalContent = false,
                lastRunAt = now - 1_000L,
                now = now,
                intervalMs = sixHours
            )
        )
    }

    @Test
    fun syncsWhenItHasNeverRun() {
        assertTrue(isDueForSync(true, lastRunAt = 0L, now = now, intervalMs = sixHours))
    }

    @Test
    fun treatsNegativeTimestampAsNeverRun() {
        // Not reachable through DataStore today, but a stored -1 must not read as "ran in
        // 1969", which would make elapsed enormous and sync forever.
        assertTrue(isDueForSync(true, lastRunAt = -1L, now = now, intervalMs = sixHours))
    }

    @Test
    fun skipsWhenTheIntervalHasNotElapsed() {
        assertFalse(
            isDueForSync(true, lastRunAt = now - (sixHours - 1), now = now, intervalMs = sixHours)
        )
    }

    @Test
    fun syncsExactlyOnTheInterval() {
        // Boundary is inclusive: at exactly six hours the sync is due, not one millisecond later.
        assertTrue(
            isDueForSync(true, lastRunAt = now - sixHours, now = now, intervalMs = sixHours)
        )
    }

    @Test
    fun syncsAfterTheIntervalHasPassed() {
        assertTrue(
            isDueForSync(true, lastRunAt = now - (sixHours * 3), now = now, intervalMs = sixHours)
        )
    }

    @Test
    fun syncsWhenTheClockMovedBackwards() {
        // The stored timestamp is in the future relative to `now` — a timezone change, a
        // manual clock set, or a device whose clock was wrong when the value was written.
        // Without this branch the throttle stays shut until real time catches up, which for
        // a badly wrong clock can be years.
        assertTrue(
            isDueForSync(true, lastRunAt = now + TimeUnit.DAYS.toMillis(365), now = now, intervalMs = sixHours)
        )
    }

    @Test
    fun theWeeklyReconcileUsesTheSameRuleWithALongerInterval() {
        // Six hours since the last full reconcile is nowhere near due...
        assertFalse(isDueForSync(true, lastRunAt = now - sixHours, now = now, intervalMs = oneWeek))
        // ...but eight days is.
        assertTrue(
            isDueForSync(true, lastRunAt = now - TimeUnit.DAYS.toMillis(8), now = now, intervalMs = oneWeek)
        )
    }

    @Test
    fun anIncrementalSyncCanBeDueWhileAFullReconcileIsNot() {
        // The ordinary case that makes the whole design work: a day after the last full
        // pull, the cheap incremental sync runs and the expensive one stays skipped.
        val aDayAgo = now - TimeUnit.DAYS.toMillis(1)
        assertTrue(isDueForSync(true, lastRunAt = aDayAgo, now = now, intervalMs = sixHours))
        assertFalse(isDueForSync(true, lastRunAt = aDayAgo, now = now, intervalMs = oneWeek))
    }

    // ── The daily reconcile's fingerprint check ───────────────────────────────────────────
    // Reading every word on every phone every day cost ~1,150 reads each, and with a few dozen
    // phones that alone came close to the 50,000. The reconcile now reads everything only when
    // the collection's count-and-checksum moved, so these cases decide most of the daily bill.

    private val thirtyDays = TimeUnit.DAYS.toMillis(30)
    private val yesterday = now - TimeUnit.DAYS.toMillis(1)

    private fun needsFullRead(
        stored: String? = "1151:1987000000000000",
        current: String? = "1151:1987000000000000",
        lastFullReadAt: Long = yesterday,
        force: Boolean = false,
        hasLocalContent: Boolean = true
    ) = needsFullVocabularyRead(force, hasLocalContent, stored, current, lastFullReadAt, now, thirtyDays)

    @Test
    fun anUnchangedDictionarySkipsTheFullRead() {
        assertFalse(needsFullRead())
    }

    @Test
    fun anyAddDeleteOrEditMovesTheFingerprintAndReadsEverything() {
        // A deleted word lowers the count; an edited one changes the checksum. Either way the
        // incremental pull cannot be trusted to have seen it.
        assertTrue(needsFullRead(current = "1150:1985000000000000"))
        assertTrue(needsFullRead(current = "1151:1987000000000123"))
    }

    @Test
    fun aPhoneWithoutAStoredFingerprintReadsEverythingOnce() {
        // First run, or the first reconcile after updating from a version that never took one.
        assertTrue(needsFullRead(stored = null))
        assertTrue(needsFullRead(stored = ""))
    }

    @Test
    fun aFailedFingerprintFallsBackToTheFullRead() {
        // The aggregation failed: behave exactly as before this check existed.
        assertTrue(needsFullRead(current = null))
    }

    @Test
    fun aForcedRefreshOrAnEmptyDeviceAlwaysReadsEverything() {
        assertTrue(needsFullRead(force = true))
        assertTrue(needsFullRead(hasLocalContent = false))
    }

    @Test
    fun aNewParserVersionReadsEverythingOnceThenSettles() {
        // 1.24 and 1.25 stored the bare "count:sum" and never parsed `theme`. The versioned
        // fingerprint cannot match it, so each phone reads the collection once more and gets the
        // section tags; after that read it stores the versioned value and settles back down.
        val legacy = "1151:1987000000000000"
        val current = versionedFingerprint(legacy)
        assertTrue(needsFullRead(stored = legacy, current = current))
        assertFalse(needsFullRead(stored = current, current = current))
    }

    @Test
    fun theSafetyIntervalStillForcesAFullReadEveryThirtyDays() {
        assertFalse(needsFullRead(lastFullReadAt = now - TimeUnit.DAYS.toMillis(29)))
        assertTrue(needsFullRead(lastFullReadAt = now - TimeUnit.DAYS.toMillis(31)))
        // Never read in full, or a clock that has since moved backwards.
        assertTrue(needsFullRead(lastFullReadAt = 0L))
        assertTrue(needsFullRead(lastFullReadAt = now + TimeUnit.DAYS.toMillis(2)))
    }
}
