package com.kasiguru.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * KasiGuru "Clay Canopy" palette. See DESIGN.md for the full contract.
 *
 * Every token below is theme-reactive: it reads [LocalDarkMode] and resolves to a light or dark value,
 * both independently measured with the WCAG relative-luminance formula (the validator lives in the
 * scratchpad; the numbers are reproducible). A token is declared `@Composable get()` specifically so it
 * can react to the current theme while still being referenced exactly like a constant everywhere it
 * already is (`color = Ink`, `face = Gold`, a default parameter value) — the read just has to happen in
 * composition, which every existing call site already is.
 *
 * One deliberate exception: [Gold], [Coral], [Tier variants and their Ink-pairing. These reward/callout
 * fills keep the *same* hex value in both themes — a badge or the primary reward button should not
 * change hue when the system theme flips. What changes instead is the ink text/icon painted on top of
 * them: light mode's dark [Ink] cannot simply follow [Ink] into its dark-mode near-white value, or every
 * "ink on gold" pairing would flip to unreadable near-white-on-gold. Those specific fill+ink pairings use
 * [RewardInk] instead, a fixed dark tone that never themes. See the fill tokens' own doc comments.
 */

/** Provided by [com.kasiguru.ui.theme.KasiGuruTheme]; true when dark mode is the active preference. */
val LocalDarkMode = compositionLocalOf { false }

private object Light {
    val Ground        = Color(0xFFF1EEFF)
    val Surface       = Color(0xFFFFFFFF)
    val SurfaceSunken = Color(0xFFE8E4F8)
    val TrackNeutral  = Color(0xFFE0DBF5)
    val BorderHairline = Color(0x141F1B3A)

    val CanopyTop    = Color(0xFF6C5CE7)
    val CanopyBottom = Color(0xFF4A3FC0)
    val OnCanopyDecor = Color(0x3DFFFFFF)
    val ChipOnCanopy  = Color(0x38FFFFFF)

    val Violet       = Color(0xFF5B4CDB)
    val VioletDeep   = Color(0xFF4034A8)
    val VioletTint   = Color(0xFFE7E3FF)
    val VioletShadow = Color(0x335B4CDB)

    val Ink   = Color(0xFF1F1B3A)
    val Muted = Color(0xFF5E5A80)
    val Faint = Color(0xFF8A86A6)

    val GreenTint = Color(0xFFE3F5E9)
    val RedTint   = Color(0xFFFDEAEC)
    val Amber     = Color(0xFF8A5A00)
    val AmberTint = Color(0xFFFFF1D6)

    val NodeLocked    = Color(0xFFDCD8EE)
    val NodeLockedInk = Color(0xFF8A86A6)
    val PathTrackIdle = Color(0xFFDFDAF3)
}

private object Dark {
    val Ground        = Color(0xFF15131F)
    val Surface       = Color(0xFF1E1B30)
    val SurfaceSunken = Color(0xFF282440)
    val TrackNeutral  = Color(0xFF332E4D)
    val BorderHairline = Color(0x1FF1EEFF)

    // The canopy is already a vivid, self-contained accent surface — it does not need a dark variant.
    val CanopyTop    = Light.CanopyTop
    val CanopyBottom = Light.CanopyBottom
    val OnCanopyDecor = Light.OnCanopyDecor
    val ChipOnCanopy  = Light.ChipOnCanopy

    val Violet       = Color(0xFF9C90F5)
    val VioletDeep   = Color(0xFF6C5CE7)
    val VioletTint   = Color(0xFF2A2660)
    val VioletShadow = Color(0x40241C57)

    val Ink   = Color(0xFFF1EEFF)
    val Muted = Color(0xFFB7B2D6)
    val Faint = Color(0xFF827DA0)

    val GreenTint = Color(0xFF1B3324)
    val RedTint   = Color(0xFF3A1E20)
    val Amber     = Color(0xFFF5C86A)
    val AmberTint = Color(0xFF3A2E12)

    val NodeLocked    = Color(0xFF332E4D)
    val NodeLockedInk = Color(0xFF8983A8)
    val PathTrackIdle = Color(0xFF3A3555)
}

// ─────────────────────────────────────────────────────────────────────────────
// Ground and surfaces
// ─────────────────────────────────────────────────────────────────────────────
/** App background. Soft lavender in light mode, near-black violet in dark; white/surface cards float. */
val Ground: Color @Composable get() = if (LocalDarkMode.current) Dark.Ground else Light.Ground
/** Cards, rows, sheets. */
val Surface: Color @Composable get() = if (LocalDarkMode.current) Dark.Surface else Light.Surface
/** Recessed wells: search fields, inactive segments, progress tracks. */
val SurfaceSunken: Color @Composable get() = if (LocalDarkMode.current) Dark.SurfaceSunken else Light.SurfaceSunken
/** Neutral progress track on a surface. */
val TrackNeutral: Color @Composable get() = if (LocalDarkMode.current) Dark.TrackNeutral else Light.TrackNeutral
/** Hairline separator, used instead of a shadow wherever only a boundary is needed. */
val BorderHairline: Color @Composable get() = if (LocalDarkMode.current) Dark.BorderHairline else Light.BorderHairline

// ─────────────────────────────────────────────────────────────────────────────
// The canopy — the deep violet panel every screen opens with. Unchanged across themes.
// ─────────────────────────────────────────────────────────────────────────────
val CanopyTop    = Light.CanopyTop
val CanopyBottom = Light.CanopyBottom

/** The only colour for text on the canopy. White measures 4.86 on the top, 7.60 on the bottom. */
val OnCanopy = Color(0xFFFFFFFF)
/** Decoration on the canopy — dividers, wave shapes, ring tracks. Never text. */
val OnCanopyDecor = Light.OnCanopyDecor
/** Translucent chip fill. Legible with a white label only over the deep end of the canopy. */
val ChipOnCanopy = Light.ChipOnCanopy

// ─────────────────────────────────────────────────────────────────────────────
// Interactive violet on light/dark surfaces
// ─────────────────────────────────────────────────────────────────────────────
val Violet: Color @Composable get() = if (LocalDarkMode.current) Dark.Violet else Light.Violet
/** The clay lip beneath violet objects. */
val VioletDeep: Color @Composable get() = if (LocalDarkMode.current) Dark.VioletDeep else Light.VioletDeep
/** Selected rows, soft fills. */
val VioletTint: Color @Composable get() = if (LocalDarkMode.current) Dark.VioletTint else Light.VioletTint
/** Violet-tinted cast shadow, never neutral grey. */
val VioletShadow: Color @Composable get() = if (LocalDarkMode.current) Dark.VioletShadow else Light.VioletShadow

// ─────────────────────────────────────────────────────────────────────────────
// Ink
// ─────────────────────────────────────────────────────────────────────────────
val Ink: Color @Composable get() = if (LocalDarkMode.current) Dark.Ink else Light.Ink
val Muted: Color @Composable get() = if (LocalDarkMode.current) Dark.Muted else Light.Muted
val Faint: Color @Composable get() = if (LocalDarkMode.current) Dark.Faint else Light.Faint

/**
 * Fixed dark ink for text/icons painted on a reward or callout fill ([Gold], [Coral], the [Tier]
 * badges). Those fills keep one hex value in both themes, so the ink on top of them must too — it
 * cannot follow [Ink] into near-white in dark mode without breaking the exact contrast pairing this
 * token exists to guarantee. Never use this for ordinary text on [Surface]/[Ground]; use [Ink].
 */
val RewardInk = Color(0xFF1F1B3A)

// ─────────────────────────────────────────────────────────────────────────────
// Reward and callout fills. These carry [RewardInk]; never a foreground on light or dark.
// Fixed across themes on purpose — see the file doc comment.
// ─────────────────────────────────────────────────────────────────────────────
val Coral     = Color(0xFFFF8B5E)  // RewardInk on it measures 7.14
val CoralDeep = Color(0xFFE0651F)  // clay lip under coral
val Gold      = Color(0xFFFFB020)  // RewardInk on it measures 9.00
val GoldDeep  = Color(0xFFD98200)  // clay lip under gold

// ─────────────────────────────────────────────────────────────────────────────
// Feedback. Green and red are deep enough to carry white text in both themes.
// ─────────────────────────────────────────────────────────────────────────────
val Green     = Color(0xFF15803D)  // white 5.02
val GreenDeep = Color(0xFF0F5C2C)
val GreenTint: Color @Composable get() = if (LocalDarkMode.current) Dark.GreenTint else Light.GreenTint
val Red       = Color(0xFFDC2626)  // white 4.83
val RedDeep   = Color(0xFFA81B1B)
val RedTint: Color @Composable get() = if (LocalDarkMode.current) Dark.RedTint else Light.RedTint

/**
 * Caution — a third feedback level between [Green] and [Red].
 *
 * Green says done and red says wrong, and between them the palette had nothing for "this is
 * probably not what you meant, but it is your call". The duplicate-word notice on the contribution
 * form is exactly that: a contributor adding a word the dictionary already carries is usually
 * repeating an entry, but Kasiguranin has genuine homonyms — `baga` is lungs, swollen, and ember —
 * so the form must be able to caution without refusing. Painting that in [Red] would read as a
 * rejection of a submission the app actually wants.
 *
 * Unlike [Gold], which is a fixed reward fill carrying [RewardInk], this is ordinary themed ink:
 * a deep ochre on light, a warm sand on dark. Measured on [AmberTint] it is 5.31 (light) and 8.45
 * (dark); on [Surface], 5.93 and 10.6.
 */
val Amber: Color @Composable get() = if (LocalDarkMode.current) Dark.Amber else Light.Amber
/** Soft fill behind an [Amber] caution notice. */
val AmberTint: Color @Composable get() = if (LocalDarkMode.current) Dark.AmberTint else Light.AmberTint

// ─────────────────────────────────────────────────────────────────────────────
// Lesson-path node states
// ─────────────────────────────────────────────────────────────────────────────
val NodeLocked: Color @Composable get() = if (LocalDarkMode.current) Dark.NodeLocked else Light.NodeLocked
val NodeLockedInk: Color @Composable get() = if (LocalDarkMode.current) Dark.NodeLockedInk else Light.NodeLockedInk
val PathTrackIdle: Color @Composable get() = if (LocalDarkMode.current) Dark.PathTrackIdle else Light.PathTrackIdle

// ─────────────────────────────────────────────────────────────────────────────
// Badge tiers. Fixed across themes — same reasoning as Gold/Coral; carry RewardInk, not Ink.
// ─────────────────────────────────────────────────────────────────────────────
val TierGold   = Color(0xFFFFC24A)
val TierGoldDeep   = Color(0xFFD98200)
val TierSilver = Color(0xFFC3C9D8)
val TierSilverDeep = Color(0xFF8E96AA)
val TierBronze = Color(0xFFD08A55)
val TierBronzeDeep = Color(0xFF9C5F2E)

// ═════════════════════════════════════════════════════════════════════════════
// Compatibility layer.
// Screens not yet rebuilt still reference the old names. Each is remapped onto a Clay Canopy token
// so nothing renders off-system in the meantime, and each disappears as its screen is rewritten.
// Do not reference anything below this line in new code.
// ═════════════════════════════════════════════════════════════════════════════
val Primary: Color @Composable get() = Violet
val PrimaryDark: Color @Composable get() = VioletDeep
val PrimaryContainer: Color @Composable get() = VioletTint
val Secondary = Gold
val SecondaryDark = GoldDeep
val SecondaryContainer = Color(0xFFFFE8BF)
val Accent = Coral
val AccentContainer = Color(0xFFFFDCCB)

val LightThemeBackground: Color @Composable get() = Ground
val LightSurfaceCard: Color @Composable get() = Surface
val LightSurfaceVariant: Color @Composable get() = SurfaceSunken
val SandBg: Color @Composable get() = Ground

val StaticInk: Color @Composable get() = Ink
val StaticMuted: Color @Composable get() = Muted
val CoastInk: Color @Composable get() = Ink
val CoastMuted: Color @Composable get() = Muted
val CoastFaint: Color @Composable get() = Faint
val TextWhite  = Color(0xFFFFFFFF)
val TextDark: Color @Composable get() = Ink

val PlayPurpleStart: Color @Composable get() = CanopyTop
val PlayPurpleEnd   = Color(0xFF8C7CF0)
val PlayGoldStart   = Gold
val PlayGoldEnd     = GoldDeep
val PlayPinkStart   = Coral
val PlayPinkEnd     = CoralDeep
val PlayChipTranslucent: Color @Composable get() = ChipOnCanopy
val PlayNavDark: Color @Composable get() = CanopyBottom

val XpGold       = Gold
val XpGoldDark   = GoldDeep
val StreakEmber  = Coral
val StreakOrange = Coral
val VocabSea        = Color(0xFF0F9C91)
val VocabSeaDark    = Color(0xFF0A756E)
val GamesCoral      = Coral
val GamesCoralLight = Color(0xFFFFA47C)
val StoriesDusk: Color @Composable get() = CanopyTop
val SkyReview       = Color(0xFF2C7BE5)
val SkyReviewDark   = Color(0xFF1E5DB8)

val Success: Color @Composable get() = Green
val Warning = Color(0xFF9A6700)
val Error: Color @Composable get() = Red
val Info    = Color(0xFF2C7BE5)
val ErrorLight: Color @Composable get() = RedTint

val BadgeGold   = TierGold
val BadgeSilver = TierSilver
val BadgeBronze = TierBronze
val BadgeWood        = Color(0xFFA87954)
val BadgeEmerald: Color @Composable get() = Green
val BadgeCrownPurple: Color @Composable get() = Violet

val HeroCardStart: Color @Composable get() = CanopyTop
val HeroCardEnd: Color @Composable get() = CanopyBottom
val VocabCardStart = Gold
val VocabCardEnd   = GoldDeep
val StoriesCardStart: Color @Composable get() = CanopyTop
val StoriesCardEnd: Color @Composable get() = CanopyBottom
val MiniGamesCardStart = Coral
val MiniGamesCardEnd   = CoralDeep
val QuestsCardStart = Gold
val QuestsCardEnd   = GoldDeep
val NudgeBurple: Color @Composable get() = Violet
val NudgeBink   = Coral
val NudgeFlame  = Coral
val NudgeGold   = Gold
