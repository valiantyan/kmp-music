package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// 首页歌曲列表滚动时显示滚动条，停止滚动后延迟隐藏，避免常驻视觉噪音。
@Composable
internal fun DesktopHomeSongScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = listState.layoutInfo
    val visibleItemsCount: Int = layoutInfo.visibleItemsInfo.size
    val metrics: DesktopHomeSongScrollbarMetrics =
        resolveDesktopHomeSongScrollbarMetrics(
            totalItemsCount = layoutInfo.totalItemsCount,
            visibleItemsCount = visibleItemsCount,
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            canScrollForward = listState.canScrollForward,
            canScrollBackward = listState.canScrollBackward,
            viewportHeightPx = layoutInfo.viewportSize.height.toFloat(),
        )
    var isDraggingScrollbar: Boolean by remember { mutableStateOf(value = false) }
    var isScrollbarVisible: Boolean by remember { mutableStateOf(value = false) }
    val isScrollbarActive: Boolean = listState.isScrollInProgress || isDraggingScrollbar
    LaunchedEffect(metrics.isVisible, isScrollbarActive) {
        if (!metrics.isVisible) {
            isScrollbarVisible = false
            return@LaunchedEffect
        }
        if (isScrollbarActive) {
            isScrollbarVisible = true
            return@LaunchedEffect
        }
        if (isScrollbarVisible) {
            delay(timeMillis = DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS)
            isScrollbarVisible = false
        }
    }
    if (!isScrollbarVisible) {
        return
    }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val thumbHeight: Dp = with(LocalDensity.current) { metrics.thumbHeightPx.toDp() }
    Box(
        modifier =
            modifier
                .width(10.dp)
                .fillMaxHeight()
                .pointerInput(listState) {
                    detectVerticalDragGestures(
                        onDragStart = { _: Offset -> isDraggingScrollbar = true },
                        onDragEnd = { isDraggingScrollbar = false },
                        onDragCancel = { isDraggingScrollbar = false },
                    ) { change, dragAmount ->
                        val targetIndex: Int =
                            resolveDesktopHomeSongScrollbarTargetIndex(
                                currentFirstVisibleItemIndex = listState.firstVisibleItemIndex,
                                totalItemsCount = listState.layoutInfo.totalItemsCount,
                                visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size,
                                viewportHeightPx =
                                    listState.layoutInfo.viewportSize.height
                                        .toFloat(),
                                dragAmountPx = dragAmount,
                            )
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                        coroutineScope.launch {
                            listState.scrollToItem(index = targetIndex)
                        }
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x14006B5C)),
        )
        Box(
            modifier =
                Modifier
                    .verticalScrollbarOffset(offsetPx = metrics.thumbOffsetPx)
                    .width(4.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x99006B5C)),
        )
    }
}

// 用像素偏移定位滑块，避免 dp 四舍五入导致滚动到末尾时滑块对不齐。
private fun Modifier.verticalScrollbarOffset(offsetPx: Float): Modifier =
    this.then(
        other =
            Modifier.offset {
                IntOffset(
                    x = 0,
                    y = offsetPx.roundToInt(),
                )
            },
    )
