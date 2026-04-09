package com.mazzika.lyrics.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldLight
import com.mazzika.lyrics.ui.theme.Success
import com.mazzika.lyrics.ui.theme.Error

enum class SyncMode { NONE, PILOT, FOLLOWER }

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSync: () -> Unit,
    modifier: Modifier = Modifier,
    syncMode: SyncMode = SyncMode.NONE,
    onPageChangedForSync: ((Int) -> Unit)? = null,
    syncPage: Int? = null,
    isDetached: Boolean = false,
    onToggleDetached: (() -> Unit)? = null,
    isTempFile: Boolean = false,
    onSaveToCatalogue: (() -> Unit)? = null,
    connectedCount: Int = 0,
    isConnectionHealthy: Boolean = true,
    pilotCurrentPage: Int = 0,
) {
    val title by viewModel.title.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showToolbar by viewModel.showToolbar.collectAsState()

    val context = LocalContext.current

    // Pilot: broadcast page changes
    LaunchedEffect(currentPage, syncMode) {
        if (syncMode == SyncMode.PILOT) {
            onPageChangedForSync?.invoke(currentPage)
        }
    }

    // Follower: apply page from pilot when not detached
    // Uses pilotCurrentPage which is always up-to-date (even when reader was closed)
    LaunchedEffect(pilotCurrentPage, pageCount, isDetached) {
        if (syncMode == SyncMode.FOLLOWER && pageCount > 0 && !isDetached) {
            if (currentPage != pilotCurrentPage) {
                viewModel.setCurrentPage(pilotCurrentPage)
            }
        }
    }

    // Also react to syncPage changes for real-time updates while reader is open
    LaunchedEffect(syncPage) {
        if (syncMode == SyncMode.FOLLOWER && syncPage != null && !isDetached) {
            viewModel.setCurrentPage(syncPage)
        }
    }

    // Immersive mode
    DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF000000)),
                    radius = 800f,
                ),
            ),
    ) {
        // Page content
        if (pageCount > 0) {
            PageFlipPager(
                pageCount = pageCount,
                currentPage = currentPage,
                onPageChanged = { viewModel.setCurrentPage(it) },
                onCenterTap = { viewModel.toggleToolbar() },
                renderPage = { pageIndex, width -> viewModel.renderPage(pageIndex, width) },
                modifier = Modifier.fillMaxSize(),
            )
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    // Session info when sync active
                    if (syncMode != SyncMode.NONE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            // Connection health dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnectionHealthy) Success else Error),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$connectedCount connect${if (connectedCount > 1) "és" else "é"}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    // Navigation libre toggle (follower) or sync icon (pilot/none)
                    if (syncMode == SyncMode.FOLLOWER && onToggleDetached != null) {
                        var showTooltip by remember { mutableStateOf(false) }

                        Box(contentAlignment = Alignment.TopEnd) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { showTooltip = true },
                                            onTap = { onToggleDetached() },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isDetached) Icons.Filled.SyncDisabled else Icons.Filled.Sync,
                                    contentDescription = if (isDetached) "Re-synchroniser" else "Navigation libre",
                                    tint = if (isDetached) Error else GoldLight,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            // Tooltip on long press
                            if (showTooltip) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = if (isDetached) "Re-synchroniser" else "Navigation libre",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                }

                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2000)
                                    showTooltip = false
                                }
                            }
                        }
                    } else if (syncMode == SyncMode.NONE) {
                        IconButton(onClick = onNavigateToSync) {
                            Icon(
                                imageVector = Icons.Filled.SyncAlt,
                                contentDescription = "Synchroniser",
                                tint = GoldLight,
                            )
                        }
                    }
                }
            }
        }

        // Bottom page indicator
        AnimatedVisibility(
            visible = showToolbar && pageCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Dot indicators (show max 10 dots)
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

        // "Fermer" pill button at bottom center
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp),
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.2f),
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fermer",
                    fontSize = 14.sp,
                )
            }
        }

        // Navigation libre pill removed — now in toolbar icon

        // Follower: Save to catalogue button (shown only when temp file)
        if (syncMode == SyncMode.FOLLOWER && isTempFile && onSaveToCatalogue != null) {
            Button(
                onClick = onSaveToCatalogue,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = "\uD83D\uDCBE Sauvegarder",
                    fontSize = 13.sp,
                )
            }
        }
    }
}

