package com.kasiguru.ui.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which stops arrive on a new screen, and how long they hold their caption back.
 *
 * Stops on the same screen share one beat (SpotlightOverlay's StepBeatMs). A stop on a new screen
 * lifts the dim while that screen fades in and takes the longer ArrivalBeatMs, so the learner sees
 * where the tour has gone. These pin that the tab switches, and only they, are treated as arrivals.
 */
class TourPacingTest {

    @Test
    fun `a tab switch leaves the new screen in view before its caption`() {
        // navigation-compose crossfades for 700ms. The caption used to land as that ended, which
        // made the Learn, Practice, Library and Me stops feel rushed.
        val crossfadeMs = 700L
        assertTrue(stepBeatMs(newScreen = true) - crossfadeMs >= 400L)
        assertEquals(StepBeatMs, stepBeatMs(newScreen = false))
    }

    private val home = TourTarget.Fixed("home")
    private val learn = TourTarget.Fixed("learn")

    @Test
    fun `a stop on the screen already showing does not wait`() {
        assertFalse(opensNewScreen(previous = home, next = home, alreadyThere = true))
    }

    @Test
    fun `a stop on a different screen waits`() {
        assertTrue(opensNewScreen(previous = home, next = learn, alreadyThere = true))
        assertTrue(opensNewScreen(previous = home, next = learn, alreadyThere = false))
    }

    @Test
    fun `a first stop waits only when its screen is not showing yet`() {
        assertFalse(opensNewScreen(previous = null, next = home, alreadyThere = true))
        assertTrue(opensNewScreen(previous = null, next = learn, alreadyThere = false))
    }

    @Test
    fun `the core chapter waits at each tab switch and nowhere else`() {
        val stops = coreChapter.stops
        val waits = stops.indices.filter { i ->
            i > 0 && opensNewScreen(stops[i - 1].target, stops[i].target, alreadyThere = true)
        }
        // Learn, Practice, Library, Me, and back to Home for the bell.
        assertEquals(listOf("Learn", "Practice", "Library", "Me", "Reminders and news"), waits.map { stops[it].title })
    }
}
