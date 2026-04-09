package com.mazzika.lyrics.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldLight
import com.mazzika.lyrics.ui.theme.PlayfairDisplay

@Composable
fun MazzikaTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsButton: Boolean = true,
    folderName: String? = null,
    folderIcon: String? = null,
) {
    val goldLineColor = Color(0x26C5A028)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF100E0C))
            .drawBehind {
                val lineY = size.height
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, goldLineColor, Color.Transparent),
                    ),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // LEFT side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (showBackButton) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF161412))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Gold,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                when {
                    folderName != null -> {
                        if (folderIcon != null) {
                            Text(
                                text = folderIcon,
                                fontSize = 18.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = folderName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = DarkTextPrimary,
                        )
                    }
                    title == "Mazzika" -> {
                        Text(
                            text = title,
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Gold, GoldLight, Gold),
                                ),
                            ),
                        )
                    }
                    else -> {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = DarkTextPrimary,
                        )
                    }
                }
            }

            // RIGHT side - Settings button
            if (showSettingsButton) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161412))
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Paramètres",
                        tint = Gold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
