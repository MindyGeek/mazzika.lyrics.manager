package com.mazzika.lyrics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens
import com.mazzika.lyrics.ui.theme.goldBrush

// ═════════════════════════════════════════════════════════════════
// QUICK TILE — 2×2 actions on Home
// ═════════════════════════════════════════════════════════════════

data class QuickAction(
    val emoji: String,
    val labelLine1: String,
    val labelLine2: String,
    val gradient: Brush,
)

@Composable
fun QuickTile(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(12.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(action.gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = action.emoji, fontSize = 22.sp, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = action.labelLine1,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = tokens.text,
                maxLines = 1,
            )
            Text(
                text = action.labelLine2,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = tokens.text,
                maxLines = 1,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// FEATURE CARD — Purple gradient live-session card on Home
// ═════════════════════════════════════════════════════════════════

@Composable
fun FeatureCard(
    eyebrow: String,
    title: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(tokens.featureGradA, tokens.featureGradB)))
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        // Glow circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to tokens.featureGlow,
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width + 30f, -40f),
                        radius = 150f,
                    )
                },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(tokens.success),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = eyebrow,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = tokens.success,
                        letterSpacing = 1.1.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp,
                    color = tokens.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = meta,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = tokens.textMid,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(12.dp, RoundedCornerShape(50), clip = false)
                    .clip(RoundedCornerShape(50))
                    .background(tokens.gold),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "▶", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// DOC ROW — List row for a document (Home recents / Catalog / Folder detail)
// ═════════════════════════════════════════════════════════════════

@Composable
fun DocRow(
    title: String,
    letter: String,
    palette: CoverPalette,
    meta: String,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current

    val rowModifier = if (tokens.isDark) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) tokens.surfaceHi else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.surface)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    }

    Row(
        modifier = modifier.then(rowModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocCover(letter = letter, palette = palette, size = 48, corner = 8, fontSize = 18)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = tokens.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(text = "PDF")
                Spacer(Modifier.width(6.dp))
                Text(
                    text = meta,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = tokens.textMid,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onMoreClick != null) {
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 28.dp)
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⋯",
                    color = tokens.textDim,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// FOLDER CARD — 1:1 square tile in 3-col grid + dashed "Nouveau" variant
// ═════════════════════════════════════════════════════════════════

@Composable
fun FolderCard(
    emoji: String,
    name: String,
    count: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(tokens.cardElevation, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = tokens.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (count != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = count,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = tokens.textDim,
            )
        }
    }
}

@Composable
fun FolderNewCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val borderColor = if (tokens.isDark) tokens.gold else tokens.gold.copy(alpha = 0.4f)

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "+",
            fontFamily = Inter,
            fontWeight = FontWeight.Light,
            fontSize = 28.sp,
            color = tokens.gold.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Nouveau",
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = tokens.gold.copy(alpha = 0.8f),
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// SUBFOLDER CHIP — Horizontal row chip in folder detail
// ═════════════════════════════════════════════════════════════════

@Composable
fun SubfolderChip(
    emoji: String,
    name: String,
    count: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface
    Column(
        modifier = modifier
            .widthIn(min = 110.dp)
            .shadow(tokens.cardElevation, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = tokens.text,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = count,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = tokens.textDim,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// SECTION HEADER — title + optional count pill + optional link
// ═════════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    link: String? = null,
    onLinkClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.36).sp,
                color = tokens.text,
            )
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                CountPill(count = count)
            }
        }
        if (link != null && onLinkClick != null) {
            Text(
                text = link,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
                color = tokens.textMid,
                modifier = Modifier.clickable(onClick = onLinkClick),
            )
        }
    }
}

@Composable
fun GroupLabel(
    text: String,
    modifier: Modifier = Modifier,
    withDot: Boolean = true,
) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withDot) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(tokens.gold),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.65.sp,
            color = tokens.textDim,
        )
    }
}
