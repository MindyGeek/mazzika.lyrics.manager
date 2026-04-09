package com.mazzika.lyrics.ui.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

@Composable
fun PageFlipPager(
    pageCount: Int,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    renderPage: (pageIndex: Int, width: Int) -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 0) return

    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { pageCount },
    )

    // Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val isZoomed = scale > 1.05f

    // Sync external page changes
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage && currentPage < pageCount) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Report page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            Log.d("PageFlipPager", "Settled on page $page")
            onPageChanged(page)
        }
    }

    // Reset zoom on page change
    LaunchedEffect(pagerState.settledPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !isZoomed,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { pageIndex ->
        var pageSize by remember { mutableStateOf(IntSize.Zero) }
        var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(pageIndex, pageSize) {
            if (pageSize.width > 0) {
                pageBitmap = renderPage(pageIndex, pageSize.width)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { pageSize = it }
                .clipToBounds()
                // Custom gesture: only consume multi-touch (2 fingers) for zoom
                // Single finger passes through to HorizontalPager for swipe
                .pointerInput(isZoomed) {
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    var pendingTapRunnable: Runnable? = null

                    awaitPointerEventScope {
                        var downPosition = Offset.Zero
                        var downTime = 0L
                        var wasDrag = false
                        var lastTapTime = 0L

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (event.changes.size == 1) {
                                        downPosition = event.changes[0].position
                                        downTime = System.currentTimeMillis()
                                        wasDrag = false
                                    }
                                }
                                PointerEventType.Move -> {
                                    when {
                                        // 2+ fingers: zoom and pan
                                        pressed.size >= 2 -> {
                                            wasDrag = true
                                            val zoomChange = calcZoom(event)
                                            val panChange = calcPan(event)

                                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                            scale = newScale
                                            if (newScale > 1.05f) {
                                                offsetX += panChange.x
                                                offsetY += panChange.y
                                            } else {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                        // 1 finger while zoomed: pan
                                        isZoomed && pressed.size == 1 -> {
                                            wasDrag = true
                                            val panChange = calcPan(event)
                                            offsetX += panChange.x
                                            offsetY += panChange.y
                                            event.changes.forEach { it.consume() }
                                        }
                                        // 1 finger not zoomed: check if dragging
                                        pressed.size == 1 -> {
                                            val moved = dist(event.changes[0].position, downPosition)
                                            if (moved > 20f) wasDrag = true
                                        }
                                    }
                                }
                                PointerEventType.Release -> {
                                    val elapsed = System.currentTimeMillis() - downTime
                                    if (!wasDrag && elapsed < 300 && event.changes.none { it.pressed }) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTime < 300) {
                                            // Double tap: toggle zoom
                                            pendingTapRunnable?.let { handler.removeCallbacks(it) }
                                            pendingTapRunnable = null
                                            lastTapTime = 0L
                                            if (isZoomed) {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            } else {
                                                scale = 2.5f
                                            }
                                        } else {
                                            // Possible single tap: wait to see if double tap follows
                                            lastTapTime = now
                                            pendingTapRunnable?.let { handler.removeCallbacks(it) }
                                            val runnable = Runnable {
                                                onCenterTap()
                                                lastTapTime = 0L
                                            }
                                            pendingTapRunnable = runnable
                                            handler.postDelayed(runnable, 300)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = if (isZoomed) offsetX else 0f
                    translationY = if (isZoomed) offsetY else 0f
                    clip = true
                },
        ) {
            pageBitmap?.let { bmp ->
                val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = this.size.width
                    val canvasHeight = this.size.height
                    val bmpWidth = bmp.width.toFloat()
                    val bmpHeight = bmp.height.toFloat()

                    val fitScale = minOf(canvasWidth / bmpWidth, canvasHeight / bmpHeight)
                    val drawWidth = bmpWidth * fitScale
                    val drawHeight = bmpHeight * fitScale
                    val left = (canvasWidth - drawWidth) / 2f
                    val top = (canvasHeight - drawHeight) / 2f

                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt()),
                    )
                }
            }
        }
    }
}

/** Calculate zoom factor from a multi-touch event. */
private fun calcZoom(event: PointerEvent): Float {
    val pressed = event.changes.filter { it.pressed }
    if (pressed.size < 2) return 1f

    val currentDist = dist(pressed[0].position, pressed[1].position)
    val prevDist = dist(pressed[0].previousPosition, pressed[1].previousPosition)

    return if (prevDist > 0.001f) currentDist / prevDist else 1f
}

/** Calculate pan (average movement) from a pointer event. */
private fun calcPan(event: PointerEvent): Offset {
    val pressed = event.changes.filter { it.pressed }
    if (pressed.isEmpty()) return Offset.Zero

    var totalX = 0f
    var totalY = 0f
    pressed.forEach {
        totalX += it.position.x - it.previousPosition.x
        totalY += it.position.y - it.previousPosition.y
    }
    return Offset(totalX / pressed.size, totalY / pressed.size)
}

private fun dist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
