package com.kasiguru.ui.navigation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every integer route argument is declared to the navigation graph as `NavType.IntType`.
 *
 * This covers a defect that shipped and only surfaced in a release-build tap-through: `story/{storyId}`
 * was the one numeric route in the graph that never declared
 * `navArgument("storyId") { type = NavType.IntType }`. Navigation then stored the argument as a
 * String, and `SavedStateHandle` threw `ClassCastException: String cannot be cast to Number` the
 * moment a story opened. Nothing caught it, because the crash is at the destination, not at
 * registration.
 *
 * A Compose navigation test would need an emulator. This reads the two source files instead —
 * `Screen.kt` for which arguments are integers (its `createRoute` signatures say so) and `NavGraph.kt`
 * for what each `composable` block declares — so the guard runs in the fast JVM suite on every push.
 */
class RouteArgumentTypeTest {

    private val navPackageDir = listOf(
        "src/main/java/com/kasiguru/ui/navigation",
        "app/src/main/java/com/kasiguru/ui/navigation"
    ).map(::File).firstOrNull { it.isDirectory }
        ?: error("could not locate the navigation package from ${File("").absolutePath}")

    private val screenSource = File(navPackageDir, "Screen.kt").readText()
    private val navGraphSource = File(navPackageDir, "NavGraph.kt").readText()

    private data class IntArg(val screen: String, val arg: String)

    /**
     * Screen objects that put an Int into their route, as (object name, placeholder name).
     *
     * A placeholder counts as an Int when the object's `createRoute` takes a parameter of that name
     * typed `Int` — the same source of truth a caller uses.
     */
    private fun integerRouteArgs(): List<IntArg> {
        val screenObject = Regex("""data object (\w+)\s*:\s*Screen\("([^"]+)"\)\s*(\{[\s\S]*?\n {4}}|)""")
        val param = Regex("""(\w+)\s*:\s*(Int|String)""")

        return screenObject.findAll(screenSource).flatMap { match ->
            val (name, pattern, body) = match.destructured
            val placeholders = Regex("""\{(\w+)}""").findAll(pattern).map { it.groupValues[1] }.toSet()
            if (placeholders.isEmpty()) return@flatMap emptySequence<IntArg>()

            val createRoute = Regex("""fun createRoute\(([^)]*)\)""").find(body)?.groupValues?.get(1).orEmpty()
            val intParams = param.findAll(createRoute)
                .filter { it.groupValues[2] == "Int" }
                .map { it.groupValues[1] }
                .toSet()

            placeholders.asSequence().filter { it in intParams }.map { IntArg(name, it) }
        }.toList()
    }

    /** The `composable(...)` block that registers [screen], bounded by the next `composable(`. */
    private fun composableBlockFor(screen: String): String? {
        val startMarker = Regex(
            """composable\(\s*(?:route\s*=\s*)?Screen\.${Regex.escape(screen)}\.route"""
        ).find(navGraphSource) ?: return null

        val afterOpen = navGraphSource.substring(startMarker.range.first + "composable(".length)
        val next = afterOpen.indexOf("composable(")
        return if (next < 0) afterOpen else afterOpen.substring(0, next)
    }

    @Test
    fun `every integer route argument declares NavType_IntType`() {
        val intArgs = integerRouteArgs()
        // The story id, the word id, the lesson index and six game levels are all integers today;
        // if the parser sees fewer than a handful it has drifted from Screen.kt's shape.
        assertTrue(
            "parsed no integer route arguments — the Screen.kt parser has drifted",
            intArgs.size >= 4
        )

        val undeclared = intArgs.filter { (screen, arg) ->
            val block = composableBlockFor(screen) ?: return@filter true
            !block.contains(Regex("""navArgument\("${Regex.escape(arg)}"\)\s*\{[^}]*NavType\.IntType"""))
        }

        assertTrue(
            "these integer route arguments are not declared NavType.IntType in NavGraph.kt, so " +
                "navigation stores them as strings and the destination crashes on read: " +
                undeclared.joinToString { "${it.screen}.${it.arg}" },
            undeclared.isEmpty()
        )
    }
}
