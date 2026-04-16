package com.mazzika.lyrics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val MusicIconColors = listOf(Color(0xFFE8D48B), Color(0xFFC5A028))
val FolderIconColors = listOf(Color(0xFFFFB74D), Color(0xFFFF9800))
val SyncIconColors = listOf(Color(0xFF80CBC4), Color(0xFF26A69A))
val ImportIconColors = listOf(Color(0xFF90CAF9), Color(0xFF42A5F5))
val SettingsIconColors = listOf(Color(0xFFBDBDBD), Color(0xFF757575))

@Composable
fun IconCircle(
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    iconTint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(colors = gradientColors),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}
