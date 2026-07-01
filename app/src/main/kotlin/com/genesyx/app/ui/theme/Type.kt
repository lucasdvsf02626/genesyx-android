package com.genesyx.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Fonts: Outfit (display) + Inter (body) — per ARCHITECTURE.md / DESIGN_TOKENS.md.
//
// TO ACTIVATE: run `scripts/download_fonts.sh` from the repo root (needs curl).
// That drops the .ttf files into res/font/. Then swap the two lines below:
//   DisplayFamily = FontFamily.SansSerif  →  DisplayFamily = outfitFamily
//   BodyFamily    = FontFamily.SansSerif  →  BodyFamily    = interFamily
//
// import androidx.compose.ui.text.font.Font
// import com.genesyx.app.R
//
// private val outfitFamily = FontFamily(
//     Font(R.font.outfit_regular,  FontWeight.Normal),
//     Font(R.font.outfit_medium,   FontWeight.Medium),
//     Font(R.font.outfit_semibold, FontWeight.SemiBold),
//     Font(R.font.outfit_bold,     FontWeight.Bold),
// )
// private val interFamily = FontFamily(
//     Font(R.font.inter_regular,  FontWeight.Normal),
//     Font(R.font.inter_medium,   FontWeight.Medium),
//     Font(R.font.inter_semibold, FontWeight.SemiBold),
// )

val DisplayFamily: FontFamily = FontFamily.SansSerif
val BodyFamily: FontFamily = FontFamily.SansSerif

val GenesyxTypography = Typography(
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
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    ),
)
