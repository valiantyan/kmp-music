package com.yanhao.kmpmusic.feature.desktop.screens

import kotlin.math.roundToInt

// 首页滚动条停止滚动后隐藏的延迟时间，按产品要求固定为 5 秒。
internal const val DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS: Long = 5_000L

/**
 * 首页歌曲列表滚动条的计算结果。
 *
 * @property isVisible 长列表是否具备显示滚动条的条件。
 * @property thumbHeightPx 滚动滑块高度，单位为像素。
 * @property thumbOffsetPx 滚动滑块距离顶部的偏移，单位为像素。
 */
internal data class DesktopHomeSongScrollbarMetrics(
    val isVisible: Boolean,
    val thumbHeightPx: Float,
    val thumbOffsetPx: Float,
)

// 滚动条显示策略把列表初始布局、滚动中和滚到底三种状态都纳入判断。
internal fun shouldShowDesktopHomeSongScrollbar(
    totalItemsCount: Int,
    visibleItemsCount: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): Boolean = totalItemsCount > visibleItemsCount || canScrollForward || canScrollBackward

// 渲染策略要求滚动中立即显示，停止滚动满 5 秒后隐藏。
internal fun shouldRenderDesktopHomeSongScrollbar(
    hasScrollableContent: Boolean,
    isScrollInProgress: Boolean,
    idleDurationMillis: Long,
): Boolean {
    if (!hasScrollableContent) {
        return false
    }
    if (isScrollInProgress) {
        return true
    }
    return idleDurationMillis < DESKTOP_HOME_SONG_SCROLLBAR_HIDE_DELAY_MILLIS
}

// 根据当前可视窗口计算滚动条滑块尺寸和位置，避免长列表滑块过小无法拖动。
internal fun resolveDesktopHomeSongScrollbarMetrics(
    totalItemsCount: Int,
    visibleItemsCount: Int,
    firstVisibleItemIndex: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
    viewportHeightPx: Float,
): DesktopHomeSongScrollbarMetrics {
    val isVisible: Boolean =
        shouldShowDesktopHomeSongScrollbar(
            totalItemsCount = totalItemsCount,
            visibleItemsCount = visibleItemsCount,
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
        )
    if (!isVisible || viewportHeightPx <= 0f) {
        return DesktopHomeSongScrollbarMetrics(
            isVisible = false,
            thumbHeightPx = 0f,
            thumbOffsetPx = 0f,
        )
    }
    val safeVisibleItemsCount: Int = visibleItemsCount.coerceAtLeast(minimumValue = 1)
    val safeTotalItemsCount: Int = totalItemsCount.coerceAtLeast(minimumValue = safeVisibleItemsCount)
    val thumbHeightPx: Float =
        (viewportHeightPx * safeVisibleItemsCount / safeTotalItemsCount)
            .coerceIn(
                minimumValue = 48f,
                maximumValue = viewportHeightPx,
            )
    val maxFirstVisibleItemIndex: Int = (safeTotalItemsCount - safeVisibleItemsCount).coerceAtLeast(minimumValue = 1)
    val maxThumbOffsetPx: Float = viewportHeightPx - thumbHeightPx
    val thumbOffsetPx: Float =
        maxThumbOffsetPx *
            firstVisibleItemIndex.coerceIn(
                minimumValue = 0,
                maximumValue = maxFirstVisibleItemIndex,
            ) / maxFirstVisibleItemIndex
    return DesktopHomeSongScrollbarMetrics(
        isVisible = true,
        thumbHeightPx = thumbHeightPx,
        thumbOffsetPx = thumbOffsetPx,
    )
}

// 拖拽距离按视口高度映射到歌曲索引，让用户可以快速跳过几十首歌曲。
internal fun resolveDesktopHomeSongScrollbarTargetIndex(
    currentFirstVisibleItemIndex: Int,
    totalItemsCount: Int,
    visibleItemsCount: Int,
    viewportHeightPx: Float,
    dragAmountPx: Float,
): Int {
    if (viewportHeightPx <= 0f || totalItemsCount <= visibleItemsCount) {
        return 0
    }
    val maxFirstVisibleItemIndex: Int = (totalItemsCount - visibleItemsCount).coerceAtLeast(minimumValue = 0)
    val itemDelta: Int = (dragAmountPx / viewportHeightPx * totalItemsCount).roundToInt()
    return (currentFirstVisibleItemIndex + itemDelta).coerceIn(
        minimumValue = 0,
        maximumValue = maxFirstVisibleItemIndex,
    )
}
