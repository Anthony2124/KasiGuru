package com.kasiguru.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = TextDark,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryDark,
    tertiary = Accent,
    onTertiary = TextWhite,
    tertiaryContainer = AccentContainer,
    onTertiaryContainer = TextDark,
    background = LightThemeBackground,
    onBackground = StaticTextHeadingBlack,
    surface = LightSurfaceCard,
    onSurface = StaticTextHeadingBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = StaticTextSubtleGray,
    error = Error,
    onError = TextWhite,
    errorContainer = ErrorLight,
    onErrorContainer = TextDark,
    outline = StaticTextSubtleGray
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = DarkBackground,
    secondary = Secondary,
    onSecondary = TextDark,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = TextDark,
    tertiary = Accent,
    onTertiary = TextWhite,
    tertiaryContainer = AccentContainer,
    onTertiaryContainer = TextDark,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextLightGray,
    error = Error,
    onError = TextWhite,
    errorContainer = ErrorLight,
    onErrorContainer = TextDark,
    outline = TextDarkGray
)

@Composable
fun KasiGuruTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KasiGuruTypography,
        content = content
    )
}
