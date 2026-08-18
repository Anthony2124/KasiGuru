package com.kasiguru.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.kasiguru.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val nunito = GoogleFont("Nunito")
private val dmSans = GoogleFont("DM Sans")

/**
 * Display voice. Nunito at 800/900 — rounded terminals are what make the type belong to the clay
 * world; a geometric-square face would fight it. Carries every heading and every number that
 * celebrates: XP, streak, score, rank.
 */
val KasiGuruDisplay = FontFamily(
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.Black)
)

/**
 * Body voice. DM Sans — clean and geometric, so long dictionary entries and story text stay
 * comfortable at length where a rounded display face would tire the eye.
 */
val KasiGuruBody = FontFamily(
    Font(googleFont = dmSans, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = dmSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = dmSans, fontProvider = provider, weight = FontWeight.Bold)
)

/** Kept so any straggling reference still resolves; new code picks a family explicitly. */
val KasiGuruFontFamily = KasiGuruBody

/**
 * Sizes are in sp so the system font-size setting is honoured. Tracking tightens as size grows,
 * which is the optical correction that makes large type look evenly set.
 *
 * Display / headline / title are Nunito. Body / label are DM Sans. The split is deliberate: the
 * moment a screen switches from "telling you how you did" to "giving you something to read", the
 * voice changes with it.
 */
val KasiGuruTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Black,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Black,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.9).sp
    ),
    displaySmall = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.7).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp
    ),

    titleLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp
    ),

    bodyLarge = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp
    ),

    labelLarge = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp
    )
)

/**
 * The Kasiguranin headword style. The language is the product, so on any screen that shows a word
 * it is the loudest thing present. Kept as a named style rather than inline sizes so a flashcard,
 * a lesson prompt and a dictionary detail all agree.
 */
val KasiguraninHeadword = TextStyle(
    fontFamily = KasiGuruDisplay,
    fontWeight = FontWeight.Black,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = (-1.0).sp
)
