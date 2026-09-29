package com.kasiguru.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kasiguru.R

/**
 * Display voice: Fredoka, the face of Adrian's onboarding designs. Rounded and friendly, it carries
 * every heading, every button and every number that celebrates (XP, streak, score, rank).
 *
 * Bundled in res/font (static 600 and 700 instances, about 50 KB each) so it renders on the very first
 * launch, offline, instead of falling back to the system face while a downloadable font loads.
 *
 * Fredoka stops at 700. The heavier weights call sites already ask for (ExtraBold, Black) resolve to
 * the 700 file explicitly rather than leaving the matcher to synthesise a fake bold.
 */
val KasiGuruDisplay = FontFamily(
    Font(R.font.fredoka_semibold, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
    Font(R.font.fredoka_bold, FontWeight.ExtraBold),
    Font(R.font.fredoka_bold, FontWeight.Black)
)

/**
 * Body voice: DM Sans. Compact and very legible at 12–14 sp, so long dictionary entries, story text
 * and long Kasiguranin words wrap later than they would in a wider geometric face.
 */
val KasiGuruBody = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
    Font(R.font.dm_sans_bold, FontWeight.ExtraBold)
)

/** Kept so any straggling reference still resolves; new code picks a family explicitly. */
val KasiGuruFontFamily = KasiGuruBody

/**
 * Sizes are in sp so the system font-size setting is honoured. Fredoka is wide and round, so it is
 * tracked less tightly than the old display face.
 *
 * Display / headline / title are Fredoka. Body / label are DM Sans: the moment a screen switches from
 * "telling you how you did" to "giving you something to read", the voice changes with it.
 */
val KasiGuruTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    ),

    titleLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    ),

    // Buttons use labelLarge, so it is Fredoka: the onboarding sets every button in it.
    labelLarge = TextStyle(
        fontFamily = KasiGuruDisplay, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = KasiGuruBody, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
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
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.5).sp
)
