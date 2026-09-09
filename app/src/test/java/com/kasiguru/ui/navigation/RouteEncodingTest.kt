package com.kasiguru.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URLDecoder

/**
 * Route arguments survive the category names the app actually has.
 *
 * This covers a defect that shipped: every Dictionary category screen came up empty because names
 * like "Body Parts & Health" were dropped into the route unencoded, and the ampersand truncated the
 * argument. It was invisible in review and obvious the moment anyone tapped a category — the exact
 * shape of bug that unit tests catch cheaply and manual checking does not, because nothing crashes
 * and the screen renders perfectly well while showing nothing.
 *
 * These run on the JVM: `Screen` deliberately encodes with `java.net.URLEncoder` rather than
 * `android.net.Uri`, so the whole navigation contract is testable without an emulator.
 */
class RouteEncodingTest {

    /** Every real vocabulary category, as the seeder spells them. */
    private val categories = listOf(
        "Greetings & Essentials",
        "Body Parts & Health",
        "House & Daily Life",
        "Nature & Environment",
        "Food & Dining",
        "Numbers & Time",
        "Animals & Wildlife",
        "Emotions & Feelings",
        "Family & People",
        "Occupations & Tools",
        "Colors & Shapes",
        "Weather & Climate"
    )

    @Test
    fun `every category round-trips through its own route`() {
        for (category in categories) {
            val route = Screen.VocabularyCategory.createRoute(category)
            val arg = route.removePrefix("vocabulary/category/")
            assertEquals(
                "category $category did not survive its route",
                category,
                URLDecoder.decode(arg, "UTF-8")
            )
        }
    }

    @Test
    fun `no route argument carries a raw separator`() {
        for (category in categories) {
            val arg = Screen.VocabularyCategory.createRoute(category)
                .removePrefix("vocabulary/category/")
            // A raw & ends the argument, a raw space ends the whole route, and a raw / invents a
            // path segment. Any of the three silently routes somewhere that does not exist.
            assertFalse("raw & in $arg", arg.contains("&"))
            assertFalse("raw space in $arg", arg.contains(" "))
            assertFalse("raw / in $arg", arg.contains("/"))
        }
    }

    @Test
    fun `a plus in a name is encoded rather than becoming a space`() {
        // URLEncoder emits + for a space, so a literal + has to come back as %2B or decoding turns
        // it into a space. Screen re-encodes spaces to %20 precisely so this stays unambiguous.
        val route = Screen.VocabularyCategory.createRoute("Health + Safety")
        val arg = route.removePrefix("vocabulary/category/")
        assertEquals("Health + Safety", URLDecoder.decode(arg, "UTF-8"))
    }

    @Test
    fun `lesson routes keep the unit id and the index apart`() {
        val route = Screen.LessonPlayer.createRoute("Body Parts & Health", 3)
        val parts = route.split("/")
        // "lesson", encoded unit id, index — three segments, because the encoded id cannot
        // contribute a slash of its own.
        assertEquals(3, parts.size)
        assertEquals("lesson", parts[0])
        assertEquals("Body Parts & Health", URLDecoder.decode(parts[1], "UTF-8"))
        assertEquals("3", parts[2])
    }

    @Test
    fun `route patterns and built routes agree on segment count`() {
        // A built route with a different number of segments than its pattern matches nothing, which
        // navigates the learner to a blank screen rather than failing loudly.
        assertEquals(
            Screen.VocabularyCategory.route.count { it == '/' },
            Screen.VocabularyCategory.createRoute("Food & Dining").count { it == '/' }
        )
        assertEquals(
            Screen.LessonPlayer.route.count { it == '/' },
            Screen.LessonPlayer.createRoute("Food & Dining", 0).count { it == '/' }
        )
    }
}
