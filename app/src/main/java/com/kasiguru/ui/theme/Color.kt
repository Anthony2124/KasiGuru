package com.kasiguru.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * KasiGuru "Jepjep's Forest" palette. See DESIGN.md for the full contract.
 *
 * Taken from Adrian's onboarding designs and the KasiGuru wordmark by sampling pixels, then checked
 * with the WCAG relative-luminance formula: a near-black night ground, dark cards, lime for the one
 * thing to do, cream from Jepjep's barong, and a soft green glow behind Jepjep for big moments.
 *
 * The app is dark only for now. A cream "Daylight" theme may follow; when it does, these tokens become
 * theme-reactive again rather than every call site learning about themes.
 *
 * Contrast figures quoted below are measured, not estimated.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Ground and surfaces
// ─────────────────────────────────────────────────────────────────────────────
/** App background, "night". White text 19.4, Faint 6.2. */
val Ground = Color(0xFF0A0E0D)
/** Cards, rows, options, inputs, the bottom bar. White text 18.4. */
val Surface = Color(0xFF141414)
/** Raised or recessed wells: search fields, inactive segments, pressed rows. White text 16.4. */
val SurfaceSunken = Color(0xFF1C211D)
/** Neutral progress track. Non-text. */
val TrackNeutral = Color(0xFF2C312D)
/** 1 dp borders on cards and options. Non-text. */
val BorderHairline = Color(0xFF3A3F3B)

// ─────────────────────────────────────────────────────────────────────────────
// The canopy: the deep forest-green panel at the top of Home, and hero fields.
// ─────────────────────────────────────────────────────────────────────────────
/** Canopy gradient start. White text 12.9. */
val CanopyTop = Color(0xFF0F3A10)
/** Canopy gradient end. White text 18.0. */
val CanopyBottom = Color(0xFF0A1A0C)

/** Text on the canopy. */
val OnCanopy = Color(0xFFFFFFFF)
/** Decoration on the canopy — dividers, ring tracks. Never text. */
val OnCanopyDecor = Color(0x3DFFFFFF)
/** Translucent chip fill on the canopy. White label on it stays above 9:1. */
val ChipOnCanopy = Color(0x38FFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// The glow: a soft radial light behind Jepjep on onboarding and reward screens.
// Stops: GlowCore at the centre, GlowMid halfway out, Ground one screen-width out. No rings.
// ─────────────────────────────────────────────────────────────────────────────
/** Centre of the glow. White 16.4, BrandLime 6.5. */
val GlowCore = Color(0xFF0A250B)
/** The glow half a screen-width out. */
val GlowMid = Color(0xFF0A190C)

// ─────────────────────────────────────────────────────────────────────────────
// Lime: "do this". Primary button faces, the active tab, links, focus, selected borders.
// ─────────────────────────────────────────────────────────────────────────────
/** The action colour. 8.3 on Ground, 7.9 on Surface. Text on a lime fill is always [OnLime]. */
val Lime = Color(0xFF71BD1D)
/** The clay lip beneath lime objects. White 6.2. */
val LimeLip = Color(0xFF3F6D0E)
/** Selected rows and soft fills. Lime text on it 6.2. */
val LimeTint = Color(0xFF1C2E12)
/** Label on a lime fill. 7.7 on Lime. White on Lime is 2.3 and never allowed. */
val OnLime = Color(0xFF0B1A05)
/** "Guru" in the wordmark, highlighted words in headings, progress fills. 7.7 on Ground, 6.5 on the glow. */
val BrandLime = Color(0xFF6CB619)
/** A selected option or tile, with a 2 dp lime border. White 7.1. */
val Olive = Color(0xFF446025)
/** The deep end of an olive gradient (category and game heroes). White 10.8. */
val OliveDeep = Color(0xFF2C4418)
/** Jepjep's barong: warm highlights, story paper, focus rings. On Ground 16.3. */
val Cream = Color(0xFFF2ECCA)
/** Cast shadow. Shadows barely read on night, so depth comes from the lip and the hairline instead. */
val ShadowTint = Color(0x66000000)

// ─────────────────────────────────────────────────────────────────────────────
// Ink
// ─────────────────────────────────────────────────────────────────────────────
/** Primary text. */
val Ink = Color(0xFFFFFFFF)
/** Secondary text. 11.8 on Surface, 10.4 on SurfaceSunken. */
val Muted = Color(0xFFC9D1C5)
/** Captions, hints, placeholders. 5.9 on Surface, 5.2 on SurfaceSunken. */
val Faint = Color(0xFF8C948A)

/**
 * Dark ink for text and icons painted on a bright fill: [Gold], [Coral], [Lime], [Red], the [Tier]
 * badges. 11.7 on Gold, 8.8 on Coral, 7.7 on Lime, 6.5 on Red. Never ordinary text on [Surface].
 */
val RewardInk = Color(0xFF0B1A05)

/** The full-screen dim behind the guided tour and dialogs, used at roughly 0.72 alpha. */
val Scrim = Color(0xFF000000)

// ─────────────────────────────────────────────────────────────────────────────
// Reward fills. Readable as foregrounds on night; as fills they carry [RewardInk].
// ─────────────────────────────────────────────────────────────────────────────
/** Streak orange. 9.5 on Ground; RewardInk on it 8.8. */
val Coral     = Color(0xFFFF9F1C)
val CoralDeep = Color(0xFFC46A00)
/** XP gold. 11.9 on Surface; RewardInk on it 11.7. */
val Gold      = Color(0xFFFFC83D)
val GoldDeep  = Color(0xFFC98A00)

// ─────────────────────────────────────────────────────────────────────────────
// Feedback. Both are bright enough to be text on night; as fills they carry [RewardInk].
// ─────────────────────────────────────────────────────────────────────────────
/** Correct / success. The lime, always with a check icon so colour is never the only signal. */
val Green     = Color(0xFF71BD1D)
val GreenDeep = Color(0xFF3F6D0E)
val GreenTint = Color(0xFF1C2E12)
/** Wrong / destructive. 6.6 on Surface. */
val Red       = Color(0xFFFF6B5B)
/** A deep red that carries white text (5.3), for destructive buttons. */
val RedDeep   = Color(0xFFC8352B)
val RedTint   = Color(0xFF3A1E1C)

/**
 * Caution — a third feedback level between [Green] and [Red], for "this is probably not what you
 * meant, but it is your call" (the duplicate-word notice on the contribution form). 11.7 on Surface,
 * 8.5 on [AmberTint].
 */
val Amber = Color(0xFFF5C86A)
/** Soft fill behind an [Amber] caution notice. */
val AmberTint = Color(0xFF3A2E12)

/** Audio, tips and links to help. 7.9 on Surface; RewardInk on it 7.7. */
val Info = Color(0xFF4FB3E8)

// ─────────────────────────────────────────────────────────────────────────────
// Lesson-path node states
// ─────────────────────────────────────────────────────────────────────────────
val NodeLocked = Color(0xFF1C211D)
val NodeLockedInk = Color(0xFF8C948A)
val PathTrackIdle = Color(0xFF2C312D)

// ─────────────────────────────────────────────────────────────────────────────
// Badge tiers. Carry RewardInk.
// ─────────────────────────────────────────────────────────────────────────────
val TierGold   = Color(0xFFFFC83D)
val TierGoldDeep   = Color(0xFFC98A00)
val TierSilver = Color(0xFFC3C9D8)
val TierSilverDeep = Color(0xFF8E96AA)
val TierBronze = Color(0xFFD08A55)
val TierBronzeDeep = Color(0xFF9C5F2E)

// ─────────────────────────────────────────────────────────────────────────────
// Named accents kept for their call sites.
// ─────────────────────────────────────────────────────────────────────────────
/** A state to attend to but not broken: guest progress at risk, a deck about to reset. Same as [Amber]. */
val Warning = Amber

/** The Review activity's accent on Home. Same as [Info]. */
val SkyReview = Info

/** The dictionary's accent in the word detail sheet. The one teal in the app. 5.4 on Surface. */
val VocabSea = Color(0xFF26B5A8)
