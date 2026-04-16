package com.mazzika.lyrics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens
import com.mazzika.lyrics.ui.theme.goldBrush

// ═════════════════════════════════════════════════════════════════
// SEARCH INPUT BAR
// ═════════════════════════════════════════════════════════════════

@Composable
fun SearchInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Chercher un titre, un artiste...",
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                else Modifier,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = tokens.textMid,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = tokens.textMid,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = tokens.text,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(tokens.gold),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// FILTER CHIP
// ═════════════════════════════════════════════════════════════════

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val onGold = if (tokens.isDark) Color(0xFF0A0800) else Color.White

    val background = if (selected)
        tokens.goldBrush()
    else
        Brush.linearGradient(
            listOf(
                if (tokens.isDark) tokens.surfaceHi else tokens.surface,
                if (tokens.isDark) tokens.surfaceHi else tokens.surface,
            ),
        )

    Box(
        modifier = modifier
            .shadow(
                if (selected) 8.dp else tokens.cardElevation,
                RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = if (selected) tokens.gold else Color.Black,
                spotColor = if (selected) tokens.gold else Color.Black,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .then(
                if (!selected && !tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(20.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.2.sp,
            color = if (selected) onGold else tokens.textMid,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// BUTTONS — Primary gold gradient & Danger outline
// ═════════════════════════════════════════════════════════════════

@Composable
fun PrimaryGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val tokens = LocalStudioTokens.current
    val onGold = if (tokens.isDark) Color(0xFF0A0A0A) else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false,
                ambientColor = tokens.gold,
                spotColor = tokens.gold,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) tokens.goldBrush()
                else Brush.linearGradient(listOf(tokens.gold.copy(alpha = 0.3f), tokens.gold.copy(alpha = 0.3f))),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingContent != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides onGold,
            ) {
                leadingContent()
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = onGold,
        )
    }
}

@Composable
fun DangerOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, tokens.danger, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingContent != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides tokens.danger,
            ) {
                leadingContent()
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = tokens.danger,
        )
    }
}
