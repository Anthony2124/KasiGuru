package com.kasiguru.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/** Theme-aware surfaces and foregrounds. Bright reward/button fills retain their identity. */
@Immutable
data class KasiGuruColors(
    val isDark: Boolean = true,
    val ground: Color = Color(0xFF0A0E0D),
    val surface: Color = Color(0xFF141414),
    val surfaceSunken: Color = Color(0xFF1C211D),
    val trackNeutral: Color = Color(0xFF2C312D),
    val borderHairline: Color = Color(0xFF3A3F3B),
    val glowCore: Color = Color(0xFF0A250B),
    val glowMid: Color = Color(0xFF0A190C),
    val limeTint: Color = Color(0xFF1C2E12),
    val brandLime: Color = Color(0xFF6CB619),
    val ink: Color = Color(0xFFFFFFFF),
    val muted: Color = Color(0xFFC9D1C5),
    val faint: Color = Color(0xFF8C948A),
    val greenTint: Color = Color(0xFF1C2E12),
    val redTint: Color = Color(0xFF3A1E1C),
    val amberTint: Color = Color(0xFF3A2E12),
    val info: Color = Color(0xFF4FB3E8),
    val nodeLocked: Color = Color(0xFF1C211D),
    val nodeLockedInk: Color = Color(0xFF8C948A),
    val pathTrackIdle: Color = Color(0xFF2C312D),
    val vocabSea: Color = Color(0xFF26B5A8),
    val limeText: Color = Color(0xFF71BD1D),
    val goldText: Color = Color(0xFFFFC83D),
    val coralText: Color = Color(0xFFFF9F1C),
    val greenText: Color = Color(0xFF71BD1D),
    val redText: Color = Color(0xFFFF6B5B),
    val amberText: Color = Color(0xFFF5C86A)
)

val DarkKasiGuruColors = KasiGuruColors()
val LightKasiGuruColors = KasiGuruColors(
    isDark = false,
    ground = Color(0xFFF6F4EA),
    surface = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEBEFDF),
    trackNeutral = Color(0xFFD4DDCA),
    borderHairline = Color(0xFFBAC7AE),
    glowCore = Color(0xFFE0EBCF),
    glowMid = Color(0xFFEFF3E6),
    limeTint = Color(0xFFE2EFCF),
    brandLime = Color(0xFF3D7010),
    ink = Color(0xFF142012),
    muted = Color(0xFF52614C),
    faint = Color(0xFF596953),
    greenTint = Color(0xFFE2EFCF),
    redTint = Color(0xFFFBE5DF),
    amberTint = Color(0xFFF8EDD0),
    info = Color(0xFF136793),
    nodeLocked = Color(0xFFEBEFDF),
    nodeLockedInk = Color(0xFF596953),
    pathTrackIdle = Color(0xFFD4DDCA),
    vocabSea = Color(0xFF08766E),
    limeText = Color(0xFF3D7010), goldText = Color(0xFF866000),
    coralText = Color(0xFF9A5000), greenText = Color(0xFF3D7010),
    redText = Color(0xFFA5261A), amberText = Color(0xFF795300)
)

val LocalKasiGuruColors = staticCompositionLocalOf { DarkKasiGuruColors }

val Ground: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.ground
val Surface: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.surface
val SurfaceSunken: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.surfaceSunken
val TrackNeutral: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.trackNeutral
val BorderHairline: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.borderHairline
val GlowCore: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.glowCore
val GlowMid: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.glowMid
val LimeTint: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.limeTint
val BrandLime: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.brandLime
val Ink: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.ink
val Muted: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.muted
val Faint: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.faint
val GreenTint: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.greenTint
val RedTint: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.redTint
val AmberTint: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.amberTint
val Info: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.info
val NodeLocked: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.nodeLocked
val NodeLockedInk: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.nodeLockedInk
val PathTrackIdle: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.pathTrackIdle
val VocabSea: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.vocabSea
val LimeText: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.limeText
val GoldText: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.goldText
val CoralText: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.coralText
val GreenText: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.greenText
val RedText: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.redText
val AmberText: Color @Composable @ReadOnlyComposable get() = LocalKasiGuruColors.current.amberText

// Fixed fills and scene colors are also valid outside composition.
val CanopyTop = Color(0xFF0F3A10)
val CanopyBottom = Color(0xFF0A1A0C)
val OnCanopy = Color(0xFFFFFFFF)
val OnCanopyDecor = Color(0x3DFFFFFF)
val ChipOnCanopy = Color(0x38FFFFFF)
val Lime = Color(0xFF71BD1D)
val LimeLip = Color(0xFF3F6D0E)
val OnLime = Color(0xFF0B1A05)
val Olive = Color(0xFF446025)
val OliveDeep = Color(0xFF2C4418)
val Cream = Color(0xFFF2ECCA)
val ShadowTint = Color(0x66000000)
val RewardInk = Color(0xFF0B1A05)
val Scrim = Color(0xFF000000)
val Coral = Color(0xFFFF9F1C)
val CoralDeep = Color(0xFFC46A00)
val Gold = Color(0xFFFFC83D)
val GoldDeep = Color(0xFFC98A00)
val Green = Color(0xFF71BD1D)
val GreenDeep = Color(0xFF3F6D0E)
val Red = Color(0xFFFF6B5B)
val RedDeep = Color(0xFFC8352B)
val Amber = Color(0xFFF5C86A)
val TierGold = Color(0xFFFFC83D)
val TierGoldDeep = Color(0xFFC98A00)
val TierSilver = Color(0xFFC3C9D8)
val TierSilverDeep = Color(0xFF8E96AA)
val TierBronze = Color(0xFFD08A55)
val TierBronzeDeep = Color(0xFF9C5F2E)
val Warning = Amber
val SkyReview: Color @Composable @ReadOnlyComposable get() = Info
