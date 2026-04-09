package com.mazzika.lyrics.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.sync.SyncRole
import com.mazzika.lyrics.ui.theme.Success

@Composable
fun SessionBanner(
    isVisible: Boolean,
    role: SyncRole,
    sessionName: String,
    connectedCount: Int,
    isConnectionLost: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
    ) {
        val displayName = if (isConnectionLost) "Connexion perdue" else sessionName

        val bgBrush = if (isConnectionLost) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFB71C1C).copy(alpha = 0.2f),
                    Color(0xFFB71C1C).copy(alpha = 0.1f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Success.copy(alpha = 0.15f),
                    Success.copy(alpha = 0.08f),
                ),
            )
        }

        val borderColor = if (isConnectionLost) {
            Color(0xFFB71C1C).copy(alpha = 0.3f)
        } else {
            Success.copy(alpha = 0.2f)
        }

        val textColor = if (isConnectionLost) Color(0xFFEF9A9A) else Success

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgBrush)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(textColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayName,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "$connectedCount connectés",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
    }
}
