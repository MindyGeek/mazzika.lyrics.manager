package com.mazzika.lyrics.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

// ═════════════════════════════════════════════════════════════════
// LIVE BANNER — Green gradient "Diffusion en cours" with pulse dot + timer
// ═════════════════════════════════════════════════════════════════

@Composable
fun LiveBanner(
    label: String = "Diffusion en cours",
    timer: String? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val greenStart = tokens.success.copy(alpha = if (tokens.isDark) 0.15f else 0.08f)
    val greenEnd = tokens.success.copy(alpha = if (tokens.isDark) 0.05f else 0.02f)
    val borderColor = tokens.success.copy(alpha = if (tokens.isDark) 0.3f else 0.25f)

    // Pulse animation for the dot
    val infinite = rememberInfiniteTransition(label = "livePulse")
    val alpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(greenStart, greenEnd)))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .graphicsLayer { this.alpha = alpha }
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tokens.success)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = tokens.success,
                        spotColor = tokens.success,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label.uppercase(),
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.1.sp,
                color = tokens.success,
            )
        }
        if (!timer.isNullOrBlank()) {
            Text(
                text = timer,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = tokens.textMid,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// SESSION CHOICE CARD — Create vs Join
// ═════════════════════════════════════════════════════════════════

@Composable
fun SessionChoiceCard(
    emoji: String,
    title: String,
    description: String,
    iconGradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconGradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 24.sp, color = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.32).sp,
                color = tokens.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = tokens.textMid,
                lineHeight = 17.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "›", fontSize = 22.sp, color = tokens.textDim, fontWeight = FontWeight.Medium)
    }
}

// ═════════════════════════════════════════════════════════════════
// DEVICE CHIP — row inside ActiveSessionCard
// ═════════════════════════════════════════════════════════════════

@Composable
fun DeviceChip(
    initial: String,
    name: String,
    status: String,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bottomBorder = if (!isLast) tokens.border else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (bottomBorder.alpha > 0f) {
                    drawLine(
                        color = bottomBorder,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tokens.pillBg)
                .then(
                    if (!tokens.isDark) Modifier.border(1.dp, tokens.cardBorder, CircleShape) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = tokens.text,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = tokens.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = tokens.success,
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tokens.success),
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// ACTIVE SESSION CARD — pilot view
// ═════════════════════════════════════════════════════════════════

@Composable
fun ActiveSessionCard(
    fileTitle: String,
    subtitle: String,
    coverLetter: String,
    coverGradient: Brush,
    devicesCount: Int,
    devices: List<Triple<String, String, String>>, // initial, name, status
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(tokens.cardElevation, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(
                if (!tokens.isDark)
                    Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp))
                else Modifier,
            )
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(coverGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = coverLetter,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileTitle,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = tokens.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = tokens.textMid,
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        Text(
            text = "$devicesCount appareils connectés".uppercase(),
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = tokens.textDim,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        devices.forEachIndexed { i, (initial, name, status) ->
            DeviceChip(
                initial = initial,
                name = name,
                status = status,
                isLast = i == devices.lastIndex,
            )
        }

        Spacer(Modifier.height(16.dp))

        content()
    }
}

// ═════════════════════════════════════════════════════════════════
// PULSE SEARCH — Concentric rings + center icon
// ═════════════════════════════════════════════════════════════════

@Composable
fun PulseSearch(
    modifier: Modifier = Modifier,
    centerEmoji: String = "🔍",
) {
    val tokens = LocalStudioTokens.current

    val infinite = rememberInfiniteTransition(label = "pulseRings")
    val scale1 by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "s1",
    )
    val scale2 by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "s2",
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { scaleX = scale1; scaleY = scale1 }
                .border(2.dp, tokens.gold.copy(alpha = 0.3f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { scaleX = scale2; scaleY = scale2 }
                .border(2.dp, tokens.gold.copy(alpha = 0.5f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer { scaleX = scale1; scaleY = scale1 }
                .border(2.dp, tokens.gold.copy(alpha = 0.7f), CircleShape),
        )
        // Center
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(tokens.goldLight, tokens.gold)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = centerEmoji, fontSize = 20.sp)
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// FOUND SESSION CARD
// ═════════════════════════════════════════════════════════════════

@Composable
fun FoundSessionCard(
    title: String,
    host: String,
    coverLetter: String,
    coverGradient: Brush,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val bg = if (tokens.isDark) tokens.surfaceHi else tokens.surface
    val onGold = if (tokens.isDark) Color(0xFF0A0A0A) else Color.White

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
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(coverGradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = coverLetter,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = tokens.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = host,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = tokens.textMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(tokens.goldLight, tokens.gold)))
                .shadow(6.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = tokens.gold, spotColor = tokens.gold)
                .clickable(onClick = onJoin)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Rejoindre",
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = onGold,
            )
        }
    }
}
