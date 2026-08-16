package com.kasiguru.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * KasiGuru "Casiguran Coast" palette — Phase 0 token foundation.
 * Identity: Purple. Warmth: Gold. Coastal accents: Sea / Coral / Ember / Dusk / Sky.
 * Gradient pairs: Purple (identity) · Gold (XP) · Pink (badges/mini-games).
 */

// ─── Primary: Purple Identity (remapped from legacy teal) ───
val PrimaryDark = Color(0xFF4A3FC0)
val Primary = Color(0xFF7B6EF6)
val PrimaryLight = Color(0xFFA78BFA)
val PrimaryContainer = Color(0xFFE8E5FF)

// ─── Secondary: Sunset Gold ───
val SecondaryDark = Color(0xFFB8860B)
val Secondary = Color(0xFFF0A500)
val SecondaryLight = Color(0xFFFFCA28)
val SecondaryContainer = Color(0xFFFFE082)

// ─── Accent: Coral Sunset ───
val Accent = Color(0xFFFF6B6B)
val AccentLight = Color(0xFFFF8A8A)
val AccentContainer = Color(0xFFFFD0D0)

// ─── Scaffold & Surface ───
val LightThemeBackground = Color(0xFFF6F7FB)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFF2F8)

val StaticTextHeadingBlack = Color(0xFF12161F)
val StaticTextSubtleGray = Color(0xFF6B7280)

val TextHeadingBlack: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val TextSubtleGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

// ─── STRICT 3 SIGNATURE PLAY GRADIENT PAIRS ───
val PlayPurpleStart = Color(0xFF7B6EF6)
val PlayPurpleEnd   = Color(0xFFA78BFA)

val PlayGoldStart   = Color(0xFFFFC94A)
val PlayGoldEnd     = Color(0xFFFF9F1C)

val PlayPinkStart   = Color(0xFFFF9FC0)
val PlayPinkEnd     = Color(0xFFFF6FA0)

val PlayCoinYellow    = Color(0xFFFFD93D)
val PlayCardOnLight   = Color(0xFFFFFFFF)
val PlayChipTranslucent = Color(0x66FFFFFF) // white @ 40%
val PlayNavDark       = Color(0xFF15132A)   // bottom-bar background
val PlayNavActive     = Color(0xFFFFC94A)   // active tab underline/glow

// Legacy compatibility fallbacks mapped directly to the 3 Play Gradients
val HeroCardStart = PlayPurpleStart
val HeroCardEnd = PlayPurpleEnd

val VocabCardStart = PlayGoldStart
val VocabCardEnd = PlayGoldEnd

val StoriesCardStart = PlayPurpleStart
val StoriesCardEnd = PlayPurpleEnd

val MiniGamesCardStart = PlayPinkStart
val MiniGamesCardEnd = PlayPinkEnd

val QuestsCardStart = PlayGoldStart
val QuestsCardEnd = PlayGoldEnd

// Floating Bottom Bar
val FloatingNavBackground = Color(0xFF15132A)
val FloatingNavActive = Color(0xFFFFC94A)

// Background & Surface (Dark Theme)
val DarkBackground = Color(0xFF0A1628)
val DarkSurface = Color(0xFF132039)
val DarkSurfaceVariant = Color(0xFF1A2A4A)
val DarkSurfaceElevated = Color(0xFF213358)
val DarkCard = Color(0xFF1E2D4D)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1A1A2E)
val TextGray = Color(0xFF9E9E9E)
val TextDarkGray = Color(0xFF616161)
val TextLightGray = Color(0xFFE0E0E0)

// Status Colors
val Success = Color(0xFF2ECC71)
val Warning = Color(0xFFF1C40F)
val Error = Color(0xFFE74C3C)
val Info = Color(0xFF3498DB)
val ErrorLight = Color(0xFFFFEBEE)

// Gamification Medal Colors
val BadgeGold = Color(0xFFFFD700)
val BadgeSilver = Color(0xFFC0C0C0)
val BadgeBronze = Color(0xFFCD7F32)
val XpGold = Color(0xFFFFB020)
val XpGoldDark = Color(0xFFFF9E1B)
val StreakOrange = Color(0xFFFF7A3C)
val LevelBlue = Color(0xFF7B6EF6)

// ─── Casiguran Coast semantic tokens ───
val VocabSea      = Color(0xFF12B3A6)   // vocabulary / coastal teal
val VocabSeaDark  = Color(0xFF0E9E97)
val GamesCoral    = Color(0xFFFF6B6B)   // mini-games
val GamesCoralLight = Color(0xFFFF8A5B)
val StoriesDusk   = Color(0xFF6C5CE7)   // stories / folklore purple
val SkyReview     = Color(0xFF3FA9F5)   // review / spaced repetition
val StreakEmber    = Color(0xFFFF7A3C)   // streak fire
val SandBg        = Color(0xFFFAF5EC)   // warm sand scaffold
val CoastInk      = Color(0xFF1C2233)   // primary text
val CoastMuted    = Color(0xFF7A8195)   // secondary text

// ─── Nudge Branding & Moodboard 1-5 Tokens ───
val NudgeBurple = Color(0xFF8636CE)
val NudgeBink = Color(0xFFFF828D)
val NudgeFlame = Color(0xFFF78864)
val NudgeMint = Color(0xFFADD5D5)
val NudgeGold = Color(0xFFFFD15C)
val CharcoalNav = Color(0xFF15132A)

// Badge Tier Metallic Tokens (Moodboards 1 & 3)
val BadgeWood = Color(0xFFA87954)
val BadgeEmerald = Color(0xFF2ECC71)
val BadgeCrownPurple = Color(0xFF9B59B6)

