package com.kasiguru.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * KasiGuru Consolidated 3-Gradient Palette System.
 * Strictly 3 signature soft saturated gradient pairs across all cards and UI components:
 * 1. Purple: #7B6EF6 -> #A78BFA (Identity, Headers, Hero Cards)
 * 2. Gold: #FFC94A -> #FF9F1C (XP, Level, Streaks, Rewards)
 * 3. Pink: #FF9FC0 -> #FF6FA0 (Mini-Games, Badges, Features)
 */

// ─── Primary: Deep Ocean Teal (Legacy Defaults) ───
val PrimaryDark = Color(0xFF0A4D4D)
val Primary = Color(0xFF0D7377)
val PrimaryLight = Color(0xFF14A3A8)
val PrimaryContainer = Color(0xFF1ECECE)

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
val TextHeadingBlack = Color(0xFF12161F)
val TextSubtleGray = Color(0xFF6B7280)

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
val XpGold = Color(0xFFFFD700)
val XpGoldDark = Color(0xFFFF9F1C)
val StreakOrange = Color(0xFFFF9F1C)
val LevelBlue = Color(0xFF7B6EF6)
