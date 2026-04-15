package com.mazzika.lyrics.ui.navigation

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

/**
 * Studio Moderne header.
 *
 * Two variants:
 *  - Brand variant (title = "Mazzika"): gold "• Mazzika" branded word + settings button
 *  - Title variant (anything else): optional back button + title/subtitle column + settings
 *
 * Visuals:
 *  - background `--header-bg`
 *  - 1dp gold gradient line at the bottom
 *  - subtle shadow
 *  - all buttons are round 40dp with `--surface-hi` bg (dark) or white bg+border (light)
 */
@Composable
fun MazzikaTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsButton: Boolean = true,
    folderName: String? = null,
    folderIcon: String? = null,
    subtitle: String? = null,
) {
    val tokens = LocalStudioTokens.current
    val isBrand = folderName == null && !showBackButton && title == "Mazzika"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.headerBg)
            .drawBehind {
                // Gold gradient line at the bottom edge, insetted 24dp
                val inset = 24.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to tokens.gold.copy(alpha = 0.5f),
                        1f to Color.Transparent,
                    ),
                    start = Offset(inset, size.height - 0.5f),
                    end = Offset(size.width - inset, size.height - 0.5f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(bottom = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // LEFT: back button (optional) + title column OR brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (showBackButton) {
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = "Retour",
                        onClick = onBackClick,
                        size = 36,
                    )
                    Spacer(Modifier.width(12.dp))
                }

                if (isBrand) {
                    BrandText(gold = tokens.gold, textColor = tokens.text)
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (folderIcon != null) {
                                Text(text = folderIcon, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = folderName ?: title,
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.4).sp,
                                color = tokens.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = tokens.textMid,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // RIGHT: settings button (or 40dp placeholder to preserve layout on Settings page)
            if (showSettingsButton) {
                CircleIconButton(
                    icon = Icons.Filled.Settings,
                    description = "Paramètres",
                    onClick = onSettingsClick,
                    size = 40,
                )
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun BrandText(gold: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(gold),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Mazzika",
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = (-0.72).sp,
            color = textColor,
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Int,
) {
    val tokens = LocalStudioTokens.current
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (tokens.isDark) tokens.surfaceHi else tokens.surface)
            .then(
                if (!tokens.isDark) Modifier.border(
                    width = 1.dp,
                    color = tokens.cardBorder,
                    shape = CircleShape,
                ) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tokens.text,
            modifier = Modifier.size(18.dp),
        )
    }
}
