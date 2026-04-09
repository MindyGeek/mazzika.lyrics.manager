package com.mazzika.lyrics.ui.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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

    Log.d("PageFlipPager", "Composing with pageCount=$pageCount, currentPage=$currentPage")

    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { pageCount },
    )

    // Sync external page changes to pager
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage && currentPage < pageCount) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Report settled page changes from pager to parent
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            Log.d("PageFlipPager", "Settled on page $page")
            onPageChanged(page)
        }
    }

    // Tap detection on the whole pager (does NOT interfere with pager's scrolling)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCenterTap,
            ),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            var pageSize by remember { mutableStateOf(IntSize.Zero) }
            var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(pageIndex, pageSize) {
                if (pageSize.width > 0) {
                    Log.d("PageFlipPager", "Rendering page $pageIndex at width ${pageSize.width}")
                    pageBitmap = renderPage(pageIndex, pageSize.width)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { pageSize = it },
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
}
