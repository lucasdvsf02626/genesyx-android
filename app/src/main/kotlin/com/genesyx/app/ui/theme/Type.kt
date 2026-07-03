package com.genesyx.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Brand fonts: Display = Outfit, Body = Inter (per the web styles.css @font-face).
//
// ACTIVE fallback: system sans-serif, so the scale/weights are already correct and
// the app compiles before the .ttf files are bundled.
//
// TO ACTIVATE THE REAL FONTS (2 steps):
//   1. Drop these 7 files into  app/src/main/res/font/  (exact lowercase names):
//        outfit_regular.ttf, outfit_medium.ttf, outfit_semibold.ttf, outfit_bold.ttf
//        inter_regular.ttf,  inter_medium.ttf,   inter_semibold.ttf
//      Grab them free (Open Font License) from fonts.google.com/specimen/Outfit
//      and fonts.google.com/specimen/Inter — rename the static weights as above.
//   2. Delete the two `= FontFamily.SansSerif` lines below and uncomment the two
//      `BundledFamily` blocks + the `val DisplayFamily = OutfitFamily` lines.
// ─────────────────────────────────────────────────────────────────────────────
val DisplayFamily = FontFamily.SansSerif
val BodyFamily = FontFamily.SansSerif

/*  ── Uncomment once the .ttf files are in res/font/ (and remove the two lines above) ──
val DisplayFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)
val BodyFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)
// Also add the import:  import androidx.compose.ui.text.font.Font
// and:                  import com.genesyx.app.R
*/

val GenesyxTypography = Typography(
    // Outfit (display)
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.025).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.025).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Inter (body)
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.005).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.005).sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    // ALL-CAPS eyebrow / section label
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    ),
)
