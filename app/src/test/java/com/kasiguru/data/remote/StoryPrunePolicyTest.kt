package com.kasiguru.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the story reconcile rule.
 *
 * The story sync used to only ever insert and update, so a story deleted in the admin portal, or
 * re-created there under a new id, stayed on every device that had already pulled it. The
 * vocabulary side solved this with [pruneWithdrawnWords]; [storiesToPrune] is the same decision for
 * stories, and the awkward cases -- the ten seeded folk tales, an incremental snapshot that is not
 * the whole world -- are the point.
 */
class StoryPrunePolicyTest {

    private val seeded = (1..10).toList()

    @Test
    fun keepsEverythingWhenCloudAndSeedCoverAllLocalRows() {
        val prune = storiesToPrune(
            localIds = listOf(1, 2, 3, 11),
            cloudIds = listOf(1, 2, 3, 11),
            seededIds = seeded
        )
        assertTrue(prune.isEmpty())
    }

    @Test
    fun dropsAStoryMissingFromBothCloudAndSeed() {
        // 11 was pulled from the portal earlier and has since been deleted there.
        val prune = storiesToPrune(
            localIds = listOf(1, 2, 11),
            cloudIds = listOf(1, 2),
            seededIds = seeded
        )
        assertEquals(listOf(11), prune)
    }

    @Test
    fun neverDropsASeededStoryTheCloudHasNeverHeardOf() {
        // Firestore carries no stories at all; the ten shipped tales must survive a full reconcile.
        val prune = storiesToPrune(
            localIds = seeded,
            cloudIds = emptyList(),
            seededIds = seeded
        )
        assertTrue(prune.isEmpty())
    }

    @Test
    fun dropsTheOldRowWhenAStoryIsReCreatedUnderANewId() {
        // Portal story 12 replaced portal story 11; the device still carries both.
        val prune = storiesToPrune(
            localIds = listOf(1, 11, 12),
            cloudIds = listOf(1, 12),
            seededIds = seeded
        )
        assertEquals(listOf(11), prune)
    }
}
