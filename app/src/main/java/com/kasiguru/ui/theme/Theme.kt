package com.kasiguru.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Clay Canopy colour scheme. Material 3 roles carry the palette so components that resolve their own
 * colours (chips, sheets, text fields, snackbars) land on-system without per-call-site overrides.
 *
 * Built inside a `@Composable` function rather than as a top-level `val`, because every colour it reads
 * ([Violet], [Ink], [Ground]...) is itself theme-reactive and can only resolve inside composition.
 *
 * Note on [ColorScheme.outline]: it is deliberately a dark ink rather than a light hairline colour,
 * because call sites across the app draw borders as `outline.copy(alpha = 0.08f)`. A pale outline
 * would make every one of those borders disappear.
 */
@Composable
private fun clayCanopyScheme(): ColorScheme = lightColorScheme(
    primary            = Violet,
    onPrimary          = Color.White,
    primaryContainer   = VioletTint,
    onPrimaryContainer = VioletDeep,

    // Gold is a fill that carries RewardInk; it never becomes a foreground on a light surface.
    secondary            = Gold,
    onSecondary          = RewardInk,
    secondaryContainer   = Color(0xFFFFE8BF),
    onSecondaryContainer = Color(0xFF6B4400),

    tertiary            = Coral,
    onTertiary          = RewardInk,
    tertiaryContainer   = Color(0xFFFFDCCB),
    onTertiaryContainer = Color(0xFF7A2E00),

    background = Ground,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceSunken,
    onSurfaceVariant = Muted,

    outline = Muted,
    outlineVariant = TrackNeutral,

    error = Red,
    onError = Color.White,
    errorContainer = RedTint,
    onErrorContainer = RedDeep,

    scrim = Ink,
    surfaceTint = Violet
)

/**
 * Both themes are fully built: [darkTheme] selects a measured dark palette (see `Color.kt`'s `Dark`
 * object) rather than being ignored. Every colour token in the app is declared `@Composable get()` and
 * reads [LocalDarkMode], which this function provides — the whole app follows the same switch.
 */
@Composable
fun KasiGuruTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // Every screen opens with the violet canopy behind the status bar, in both themes, so the
            // system icons must stay light regardless of which theme is active.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalDarkMode provides darkTheme) {
        MaterialTheme(
            colorScheme = clayCanopyScheme(),
            typography = KasiGuruTypography,
            content = content
        )
    }
}
