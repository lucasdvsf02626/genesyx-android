package com.genesyx.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette — values from src/styles.css (oklch), converted to ARGB.
// See docs/DESIGN_TOKENS.md. Reused unchanged in dark mode.
val ElectricLavender = Color(0xFF4D4DAA) // primary
val PrimaryLight = Color(0xFF8B7FE8)
val PrimaryContainer = Color(0xFFC8C0F5)
val PowderBlue = Color(0xFF8DD2E2) // fertile-window tint
val PowderPink = Color(0xFFDDA4D3) // period tint
val ElectricBlue = Color(0xFF57A1CE) // hydration / pH alkaline
val BabyLavender = Color(0xFF8888D3) // luteal tint / avatar gradient start
val ElectricPink = Color(0xFFC782D8) // avatar gradient end / pregnancy accent
val BabyPink = Color(0xFFDEBED2)

// pH status colors (use-ph.ts)
val PhAcidic = Color(0xFFD85A8A)
val PhOptimal = Color(0xFF3FA37A)
val PhAlkaline = Color(0xFF4D4DAA)

// Nutrition focus-food accents (per phase, hardcoded in web)
val FoodPeriod = Color(0xFFF48FB1)
val FoodFollicular = Color(0xFFA5D6A7)
val FoodOvulatory = Color(0xFFCE93D8)
val FoodLuteal = Color(0xFFB39DDB)

// ── Light semantic tokens
val LightBackground = Color(0xFFF2F2F2) // zenith
val LightCard = Color(0xFFFFFFFF)
val LightForeground = Color(0xFF1F1F1F)
val LightMutedForeground = Color(0xFF6B6878)
val LightMuted = Color(0xFFEEEBF1)
val LightSecondary = Color(0xFFF2EFF6)
val LightBorder = Color(0xFFE6E4EC)
val LightDestructive = Color(0xFFD93636)

// ── Dark semantic tokens (.dark)
val DarkBackground = Color(0xFF000000)
val DarkCard = Color(0xFF1F1F1F)
val DarkForeground = Color(0xFFFFFFFF)
val DarkMutedForeground = Color(0xFFB8B5C4)
val DarkMuted = Color(0xFF2A2730)
val DarkSecondary = Color(0xFF2A2730)
val DarkBorder = Color(0x1AFFFFFF) // white @10%
val DarkPrimary = Color(0xFF9B7BD8) // brighter lavender for dark
val DarkDestructive = Color(0xFFE0463A)
