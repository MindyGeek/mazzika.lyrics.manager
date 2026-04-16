package com.mazzika.lyrics.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═════════════════════════════════════════════════
// STUDIO TOKENS — non-Material3 design tokens
// (header bg, nav bg, gold-light, gradients, etc.)
// ═════════════════════════════════════════════════
data class StudioTokens(
    val isDark: Boolean,
    val bg: Color,
    val bgAlt: Color,
    val surface: Color,
    val surfaceHi: Color,
    val border: Color,
    val text: Color,
    val textMid: Color,
    val textDim: Color,
    val headerBg: Color,
    val headerBorder: Color,
    val navBg: Color,
    val navBorder: Color,
    val cardBorder: Color,
    val pillBg: Color,
    val gold: Color,
    val goldLight: Color,
    val success: Color,
    val danger: Color,
    val accentBlue: Color,
    val accentPurple: Color,
    val featureGradA: Color,
    val featureGradB: Color,
    val featureGlow: Color,
    val cardElevation: Dp,
)

val StudioTokensDark = StudioTokens(
    isDark = true,
    bg = DarkBg,
    bgAlt = DarkBgAlt,
    surface = DarkSurface,
    surfaceHi = DarkSurfaceHi,
    border = DarkBorder,
    text = DarkText,
    textMid = DarkTextMid,
    textDim = DarkTextDim,
    headerBg = DarkHeaderBg,
    headerBorder = DarkHeaderBorder,
    navBg = DarkNavBg,
    navBorder = DarkNavBorder,
    cardBorder = Color.Transparent,
    pillBg = DarkSurfaceHi,
    gold = GoldDark,
    goldLight = GoldLightDark,
    success = Success,
    danger = Danger,
    accentBlue = AccentBlue,
    accentPurple = AccentPurple,
    featureGradA = FeatureDarkA,
    featureGradB = FeatureDarkB,
    featureGlow = FeatureGlowDark,
    cardElevation = 0.dp,
)

val StudioTokensLight = StudioTokens(
    isDark = false,
    bg = LightBg,
    bgAlt = LightBgAlt,
    surface = LightSurface,
    surfaceHi = LightSurfaceHi,
    border = LightBorder,
    text = LightText,
    textMid = LightTextMid,
    textDim = LightTextDim,
    headerBg = LightHeaderBg,
    headerBorder = LightHeaderBorder,
    navBg = LightNavBg,
    navBorder = LightNavBorder,
    cardBorder = LightCardBorder,
    pillBg = LightPillBg,
    gold = GoldLight,
    goldLight = GoldLightLight,
    success = SuccessLight,
    danger = DangerLight,
    accentBlue = AccentBlueLight,
    accentPurple = AccentPurpleLight,
    featureGradA = FeatureLightA,
    featureGradB = FeatureLightB,
    featureGlow = FeatureGlowLight,
    cardElevation = 2.dp,
)

val LocalStudioTokens = staticCompositionLocalOf { StudioTokensDark }

// Gold gradient brush used for nav indicator, FAB, buttons
fun StudioTokens.goldBrush(): Brush = Brush.linearGradient(listOf(goldLight, gold))

// ═════════════════════════════════════════════════
// MATERIAL3 COLOR SCHEMES (derived from StudioTokens)
// ═════════════════════════════════════════════════
private fun materialDark(t: StudioTokens) = darkColorScheme(
    primary = t.gold, onPrimary = Color(0xFF0A0800),
    primaryContainer = t.gold, onPrimaryContainer = Color.White,
    secondary = t.goldLight, onSecondary = Color.Black,
    background = t.bg, onBackground = t.text,
    surface = t.surface, onSurface = t.text,
    surfaceVariant = t.surfaceHi, onSurfaceVariant = t.textMid,
    outline = t.border, outlineVariant = t.textDim,
    error = t.danger, onError = Color.White,
)

private fun materialLight(t: StudioTokens) = lightColorScheme(
    primary = t.gold, onPrimary = Color.White,
    primaryContainer = t.goldLight, onPrimaryContainer = Color.White,
    secondary = t.goldLight, onSecondary = Color.White,
    background = t.bg, onBackground = t.text,
    surface = t.surface, onSurface = t.text,
    surfaceVariant = t.bgAlt, onSurfaceVariant = t.textMid,
    outline = t.border, outlineVariant = t.textDim,
    error = t.danger, onError = Color.White,
)

@Composable
fun MazzikaLyricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) StudioTokensDark else StudioTokensLight
    val colorScheme = if (darkTheme) materialDark(tokens) else materialLight(tokens)
    CompositionLocalProvider(LocalStudioTokens provides tokens) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
