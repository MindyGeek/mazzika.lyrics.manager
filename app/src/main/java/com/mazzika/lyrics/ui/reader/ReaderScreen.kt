package com.mazzika.lyrics.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mazzika.lyrics.ui.theme.GoldDark
import com.mazzika.lyrics.ui.theme.GoldLightDark
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.Success
import com.mazzika.lyrics.ui.theme.Danger

enum class SyncMode { NONE, PILOT, FOLLOWER }

// The reader is always visually dark (immersive PDF viewer).
private val ReaderGold = GoldDark
private val ReaderGoldLight = GoldLightDark

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
    LaunchedEffect(pilotCurrentPage, pageCount, isDetached) {
        if (syncMode == SyncMode.FOLLOWER && pageCount > 0 && !isDetached) {
            if (currentPage != pilotCurrentPage) viewModel.setCurrentPage(pilotCurrentPage)
        }
    }

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
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
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

        // ── Top toolbar
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopReaderBar(
                title = title,
                syncMode = syncMode,
                connectedCount = connectedCount,
                isConnectionHealthy = isConnectionHealthy,
                isDetached = isDetached,
                onBack = onNavigateBack,
                onToggleDetached = onToggleDetached,
                onNavigateToSync = onNavigateToSync,
            )
        }

        // ── Bottom: page dots + Fermer pill (+ Save for follower temp file)
        AnimatedVisibility(
            visible = showToolbar && pageCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomReaderBar(
                currentPage = currentPage,
                pageCount = pageCount,
                onClose = onNavigateBack,
                showSave = syncMode == SyncMode.FOLLOWER && isTempFile,
                onSave = onSaveToCatalogue,
            )
        }
    }
}

@Composable
private fun TopReaderBar(
    title: String,
    syncMode: SyncMode,
    connectedCount: Int,
    isConnectionHealthy: Boolean,
    isDetached: Boolean,
    onBack: () -> Unit,
    onToggleDetached: (() -> Unit)?,
    onNavigateToSync: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = "Retour",
            onClick = onBack,
        )
        Spacer(Modifier.width(12.dp))

        // Title + status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (syncMode != SyncMode.NONE) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isConnectionHealthy) Success else Danger),
                    )
                    Spacer(Modifier.width(6.dp))
                    val roleLabel = if (syncMode == SyncMode.PILOT) "Pilote" else "Follower"
                    Text(
                        text = "$roleLabel • $connectedCount connecté${if (connectedCount > 1) "s" else ""}",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        // Right action
        when {
            syncMode == SyncMode.FOLLOWER && onToggleDetached != null -> {
                var showTooltip by remember { mutableStateOf(false) }
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
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
                            tint = if (isDetached) Danger else ReaderGoldLight,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (showTooltip) {
                        Box(
                            modifier = Modifier
                                .padding(top = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A2A2A))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (isDetached) "Re-synchroniser" else "Navigation libre",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                fontSize = 11.sp,
                            )
                        }
                        LaunchedEffect(showTooltip) {
                            kotlinx.coroutines.delay(2000)
                            showTooltip = false
                        }
                    }
                }
            }
            syncMode == SyncMode.NONE -> {
                GlassIconButton(
                    icon = Icons.Filled.SyncAlt,
                    description = "Synchroniser",
                    onClick = onNavigateToSync,
                )
            }
            else -> Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun BottomReaderBar(
    currentPage: Int,
    pageCount: Int,
    onClose: () -> Unit,
    showSave: Boolean = false,
    onSave: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                ),
            )
            .padding(top = 20.dp, bottom = 40.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Page dots + text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (pageCount in 1..10) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (i in 0 until pageCount) {
                        val isActive = i == currentPage
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (isActive) 20.dp else 6.dp,
                                    height = 6.dp,
                                )
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isActive)
                                        Brush.linearGradient(listOf(ReaderGoldLight, ReaderGold))
                                    else
                                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f))),
                                ),
                        )
                    }
                }
            }
            Text(
                text = "Page ${currentPage + 1} / $pageCount",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }

        // Close pill
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showSave && onSave != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(ReaderGoldLight, ReaderGold)))
                        .clickable(onClick = onSave)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Sauvegarder",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Fermer",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}
