package com.kasiguru.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.kasiguru.domain.preferences.*

@Composable
fun KasiGuruTheme(mode: AppearanceMode = AppearanceMode.SYSTEM, textSizePercent: Int = 100, content: @Composable () -> Unit) {
    val dark = when (mode) { AppearanceMode.SYSTEM -> isSystemInDarkTheme(); AppearanceMode.DARK -> true; AppearanceMode.LIGHT -> false }
    val palette = if (dark) DarkKasiGuruColors else LightKasiGuruColors
    val density = LocalDensity.current
    val scaledDensity = Density(density.density, density.fontScale * TextSize.normalize(textSizePercent) / 100f)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = palette.limeText, onPrimary = if (dark) OnLime else Cream,
        primaryContainer = palette.limeTint, onPrimaryContainer = palette.limeText,
        secondary = palette.goldText, onSecondary = if (dark) RewardInk else Cream,
        secondaryContainer = palette.amberTint, onSecondaryContainer = palette.goldText,
        tertiary = palette.coralText, onTertiary = if (dark) RewardInk else Cream,
        background = palette.ground, onBackground = palette.ink,
        surface = palette.surface, onSurface = palette.ink,
        surfaceVariant = palette.surfaceSunken, onSurfaceVariant = palette.muted,
        surfaceContainerLowest = palette.ground, surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surface, surfaceContainerHigh = palette.surfaceSunken,
        surfaceContainerHighest = palette.surfaceSunken,
        inverseSurface = if (dark) Cream else Color(0xFF142012),
        inverseOnSurface = if (dark) RewardInk else Cream,
        inversePrimary = if (dark) LimeLip else Lime,
        outline = palette.muted, outlineVariant = palette.borderHairline,
        error = palette.redText, onError = if (dark) RewardInk else Cream,
        errorContainer = palette.redTint, onErrorContainer = palette.redText,
        scrim = Scrim, surfaceTint = Color.Transparent
    )
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }
    CompositionLocalProvider(LocalKasiGuruColors provides palette, LocalDensity provides scaledDensity,
        LocalReducedMotion provides rememberReducedMotion(), LocalContentColor provides palette.ink) {
        StatusBarIcons()
        MaterialTheme(colorScheme = scheme, typography = KasiGuruTypography, content = content)
    }
}

@Composable
fun StatusBarIcons(dark: Boolean = !LocalKasiGuruColors.current.isDark) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect { WindowCompat.getInsetsController((view.context as Activity).window, view).isAppearanceLightStatusBars = dark }
}
