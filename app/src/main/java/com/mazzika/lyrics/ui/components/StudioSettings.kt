package com.mazzika.lyrics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

// ═════════════════════════════════════════════════════════════════
// SETTINGS CARD — rounded card wrapping one or more SettingsRows
// ═════════════════════════════════════════════════════════════════

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                else Modifier,
            ),
    ) {
        content()
    }
}

@Composable
fun SettingsRow(
    title: String,
    description: String? = null,
    isLast: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val tokens = LocalStudioTokens.current
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = mod
            .fillMaxWidth()
            .drawBehind {
                if (!isLast) {
                    drawLine(
                        color = tokens.border,
                        start = Offset(16.dp.toPx(), size.height),
                        end = Offset(size.width - 16.dp.toPx(), size.height),
                        strokeWidth = 1f,
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = tokens.text,
            )
            if (description != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = tokens.textMid,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsValueText(text: String, isGold: Boolean = true) {
    val tokens = LocalStudioTokens.current
    Text(
        text = text,
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = if (isGold) tokens.gold else tokens.textMid,
    )
}

// ═════════════════════════════════════════════════════════════════
// THEME PICKER — segmented 3-option control
// ═════════════════════════════════════════════════════════════════

enum class ThemeChoice { LIGHT, DARK, SYSTEM }

@Composable
fun ThemePicker(
    selected: ThemeChoice,
    onSelect: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tokens.pillBg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ThemeOption("☀", selected == ThemeChoice.LIGHT) { onSelect(ThemeChoice.LIGHT) }
        ThemeOption("🌙", selected == ThemeChoice.DARK) { onSelect(ThemeChoice.DARK) }
        ThemeOption("A", selected == ThemeChoice.SYSTEM) { onSelect(ThemeChoice.SYSTEM) }
    }
}

@Composable
private fun ThemeOption(symbol: String, active: Boolean, onClick: () -> Unit) {
    val tokens = LocalStudioTokens.current
    val onGold = if (tokens.isDark) Color(0xFF0A0800) else Color.White
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) Brush.linearGradient(listOf(tokens.goldLight, tokens.gold))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = if (active) onGold else tokens.textMid,
        )
    }
}
