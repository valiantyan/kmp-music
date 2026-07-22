package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

/**
 * 当前列表滚动位置，用于识别短暂滚轮事件造成的真实位置变化。
 *
 * @property firstVisibleItemIndex 首个可见条目索引。
 * @property firstVisibleItemScrollOffset 首个可见条目像素偏移。
 */
private data class DesktopLazyListScrollPosition(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
)

/**
 * 桌面长列表统一滚动条，集中处理滚动观察、拖拽、几何计算与自动隐藏。
 */
@Composable
internal fun DesktopAutoHideLazyScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    style: DesktopLazyScrollbarStyle = DesktopLazyScrollbarStyle.Standard,
) {
    val layoutInfo: LazyListLayoutInfo = listState.layoutInfo
    val metrics: DesktopLazyScrollbarMetrics =
        resolveDesktopLazyScrollbarMetrics(
            totalItemsCount = layoutInfo.totalItemsCount,
            visibleItemsCount = layoutInfo.visibleItemsInfo.size,
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            canScrollForward = listState.canScrollForward,
            canScrollBackward = listState.canScrollBackward,
            viewportHeightPx = layoutInfo.viewportSize.height.toFloat(),
        )
    val scrollPosition: DesktopLazyListScrollPosition =
        DesktopLazyListScrollPosition(
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
        )
    var previousScrollPosition: DesktopLazyListScrollPosition by remember(listState) {
        mutableStateOf(value = scrollPosition)
    }
    var isDraggingScrollbar: Boolean by remember { mutableStateOf(value = false) }
    var isScrollbarVisible: Boolean by remember { mutableStateOf(value = false) }
    var dragState: DesktopLazyScrollbarDragState by remember(listState) {
        mutableStateOf(
            value = startDesktopLazyScrollbarDrag(initialFirstVisibleItemIndex = listState.firstVisibleItemIndex),
        )
    }
    val didScroll: Boolean = scrollPosition != previousScrollPosition
    val isScrollbarActive: Boolean = listState.isScrollInProgress || isDraggingScrollbar
    LaunchedEffect(metrics.isVisible, scrollPosition, isScrollbarActive) {
        previousScrollPosition = scrollPosition
        if (!metrics.isVisible) {
            isScrollbarVisible = false
            return@LaunchedEffect
        }
        if (isScrollbarActive) {
            isScrollbarVisible = true
            return@LaunchedEffect
        }
        if (didScroll) {
            isScrollbarVisible = true
        }
        if (isScrollbarVisible) {
            delay(timeMillis = DESKTOP_AUTO_HIDE_LAZY_SCROLLBAR_DELAY_MILLIS)
            isScrollbarVisible = false
        }
    }
    if (!metrics.isVisible) {
        return
    }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val visualSpec: DesktopLazyScrollbarVisualSpec = resolveDesktopLazyScrollbarVisualSpec(style = style)
    val thumbHeight: Dp = with(LocalDensity.current) { metrics.thumbHeightPx.toDp() }
    val visualAlpha: Float = if (isScrollbarVisible) 1f else 0f
    Box(
        modifier =
            modifier
                .width(width = visualSpec.containerWidth)
                .fillMaxHeight()
                .pointerInput(listState) {
                    detectVerticalDragGestures(
                        onDragStart = { _: Offset ->
                            dragState =
                                startDesktopLazyScrollbarDrag(
                                    initialFirstVisibleItemIndex = listState.firstVisibleItemIndex,
                                )
                            isDraggingScrollbar = true
                        },
                        onDragEnd = { isDraggingScrollbar = false },
                        onDragCancel = { isDraggingScrollbar = false },
                    ) { change, dragAmount: Float ->
                        val updatedDragState: DesktopLazyScrollbarDragState =
                            accumulateDesktopLazyScrollbarDrag(
                                state = dragState,
                                dragAmountPx = dragAmount,
                            )
                        dragState = updatedDragState
                        val targetIndex: Int =
                            resolveDesktopLazyScrollbarTargetIndex(
                                currentFirstVisibleItemIndex = updatedDragState.initialFirstVisibleItemIndex,
                                totalItemsCount = listState.layoutInfo.totalItemsCount,
                                visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size,
                                viewportHeightPx =
                                    listState.layoutInfo.viewportSize.height
                                        .toFloat(),
                                accumulatedDragAmountPx = updatedDragState.accumulatedDragAmountPx,
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
                    .width(width = visualSpec.trackWidth)
                    .fillMaxHeight()
                    .alpha(alpha = visualAlpha)
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = visualSpec.trackColor),
        )
        Box(
            modifier =
                Modifier
                    .desktopLazyScrollbarOffset(offsetPx = metrics.thumbOffsetPx)
                    .width(width = visualSpec.thumbWidth)
                    .height(height = thumbHeight)
                    .alpha(alpha = visualAlpha)
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = visualSpec.thumbColor),
        )
    }
}

// 像素偏移避免 dp 四舍五入导致滚动到末尾时滑块对不齐。
private fun Modifier.desktopLazyScrollbarOffset(offsetPx: Float): Modifier =
    this.then(
        other =
            Modifier.offset {
                IntOffset(
                    x = 0,
                    y = offsetPx.roundToInt(),
                )
            },
    )
