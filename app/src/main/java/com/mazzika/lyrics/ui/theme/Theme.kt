package com.mazzika.lyrics.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MazzikaDarkColorScheme = darkColorScheme(
    primary = Gold, onPrimary = DarkBackground,
    primaryContainer = GoldDark, onPrimaryContainer = GoldLight,
    secondary = GoldLight, onSecondary = DarkBackground,
    background = DarkBackground, onBackground = DarkTextPrimary,
    surface = DarkSurface, onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated, onSurfaceVariant = DarkTextSecondary,
    outline = GoldDeep, outlineVariant = DarkTextMuted,
    error = Error,
)

private val MazzikaLightColorScheme = lightColorScheme(
    primary = Gold, onPrimary = LightBackground,
    primaryContainer = GoldLight, onPrimaryContainer = GoldDeep,
    secondary = GoldDark, onSecondary = LightBackground,
    background = LightBackground, onBackground = LightTextPrimary,
    surface = LightSurface, onSurface = LightTextPrimary,
    surfaceVariant = LightBackground, onSurfaceVariant = LightTextSecondary,
    outline = GoldDark, outlineVariant = LightTextMuted,
    error = Error,
)

@Composable
fun MazzikaLyricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MazzikaDarkColorScheme else MazzikaLightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
