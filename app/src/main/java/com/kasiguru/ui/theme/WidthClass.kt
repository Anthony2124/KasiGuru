package com.kasiguru.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * How wide the window is, in Material's own three-bucket scheme (compact / medium / expanded),
 * derived from `screenWidthDp` rather than the official `material3-window-size-class` artifact -
 * that needs an Activity threaded through every screen for `calculateWindowSizeClass`, and this
 * app has no breakpoint handling anywhere yet (confirmed: zero `WindowSizeClass` or
 * `BoxWithConstraints` usage before this file), so a local, dependency-free version is the lower-risk
 * way to add it once, at the shell level, rather than per screen.
 *
 * Breakpoints match Material's compact/medium/expanded window size classes: compact <600dp,
 * medium 600-839dp, expanded >=840dp.
 */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberWidthClass(): WidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> WidthClass.EXPANDED
        widthDp >= 600 -> WidthClass.MEDIUM
        else -> WidthClass.COMPACT
    }
}
