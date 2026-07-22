package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 歌手列表滚动条隐藏延迟，按需求固定为 5 秒。
 */
internal const val DESKTOP_LOCAL_ARTIST_SCROLLBAR_HIDE_DELAY_MILLIS: Long = DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS

// 歌手列表滚动条监听实际滚动位置变化，避免瞬时滚轮事件错过显示窗口。
@Composable
internal fun DesktopLocalArtistScrollbar(
    listState: LazyListState,
    visibilitySignal: Int,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = listState.layoutInfo
    val visibleItemsCount: Int = layoutInfo.visibleItemsInfo.size.coerceAtLeast(minimumValue = 1)
    val metrics: DesktopHomeSongScrollbarMetrics =
        resolveDesktopHomeSongScrollbarMetrics(
            totalItemsCount = layoutInfo.totalItemsCount,
            visibleItemsCount = visibleItemsCount,
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            canScrollForward = listState.canScrollForward,
            canScrollBackward = listState.canScrollBackward,
            viewportHeightPx = layoutInfo.viewportSize.height.toFloat(),
        )
    var isScrollbarVisible: Boolean by remember { mutableStateOf(value = false) }
    val firstVisibleItemIndex: Int = listState.firstVisibleItemIndex
    val firstVisibleItemScrollOffset: Int = listState.firstVisibleItemScrollOffset
    val hasObservedScroll: Boolean =
        visibilitySignal > 0 ||
            firstVisibleItemIndex > 0 ||
            firstVisibleItemScrollOffset > 0 ||
            listState.isScrollInProgress
    LaunchedEffect(
        metrics.isVisible,
        visibilitySignal,
        firstVisibleItemIndex,
        firstVisibleItemScrollOffset,
        listState.isScrollInProgress,
    ) {
        if (!metrics.isVisible || !hasObservedScroll) {
            isScrollbarVisible = false
            return@LaunchedEffect
        }
        isScrollbarVisible = true
        delay(timeMillis = DESKTOP_LOCAL_ARTIST_SCROLLBAR_HIDE_DELAY_MILLIS)
        isScrollbarVisible = false
    }
    if (!metrics.isVisible || !isScrollbarVisible) {
        return
    }
    val thumbHeight: Dp = with(LocalDensity.current) { metrics.thumbHeightPx.toDp() }
    Box(
        modifier =
            modifier
                .width(width = 12.dp)
                .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .width(width = 6.dp)
                    .fillMaxHeight()
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color(0x26006B5C)),
        )
        Box(
            modifier =
                Modifier
                    .desktopLocalArtistScrollbarOffset(offsetPx = metrics.thumbOffsetPx)
                    .width(width = 6.dp)
                    .height(height = thumbHeight)
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color(0xFF006B5C)),
        )
    }
}

// 歌手列表滚动条只在内容超出列表视口时具备显示条件。
internal fun shouldShowDesktopLocalArtistScrollbar(
    totalItemsCount: Int,
    visibleItemsCount: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): Boolean =
    shouldShowDesktopHomeSongScrollbar(
        totalItemsCount = totalItemsCount,
        visibleItemsCount = visibleItemsCount,
        canScrollForward = canScrollForward,
        canScrollBackward = canScrollBackward,
    )

// 歌手列表滚动条滚动中立即显示，停止滚动满 5 秒后隐藏。
internal fun shouldRenderDesktopLocalArtistScrollbar(
    hasScrollableContent: Boolean,
    isScrollInProgress: Boolean,
    idleDurationMillis: Long,
): Boolean =
    shouldRenderDesktopHomeSongScrollbar(
        hasScrollableContent = hasScrollableContent,
        isScrollInProgress = isScrollInProgress,
        idleDurationMillis = idleDurationMillis,
    )

// 用像素偏移定位滑块，保持和首页滚动条相同的末端对齐方式。
private fun Modifier.desktopLocalArtistScrollbarOffset(offsetPx: Float): Modifier =
    this.then(
        other =
            Modifier.offset {
                IntOffset(
                    x = 0,
                    y = offsetPx.roundToInt(),
                )
            },
    )
