package com.kasiguru.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards where a missed exercise comes back.
 *
 * The failure this prevents is silent: an off-by-one that inserts at or before the current position
 * would show the learner the same item they just got wrong, with the answer still on screen.
 */
class ExpandingRehearsalTest {

    @Test
    fun theGapWidensWithEachMiss() {
        assertEquals(2, ExpandingRehearsal.gapFor(1))
        assertEquals(5, ExpandingRehearsal.gapFor(2))
        assertEquals(10, ExpandingRehearsal.gapFor(3))
    }

    @Test
    fun theGapStopsWideningRatherThanGrowingForever() {
        // Otherwise a word missed five times is pushed past the end of every remaining run and the
        // lesson finishes without it ever being answered.
        assertEquals(10, ExpandingRehearsal.gapFor(4))
        assertEquals(10, ExpandingRehearsal.gapFor(12))
    }

    @Test
    fun aRequeuedItemAlwaysLandsAfterTheCurrentPosition() {
        for (position in 0..20) {
            for (misses in 1..5) {
                val index = ExpandingRehearsal.insertIndex(position, misses, queueSize = 30)
                assertTrue("miss at $position came back at $index", index > position)
            }
        }
    }

    @Test
    fun aMissNearTheEndIsClampedIntoTheQueueRatherThanDropped() {
        // A lesson only ends when every item is answered correctly, so the retry has to exist.
        assertEquals(9, ExpandingRehearsal.insertIndex(currentPosition = 8, missCount = 3, queueSize = 9))
    }

    @Test
    fun theFirstMissComesBackTwoItemsLater() {
        assertEquals(5, ExpandingRehearsal.insertIndex(currentPosition = 3, missCount = 1, queueSize = 12))
    }
}
