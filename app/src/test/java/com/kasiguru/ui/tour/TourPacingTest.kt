package com.kasiguru.ui.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which stops wait for a new screen before their caption appears.
 *
 * The middle of the core chapter switches tab at every stop, and showing each caption over a screen
 * still fading in is what made that stretch feel rushed. These pin that the tab switches, and only
 * they, get the longer beat.
 */
class TourPacingTest {

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
