package com.mazzika.lyrics.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldLight
import com.mazzika.lyrics.ui.theme.MazzikaLyricsTheme
import com.mazzika.lyrics.ui.theme.Success
import com.mazzika.lyrics.ui.theme.Error

// ==================== PREVIEWS ====================

/**
 * Preview statique du layout du lecteur (sans ViewModel).
 * Permet d'ajuster les valeurs de padding, taille, couleurs directement dans Android Studio.
 */
@Composable
internal fun ReaderScreenPreviewContent(
    title: String = "Cocktail Hadhra - Saber",
    currentPage: Int = 0,
    pageCount: Int = 5,
    showToolbar: Boolean = true,
    syncMode: SyncMode = SyncMode.NONE,
    connectedCount: Int = 0,
    isConnectionHealthy: Boolean = true,
    isDetached: Boolean = false,
    isTempFile: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF000000)),
                    radius = 800f,
                ),
            ),
    ) {
        // Fake page content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\uD83C\uDFB5",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Page ${currentPage + 1}",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                )
            }
        }

        // Top toolbar
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.width(48.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    // Session info
                    if (syncMode != SyncMode.NONE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnectionHealthy) Success else Error),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$connectedCount",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "\u21BB", color = GoldLight, fontSize = 20.sp)
                    }
                }
            }
        }

        // Bottom page indicator
        AnimatedVisibility(
            visible = showToolbar && pageCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                            ),
                        ),
                    )
                    .padding(bottom = 80.dp, top = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (pageCount <= 10) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (i in 0 until pageCount) {
                                val isActive = i == currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(
                                            width = if (isActive) 16.dp else 6.dp,
                                            height = 6.dp,
                                        )
                                        .clip(CircleShape)
                                        .background(if (isActive) Gold else Color.White.copy(alpha = 0.5f)),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "${currentPage + 1} / $pageCount",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // "Fermer" pill button
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp),
        ) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            ) {
                Text(text = "\u2715", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Fermer", fontSize = 14.sp)
            }
        }

        // Follower pills
        if (syncMode == SyncMode.FOLLOWER) {
            Button(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDetached) Gold else Color.White.copy(alpha = 0.15f),
                    contentColor = if (isDetached) Color.Black else Color.White,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = if (isDetached) "Re-synchroniser" else "Navigation libre",
                    fontSize = 13.sp,
                )
            }

            if (isTempFile) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(text = "\uD83D\uDCBE Sauvegarder", fontSize = 13.sp)
                }
            }
        }
    }
}

@Preview(
    name = "Lecteur - Mode normal",
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
)
@Composable
internal fun ReaderPreviewNormal() {
    MazzikaLyricsTheme(darkTheme = true) {
        ReaderScreenPreviewContent(
            title = "Ya Rayah - Dahmane El Harrachi",
            currentPage = 1,
            pageCount = 4,
            showToolbar = true,
        )
    }
}

@Preview(
    name = "Lecteur - Mode Pilote",
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
)
@Composable
internal fun ReaderPreviewPilot() {
    MazzikaLyricsTheme(darkTheme = true) {
        ReaderScreenPreviewContent(
            title = "Cocktail Hadhra - Saber",
            currentPage = 0,
            pageCount = 3,
            showToolbar = true,
            syncMode = SyncMode.PILOT,
            connectedCount = 4,
            isConnectionHealthy = true,
        )
    }
}

@Preview(
    name = "Lecteur - Mode Suiveur",
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
)
@Composable
internal fun ReaderPreviewFollower() {
    MazzikaLyricsTheme(darkTheme = true) {
        ReaderScreenPreviewContent(
            title = "Amazing Grace",
            currentPage = 2,
            pageCount = 6,
            showToolbar = true,
            syncMode = SyncMode.FOLLOWER,
            connectedCount = 3,
            isConnectionHealthy = true,
            isTempFile = true,
        )
    }
}

@Preview(
    name = "Lecteur - Suiveur detache",
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
)
@Composable
internal fun ReaderPreviewDetached() {
    MazzikaLyricsTheme(darkTheme = true) {
        ReaderScreenPreviewContent(
            title = "Hotel California",
            currentPage = 3,
            pageCount = 5,
            showToolbar = true,
            syncMode = SyncMode.FOLLOWER,
            connectedCount = 2,
            isConnectionHealthy = false,
            isDetached = true,
        )
    }
}

@Preview(
    name = "Lecteur - Tablette",
    showBackground = true,
    widthDp = 800,
    heightDp = 1200,
)
@Composable
internal fun ReaderPreviewTablet() {
    MazzikaLyricsTheme(darkTheme = true) {
        ReaderScreenPreviewContent(
            title = "Enta Omri - Oum Kalthoum",
            currentPage = 0,
            pageCount = 8,
            showToolbar = true,
            syncMode = SyncMode.PILOT,
            connectedCount = 5,
        )
    }
}
