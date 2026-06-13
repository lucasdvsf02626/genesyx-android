package com.genesyx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GenesyxLightColors = lightColorScheme(
    primary = ElectricLavender,
    onPrimary = LightCard,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = LightForeground,
    secondary = LightSecondary,
    onSecondary = LightForeground,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedForeground,
    outline = LightBorder,
    error = LightDestructive,
    onError = LightCard,
)

private val GenesyxDarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = DarkForeground,
    secondary = DarkSecondary,
    onSecondary = DarkForeground,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedForeground,
    outline = DarkBorder,
    error = DarkDestructive,
    onError = DarkForeground,
)

@Composable
fun GenesyxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) GenesyxDarkColors else GenesyxLightColors,
        typography = GenesyxTypography,
        content = content,
    )
}
