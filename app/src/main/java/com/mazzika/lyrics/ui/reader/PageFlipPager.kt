package com.mazzika.lyrics.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

@Composable
fun PageFlipPager(
    pageCount: Int,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    renderPage: (pageIndex: Int, width: Int) -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = currentPage) { pageCount }

    // Sync external page changes to pager
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Report page changes from pager to parent
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onPageChanged(page)
        }
    }

    // Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val isZoomed = scale > 1.05f

    // Reset zoom on page change
    LaunchedEffect(pagerState.settledPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Disable pager scrolling when zoomed
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !isZoomed,
        modifier = modifier.fillMaxSize(),
    ) { pageIndex ->
        var pageSize by remember { mutableStateOf(IntSize.Zero) }
        var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(pageIndex, pageSize) {
            if (pageSize.width > 0) {
                pageBitmap = renderPage(pageIndex, pageSize.width)
            }
        }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale
            if (newScale > 1.05f) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { pageSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onTap = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val centerZoneWidth = size.width * 0.3f
                            val centerZoneHeight = size.height * 0.3f
                            if (abs(offset.x - centerX) < centerZoneWidth / 2 &&
                                abs(offset.y - centerY) < centerZoneHeight / 2
                            ) {
                                onCenterTap()
                            }
                        },
                    )
                }
                .transformable(state = transformState)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = if (isZoomed) offsetX else 0f
                    translationY = if (isZoomed) offsetY else 0f
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
