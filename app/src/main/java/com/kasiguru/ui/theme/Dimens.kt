package com.kasiguru.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The single spacing / radius / elevation scale for KasiGuru.
 *
 * Before this existed the codebase mixed 14, 18, 20, 22, 24, 26 and 28 dp corner radii on adjacent
 * surfaces, which is the main reason the old screens read as "a pile of cards" rather than one
 * designed system. Every new surface picks from these steps and nothing else.
 */
object Space {
    /** Hairline gaps inside a chip or between an icon and its label. */
    val xxs = 4.dp
    /** Tight internal padding; gap between stacked lines of text. */
    val xs = 8.dp
    /** Default gap between sibling controls in a row. */
    val sm = 12.dp
    /** Standard content padding and the default gap between list items. */
    val md = 16.dp
    /** Gap between distinct groups inside one section. */
    val lg = 24.dp
    /** Gap between major sections of a screen. */
    val xl = 32.dp
    /** Breathing room above a screen's primary heading or below a hero. */
    val xxl = 48.dp

    /** Horizontal page gutter. Every full-width screen uses this, so edges line up across tabs. */
    val gutter = 20.dp

    /**
     * Trailing breathing room at the end of a scrollable screen.
     *
     * Deliberately small: the navigation shell insets the whole NavHost by the height of the bar
     * and its raised action, so screens no longer guess at it. Content padding alone could never
     * solve the overlap anyway - it only adds scroll room at the end, so a short screen still had
     * the floating action sitting on top of it.
     */
    val navBarClearance = 24.dp
}

/**
 * Corner radii. Four steps only — a surface is either a control, a tile, a panel, or a sheet.
 */
object Radius {
    /** Chips, badges, small inputs. */
    val chip = 14.dp
    /** Tiles, list rows, compact surfaces. */
    val tile = 20.dp
    /** Panels, hero surfaces, cards that own a whole section. */
    val panel = 28.dp
    /** Bottom sheets and full-bleed surfaces anchored to a screen edge. */
    val sheet = 32.dp
    /** Fully rounded: pills, avatars, FABs. */
    val pill = 999.dp
}

object Shapes {
    val chip = RoundedCornerShape(Radius.chip)
    val tile = RoundedCornerShape(Radius.tile)
    val panel = RoundedCornerShape(Radius.panel)
    val pill = RoundedCornerShape(Radius.pill)
    /** Sheets round the top edge only; the bottom is flush with the screen. */
    val sheetTop = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
}

/**
 * Elevation. The light Casiguran Coast system leans on tinted borders rather than heavy shadows,
 * so these stay deliberately low; anything above [Elevation.floating] is a bug.
 */
object Elevation {
    val flat = 0.dp
    val raised = 2.dp
    val overlay = 6.dp
    val floating = 12.dp
}

/** Minimum interactive size, per the Material accessibility guideline. */
object Touch {
    val minTarget = 48.dp
}

/**
 * Clay geometry. Compose has no inset-shadow API, so claymorphism's "double shadow" is built from
 * three real layers rather than faked with a single drop shadow:
 *
 *  1. the **lip** — a solid darker shape offset down by [lip], drawn behind the face;
 *  2. the **lit top** — a white gradient across the upper [litFraction] of the face;
 *  3. the **cast shadow** — a violet-tinted shadow with both offset and blur.
 *
 * Pressing compresses the object: the lip shrinks to [lipPressed] and the face drops by the
 * difference, so the thing physically squashes instead of merely dimming.
 */
object Clay {
    /** Height of the solid darker base beneath a clay object at rest. */
    val lip = 5.dp
    /** Lip height while pressed. The face travels (lip - lipPressed) downward. */
    val lipPressed = 1.dp
    /** Cast-shadow elevation for a clay object at rest. */
    val shadow = 10.dp
    /** Cast-shadow elevation while pressed — the object sits closer to the surface. */
    val shadowPressed = 3.dp
    /** Fraction of the face height covered by the lit-top gradient. */
    const val litFraction = 0.45f
    /** Alpha of the lit-top highlight where it meets the top edge. */
    const val litAlpha = 0.22f

    /** Press-down duration, in milliseconds. Fast, so the control feels mechanical. */
    const val pressDownMs = 120
    /** Release duration. Slightly slower than the press so the rebound reads as weight. */
    const val pressUpMs = 180
}
