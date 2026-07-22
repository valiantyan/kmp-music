package com.yanhao.kmpmusic.feature.desktop.components

import kotlin.math.roundToInt

/**
 * 桌面长列表停止滚动后隐藏滚动条的统一延迟时间（5000ms）。
 */
internal const val DESKTOP_AUTO_HIDE_LAZY_SCROLLBAR_DELAY_MILLIS: Long = 5_000L

/**
 * 桌面长列表滚动条的计算结果。
 *
 * @property isVisible 当前内容是否具备显示滚动条的条件。
 * @property thumbHeightPx 滚动滑块高度，单位为像素。
 * @property thumbOffsetPx 滚动滑块距离顶部的偏移，单位为像素。
 */
internal data class DesktopLazyScrollbarMetrics(
    val isVisible: Boolean,
    val thumbHeightPx: Float,
    val thumbOffsetPx: Float,
)

/**
 * 单次滚动条拖动会话，保留起始索引和未取整的累计像素距离。
 *
 * @property initialFirstVisibleItemIndex 拖动开始时的首个可见条目索引。
 * @property accumulatedDragAmountPx 本次拖动已累计的像素距离。
 */
internal data class DesktopLazyScrollbarDragState(
    val initialFirstVisibleItemIndex: Int,
    val accumulatedDragAmountPx: Float,
)

// 每次按下都从当前列表位置创建新会话，避免继承上一次拖动余量。
internal fun startDesktopLazyScrollbarDrag(initialFirstVisibleItemIndex: Int): DesktopLazyScrollbarDragState =
    DesktopLazyScrollbarDragState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        accumulatedDragAmountPx = 0f,
    )

// 先累计原始浮点位移再映射索引，避免小步手势在逐次取整时全部丢失。
internal fun accumulateDesktopLazyScrollbarDrag(
    state: DesktopLazyScrollbarDragState,
    dragAmountPx: Float,
): DesktopLazyScrollbarDragState = state.copy(accumulatedDragAmountPx = state.accumulatedDragAmountPx + dragAmountPx)

// 显示条件同时考虑数量与双向滚动能力，覆盖初始布局和列表末端。
internal fun shouldShowDesktopLazyScrollbar(
    totalItemsCount: Int,
    visibleItemsCount: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): Boolean = totalItemsCount > visibleItemsCount || canScrollForward || canScrollBackward

// 根据当前可视窗口计算滑块尺寸和位置，保证长列表滑块仍可拖动。
internal fun resolveDesktopLazyScrollbarMetrics(
    totalItemsCount: Int,
    visibleItemsCount: Int,
    firstVisibleItemIndex: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
    viewportHeightPx: Float,
): DesktopLazyScrollbarMetrics {
    val isVisible: Boolean =
        shouldShowDesktopLazyScrollbar(
            totalItemsCount = totalItemsCount,
            visibleItemsCount = visibleItemsCount,
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
        )
    if (!isVisible || viewportHeightPx <= 0f) {
        return DesktopLazyScrollbarMetrics(
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
    return DesktopLazyScrollbarMetrics(
        isVisible = true,
        thumbHeightPx = thumbHeightPx,
        thumbOffsetPx = thumbOffsetPx,
    )
}

// 拖拽距离按视口高度映射到列表索引，让用户可以快速跨越长列表。
internal fun resolveDesktopLazyScrollbarTargetIndex(
    currentFirstVisibleItemIndex: Int,
    totalItemsCount: Int,
    visibleItemsCount: Int,
    viewportHeightPx: Float,
    accumulatedDragAmountPx: Float,
): Int {
    if (viewportHeightPx <= 0f || totalItemsCount <= visibleItemsCount) {
        return 0
    }
    val maxFirstVisibleItemIndex: Int = (totalItemsCount - visibleItemsCount).coerceAtLeast(minimumValue = 0)
    val itemDelta: Int = (accumulatedDragAmountPx / viewportHeightPx * totalItemsCount).roundToInt()
    return (currentFirstVisibleItemIndex + itemDelta).coerceIn(
        minimumValue = 0,
        maximumValue = maxFirstVisibleItemIndex,
    )
}
