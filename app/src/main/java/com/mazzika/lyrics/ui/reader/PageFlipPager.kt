package com.mazzika.lyrics.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    var size by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var targetPage by remember { mutableIntStateOf(currentPage) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val isZoomed = scale > 1.05f

    val animatedOffset by animateFloatAsState(
        targetValue = if (targetPage != currentPage) {
            if (targetPage > currentPage) -size.width.toFloat() else size.width.toFloat()
        } else {
            0f
        },
        animationSpec = tween(300),
        finishedListener = {
            if (targetPage != currentPage) {
                onPageChanged(targetPage)
                dragOffset = 0f
            }
        },
        label = "pageFlip",
    )

    // Render bitmap when page or size changes
    LaunchedEffect(currentPage, size) {
        if (size.width > 0) {
            bitmap = renderPage(currentPage, size.width)
        }
    }

    // Reset zoom when page changes
    LaunchedEffect(currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        targetPage = currentPage
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        if (newScale > 1.05f) {
            offsetX += panChange.x
            offsetY += panChange.y
        }
        if (newScale <= 1.05f) {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(isZoomed, pageCount, currentPage) {
                if (!isZoomed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = size.width * 0.25f
                            if (abs(dragOffset) > threshold) {
                                val newPage = if (dragOffset < 0) {
                                    (currentPage + 1).coerceAtMost(pageCount - 1)
                                } else {
                                    (currentPage - 1).coerceAtLeast(0)
                                }
                                if (newPage != currentPage) {
                                    targetPage = newPage
                                } else {
                                    dragOffset = 0f
                                }
                            } else {
                                dragOffset = 0f
                            }
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        },
                    )
                }
            }
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
            .transformable(state = transformState, enabled = true)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = if (isZoomed) offsetX else {
                    if (targetPage != currentPage) animatedOffset else dragOffset
                }
                translationY = if (isZoomed) offsetY else 0f
            },
    ) {
        bitmap?.let { bmp ->
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
