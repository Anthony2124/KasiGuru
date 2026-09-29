package com.kasiguru.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Jepjep's Forest colour scheme. Material 3 roles carry the palette so components that resolve their
 * own colours (chips, sheets, text fields, snackbars, stock buttons) land on-system without per-call
 * overrides.
 *
 * [ColorScheme.onPrimary] is [OnLime], not white: white on lime measures 2.3:1. Any Material button
 * given a lime, red, gold or coral container therefore gets a dark label by default.
 *
 * [ColorScheme.outline] stays a readable mid tone because call sites draw borders as
 * `outline.copy(alpha = 0.08f)`; a pale outline would make every one of those borders disappear.
 */
private val forestScheme: ColorScheme = darkColorScheme(
    primary            = Lime,
    onPrimary          = OnLime,
    primaryContainer   = LimeTint,
    onPrimaryContainer = Lime,

    secondary            = Gold,
    onSecondary          = RewardInk,
    secondaryContainer   = Color(0xFF3A2E12),
    onSecondaryContainer = Gold,

    tertiary            = Coral,
    onTertiary          = RewardInk,
    tertiaryContainer   = Color(0xFF3A2710),
    onTertiaryContainer = Coral,

    background = Ground,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceSunken,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Ground,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceSunken,
    surfaceContainerHighest = SurfaceSunken,
    inverseSurface = Cream,
    inverseOnSurface = RewardInk,
    inversePrimary = LimeLip,

    outline = Muted,
    outlineVariant = TrackNeutral,

    error = Red,
    onError = RewardInk,
    errorContainer = RedTint,
    onErrorContainer = Red,

    scrim = Scrim,
    surfaceTint = Color.Transparent
)

/**
 * The app is dark only for now (see DESIGN.md). A cream "Daylight" theme is a later, optional step;
 * the dark-mode preference is kept in storage so it can come back without a migration.
 */
@Composable
fun KasiGuruTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    CompositionLocalProvider(
        LocalReducedMotion provides rememberReducedMotion()
    ) {
        MaterialTheme(
            colorScheme = forestScheme,
            typography = KasiGuruTypography,
            content = content
        )
    }
}

/**
 * Sets whether the system status-bar icons are drawn dark.
 *
 * Every screen sits on night or on a dark scene scrim, so callers pass false. Kept as a function so a
 * screen that ever puts something light behind the status bar can say so.
 */
@Composable
fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = dark
    }
}
