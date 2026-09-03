package com.kasiguru.ui.tour

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Proves that every anchor the tour points at is still attached to something on screen.
 *
 * This is the regression that would otherwise ship in silence. `Modifier.tourAnchor` is one call on
 * one element; delete or restructure that element during an unrelated redesign and the tour does not
 * fail, it just dims the screen and cuts no hole - the caption still talks confidently about a
 * control the learner cannot see highlighted. Nothing in the compiler, the app, or a manual pass over
 * the *other* screens would catch it.
 *
 * There are no Compose UI tests in this project and this does not need them: attachment is a fact
 * about the source, so the source is what gets read.
 */
class TourAnchorAttachmentTest {

    private val strictCall = Regex("""\.tourAnchor\(\s*(?:TourAnchor\.)?(\w+)""")

    /**
     * Anchors named in a file that also attaches anchors, without the name appearing in a direct call.
     *
     * `KasiGuruBottomBar` is why this exists. It attaches its five nav anchors indirectly -
     * `Modifier.tourAnchor(it.tourAnchor)`, where `it` is a `BottomNavItem` whose anchor was set in a
     * list literal further up. A strict regex sees none of NavLearn..NavProfile and fails on code that
     * is perfectly correct.
     *
     * The looser rule would pass a file that merely imports an anchor without attaching it. That is
     * the right trade: a guard that fails on correct code gets deleted, and then the silent
     * degradation it was written to prevent comes straight back.
     */
    private val looseMention = Regex("""TourAnchor\.(\w+)""")

    private fun sourceRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")!!).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main/java/com/kasiguru")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("main source root not found above ${System.getProperty("user.dir")}")
    }

    private fun attachedAnchorNames(): Set<String> {
        val root = sourceRoot()
        val kotlinFiles = root.walkTopDown().filter { it.isFile && it.extension == "kt" }

        val attached = mutableSetOf<String>()
        for (file in kotlinFiles) {
            // The tour's own package declares the anchors and the chapters; it attaches nothing.
            if (file.parentFile.name == "tour") continue

            val text = file.readText()
            if (!text.contains(".tourAnchor(")) continue

            strictCall.findAll(text).forEach { attached += it.groupValues[1] }
            looseMention.findAll(text).forEach { attached += it.groupValues[1] }
        }
        return attached
    }

    @Test
    fun `every anchor a stop points at is attached somewhere on screen`() {
        val attached = attachedAnchorNames()
        assertTrue(
            "Found no tourAnchor call sites at all - the scan is broken, not the app",
            attached.isNotEmpty()
        )

        val missing = tourChapters
            .flatMap { chapter -> chapter.stops.mapNotNull { it.anchor?.let { a -> chapter to a } } }
            .filter { (_, anchor) -> anchor.name !in attached }

        assertTrue(
            "These anchors are used by a tour stop but attached nowhere, so the spotlight would " +
                "dim the screen and cut no hole: " +
                missing.joinToString { (chapter, anchor) -> "${chapter.id}/${anchor.name}" },
            missing.isEmpty()
        )
    }

    @Test
    fun `every anchor is used by at least one stop`() {
        // The other direction. An anchor nothing points at is dead weight: it still measures itself
        // on every layout pass while a tour is running, for a hole no chapter will ever cut.
        val used = tourChapters.flatMap { it.stops }.mapNotNull { it.anchor?.name }.toSet()
        val unused = TourAnchor.entries.map { it.name }.filter { it !in used }

        assertTrue(
            "These anchors are declared and attached but no stop points at them: $unused",
            unused.isEmpty()
        )
    }
}
