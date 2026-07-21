package com.yanhao.kmpmusic.feature.app.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * 播放页下滑关闭阈值，0.5 表示底层 App 至少露出半屏。
 */
private const val PLAYER_DISMISS_DRAG_FRACTION = 0.5f

/**
 * 播放页未达到关闭阈值时的回弹时长(220ms)。
 */
private const val PLAYER_DRAG_SETTLE_BACK_MILLIS = 220

/**
 * 播放页完整滑出屏幕时长(320ms)，与打开播放页完整滑入的速度保持对称。
 */
private const val PLAYER_DRAG_SETTLE_DISMISS_FULL_MILLIS = 320

/**
 * 为播放页覆盖层提供下滑关闭手势，保证普通无 chrome 页面不继承播放页手势。
 */
@Composable
internal fun MobilePlayerOverlayGesture(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
        val density = LocalDensity.current
        val contentHeightPx: Float = with(density) { maxHeight.toPx() }
        val navigationBarBottomPx: Float = WindowInsets.navigationBars.getBottom(density = density).toFloat()
        val dismissDistancePx: Float =
            calculatePlayerDismissDistance(
                contentHeightPx = contentHeightPx,
                navigationBarBottomPx = navigationBarBottomPx,
            )
        var dragOffsetPx: Float by remember { mutableFloatStateOf(value = 0f) }
        val dragState =
            rememberDraggableState { dragDeltaPx: Float ->
                dragOffsetPx =
                    calculatePlayerDragOffset(
                        currentOffsetPx = dragOffsetPx,
                        dragDeltaPx = dragDeltaPx,
                    )
            }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                    ),
        )
        content(
            Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(x = 0, y = dragOffsetPx.roundToInt())
                }.draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (isPlayerOverlayDismissDrag(
                                dragOffsetPx = dragOffsetPx,
                                screenHeightPx = dismissDistancePx,
                            )
                        ) {
                            animatePlayerDragOffsetToDismiss(
                                currentOffsetPx = dragOffsetPx,
                                screenHeightPx = dismissDistancePx,
                                updateOffsetPx = { nextOffsetPx: Float -> dragOffsetPx = nextOffsetPx },
                            )
                            onDismiss()
                        } else {
                            animatePlayerDragOffsetBack(
                                initialOffsetPx = dragOffsetPx,
                                updateOffsetPx = { nextOffsetPx: Float -> dragOffsetPx = nextOffsetPx },
                            )
                        }
                    },
                ),
        )
    }
}

/**
 * 播放页关闭距离包含系统导航栏底部区域，保证整页从屏幕物理底边退出。
 */
internal fun calculatePlayerDismissDistance(
    contentHeightPx: Float,
    navigationBarBottomPx: Float,
): Float {
    if (contentHeightPx <= 0f) {
        return 0f
    }
    return contentHeightPx + navigationBarBottomPx.coerceAtLeast(minimumValue = 0f)
}

/**
 * 累计播放页下滑距离，向上拖动只回收到 0，避免页面被拖出顶部。
 */
internal fun calculatePlayerDragOffset(
    currentOffsetPx: Float,
    dragDeltaPx: Float,
): Float = (currentOffsetPx + dragDeltaPx).coerceAtLeast(minimumValue = 0f)

/**
 * 判断播放页是否达到关闭阈值；屏幕高度无效时保守地回弹。
 */
internal fun isPlayerOverlayDismissDrag(
    dragOffsetPx: Float,
    screenHeightPx: Float,
): Boolean {
    if (screenHeightPx <= 0f) {
        return false
    }
    return dragOffsetPx >= screenHeightPx * PLAYER_DISMISS_DRAG_FRACTION
}

/**
 * 计算手势关闭时的最终位移，避免超过屏幕高度时把页面反向拉回。
 */
internal fun calculatePlayerDismissTargetOffset(
    dragOffsetPx: Float,
    screenHeightPx: Float,
): Float {
    if (screenHeightPx <= 0f) {
        return dragOffsetPx.coerceAtLeast(minimumValue = 0f)
    }
    return maxOf(dragOffsetPx.coerceAtLeast(minimumValue = 0f), screenHeightPx)
}

/**
 * 计算松手关闭后的剩余滑出时长，确保半屏松手后继续向下移动而不是瞬间消失。
 */
internal fun calculatePlayerDismissSettleMillis(
    currentOffsetPx: Float,
    targetOffsetPx: Float,
    screenHeightPx: Float,
): Int {
    if (screenHeightPx <= 0f) {
        return 0
    }
    val remainingDistancePx: Float = (targetOffsetPx - currentOffsetPx).coerceAtLeast(minimumValue = 0f)
    if (remainingDistancePx <= 0f) {
        return 0
    }
    return (PLAYER_DRAG_SETTLE_DISMISS_FULL_MILLIS * (remainingDistancePx / screenHeightPx))
        .roundToInt()
        .coerceAtLeast(minimumValue = 1)
}

// 播放页未关闭时用独立动画回弹，不触发全局 chrome 动画。
private suspend fun animatePlayerDragOffsetBack(
    initialOffsetPx: Float,
    updateOffsetPx: (Float) -> Unit,
) {
    val dragAnimation = Animatable(initialValue = initialOffsetPx)
    dragAnimation.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = PLAYER_DRAG_SETTLE_BACK_MILLIS),
    ) {
        updateOffsetPx(value)
    }
    updateOffsetPx(0f)
}

// 手势关闭先把播放页继续滑出屏幕，再切导航；父级 outgoing 空层负责避免导航后的残影闪回。
private suspend fun animatePlayerDragOffsetToDismiss(
    currentOffsetPx: Float,
    screenHeightPx: Float,
    updateOffsetPx: (Float) -> Unit,
) {
    val targetOffsetPx: Float =
        calculatePlayerDismissTargetOffset(
            dragOffsetPx = currentOffsetPx,
            screenHeightPx = screenHeightPx,
        )
    val durationMillis: Int =
        calculatePlayerDismissSettleMillis(
            currentOffsetPx = currentOffsetPx,
            targetOffsetPx = targetOffsetPx,
            screenHeightPx = screenHeightPx,
        )
    if (durationMillis > 0) {
        val dragAnimation = Animatable(initialValue = currentOffsetPx)
        dragAnimation.animateTo(
            targetValue = targetOffsetPx,
            animationSpec = tween(durationMillis = durationMillis),
        ) {
            updateOffsetPx(value)
        }
    }
    updateOffsetPx(targetOffsetPx)
    withFrameNanos { }
}
