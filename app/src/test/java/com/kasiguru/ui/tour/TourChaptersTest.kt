package com.kasiguru.ui.tour

import com.kasiguru.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the invariants that hold the guided tour together across three files that the compiler cannot
 * relate to each other: the stop list here, the routes in [Screen], and the anchors that
 * `KasiGuruBottomBar` and `LearnScreen` actually attach.
 *
 * The failure this guards against is silent rather than loud. A stop pointing at a route the bottom
 * bar does not show would still compile and still run - it would simply dim the screen, cut no hole,
 * and leave the learner reading a caption about something they cannot see.
 */
class TourChaptersTest {

    /** The five destinations `NavGraph` shows the bottom bar on. A tour stop cannot live elsewhere. */
    private val tabRoutes = Screen.tabRoots

    /** Fixed routes only; a resolved target has no route until the chapter starts. */
    private fun TourStop.fixedRoute(): String? = (target as? TourTarget.Fixed)?.route

    private val coreStops get() = coreChapter.stops

    @Test
    fun `every stop targets a tab root`() {
        coreStops.forEach { stop ->
            assertTrue(
                "Stop '${stop.title}' targets ${stop.fixedRoute()}, which is not one of the five tab roots",
                stop.fixedRoute() in tabRoutes
            )
        }
    }

    @Test
    fun `tour stays inside the length a first-run tour can hold attention for`() {
        // Nine is the ceiling, and it is the *only* chapter anyone is made to see. Everything past
        // this is opt-in from the help page, which is what lets the tutorial cover every screen
        // without ever asking a new learner for more than about ninety seconds.
        assertTrue("The core chapter is what everyone sees; past nine stops it gets abandoned", coreStops.size <= 9)
        assertTrue("A tour needs enough stops to cover the five tabs", coreStops.size >= 6)
    }

    @Test
    fun `every tab is introduced exactly once`() {
        val introduced = coreStops
            .filter { it.anchor?.name?.startsWith("Nav") == true }
            .mapNotNull { it.fixedRoute() }
        assertEquals(
            "Each tab should be introduced by exactly one nav-anchored stop",
            tabRoutes,
            introduced.toSet()
        )
        assertEquals("No tab should be introduced twice", introduced.size, introduced.toSet().size)
    }

    @Test
    fun `tour opens and closes on Learn`() {
        assertEquals(Screen.Learn.route, coreStops.first().fixedRoute())
        assertEquals(Screen.Learn.route, coreStops.last().fixedRoute())
    }

    @Test
    fun `no stop hardcodes a corpus count`() {
        // The dictionary grows through the admin portal. A number written into tour copy goes stale
        // silently, exactly as "487 vocabulary entries" did in the FAQ.
        val digits = Regex("\\d")
        coreStops.forEach { stop ->
            assertTrue(
                "Stop '${stop.title}' hardcodes a number; the corpus and badge counts are live",
                !digits.containsMatchIn(stop.body)
            )
        }
    }

    @Test
    fun `each stop carries copy worth reading`() {
        coreStops.forEach { stop ->
            assertTrue("A stop needs a title", stop.title.isNotBlank())
            assertTrue(
                "Stop '${stop.title}' should say more than its own name",
                stop.body.length > 40
            )
        }
    }

    @Test
    fun `every chapter appears exactly once`() {
        val ids = tourChapters.map { it.id }
        assertEquals("A chapter id must not be listed twice", ids.size, ids.toSet().size)
    }

    @Test
    fun `no stop points inside a dialog`() {
        // Dialogs are separate windows: the overlay draws under them and their bounds are measured
        // from the dialog's own origin, so a hole cut from one lands in the corner of the app.
        tourChapters.forEach { chapter ->
            chapter.stops.forEach { stop ->
                assertTrue(
                    "Stop '${stop.title}' in ${chapter.id} anchors inside a dialog",
                    stop.anchor !in DialogAnchors
                )
            }
        }
    }

    @Test
    fun `no chapter is longer than a learner will finish`() {
        tourChapters.forEach { chapter ->
            assertTrue(
                "Chapter ${chapter.id} has ${chapter.stops.size} stops; past eight they get abandoned",
                chapter.stops.size <= 9
            )
            assertTrue("Chapter ${chapter.id} has no stops", chapter.stops.isNotEmpty())
        }
    }

    @Test
    fun `every fixed target names a real route`() {
        // Read Screen.kt rather than reflecting over the sealed class: kotlin-reflect is not on the
        // unit-test classpath, and adding it for one assertion would be a dependency per test.
        val source = File(sourceRoot(), "ui/navigation/Screen.kt").readText()
        val declared = Regex(""": Screen\("([^"]+)"\)""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()

        assertTrue("Failed to parse any routes out of Screen.kt", declared.size > 10)

        tourChapters.forEach { chapter ->
            chapter.stops.mapNotNull { it.fixedRoute() }.forEach { route ->
                assertTrue(
                    "$route is used by a tour stop but is not declared in Screen.kt",
                    route in declared
                )
            }
        }
    }

    /**
     * Walks up from the test's working directory to the app's main source root.
     *
     * Same approach as [com.kasiguru.data.repository.IssueReportRulesParityTest], which needs to read
     * firestore.rules for the same reason: a unit test's working directory is not the repository root
     * and is not guaranteed to be stable across Gradle versions.
     */
    private fun sourceRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")!!).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main/java/com/kasiguru")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("main source root not found above ${System.getProperty("user.dir")}")
    }
}
