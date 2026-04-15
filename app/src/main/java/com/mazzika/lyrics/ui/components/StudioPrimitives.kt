package com.mazzika.lyrics.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.CoverBlueA
import com.mazzika.lyrics.ui.theme.CoverBlueB
import com.mazzika.lyrics.ui.theme.CoverGoldA
import com.mazzika.lyrics.ui.theme.CoverGoldB
import com.mazzika.lyrics.ui.theme.CoverGreenA
import com.mazzika.lyrics.ui.theme.CoverGreenB
import com.mazzika.lyrics.ui.theme.CoverOrangeA
import com.mazzika.lyrics.ui.theme.CoverOrangeB
import com.mazzika.lyrics.ui.theme.CoverPinkA
import com.mazzika.lyrics.ui.theme.CoverPinkB
import com.mazzika.lyrics.ui.theme.CoverPurpleA
import com.mazzika.lyrics.ui.theme.CoverPurpleB
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens
import com.mazzika.lyrics.ui.theme.goldBrush

// ═════════════════════════════════════════════════════════════════
// COVER GRADIENTS — 6 cycling colors matching the mockups (c1..c6)
// ═════════════════════════════════════════════════════════════════

data class CoverPalette(val a: Color, val b: Color)

val CoverPalettes = listOf(
    CoverPalette(CoverOrangeA, CoverOrangeB),   // c1
    CoverPalette(CoverGreenA, CoverGreenB),     // c2
    CoverPalette(CoverPurpleA, CoverPurpleB),   // c3
    CoverPalette(CoverPinkA, CoverPinkB),       // c4
    CoverPalette(CoverBlueA, CoverBlueB),       // c5
    CoverPalette(CoverGoldA, CoverGoldB),       // c6
)

fun paletteFor(seed: Any): CoverPalette {
    val h = seed.hashCode()
    val idx = ((h % CoverPalettes.size) + CoverPalettes.size) % CoverPalettes.size
    return CoverPalettes[idx]
}

fun CoverPalette.brush() = Brush.linearGradient(listOf(a, b))

/**
 * A rounded-corner colored cover with a centered monogram letter.
 * Used for doc rows, found sessions, and active-session covers.
 */
@Composable
fun DocCover(
    letter: String,
    palette: CoverPalette,
    size: Int = 48,
    corner: Int = 8,
    fontSize: Int = 18,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(palette.brush()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            color = Color.White,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// PILL — small rounded rectangle label (e.g. "PDF", count badge)
// ═════════════════════════════════════════════════════════════════

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(tokens.pillBg)
            .then(
                if (!tokens.isDark) Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(3.dp)) else Modifier,
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = tokens.textMid,
        )
    }
}

@Composable
fun CountPill(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tokens.pillBg)
            .then(
                if (!tokens.isDark) Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(10.dp)) else Modifier,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = count.toString(),
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = tokens.textMid,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// FAB — rounded square, gold gradient, shadow
// ═════════════════════════════════════════════════════════════════

@Composable
fun StudioFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    val onGold = if (tokens.isDark) Color(0xFF0A0A0A) else Color.White
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = tokens.gold,
                spotColor = tokens.gold,
            )
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.goldBrush())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides onGold,
        ) {
            content()
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// TOGGLE — 46x26 pill, gold gradient when ON, knob 18dp
// ═════════════════════════════════════════════════════════════════

@Composable
fun StudioToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val offsetX by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = tween(180),
        label = "toggleKnob",
    )

    Box(
        modifier = modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) tokens.goldBrush() else Brush.linearGradient(listOf(tokens.pillBg, tokens.pillBg)))
            .then(
                if (!checked && !tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(13.dp))
                else Modifier,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 3.dp)
                .offset(x = offsetX.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else tokens.textDim),
        )
    }
}
