package com.yanhao.kmpmusic.feature.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 歌手详情页边界回弹状态。
 *
 * @property pullStretchHeight 当前下拉放大高度。
 * @property bottomBounceOffset 当前底部回弹位移。
 * @property nestedScrollConnection 只在列表到达边界后消费回弹手势。
 */
internal data class ArtistDetailPullStretchState(
    val pullStretchHeight: Dp,
    val bottomBounceOffset: Dp,
    val nestedScrollConnection: NestedScrollConnection,
)

// 底部回弹最大位移保持轻量，避免列表越过迷你播放器太多。
internal val artistDetailMaxBottomBounceHeight: Dp = 56.dp

/**
 * 记住歌手详情页边界回弹状态，避免把 overscroll 逻辑散进页面结构。
 */
@Composable
internal fun rememberArtistDetailPullStretchState(
    listState: LazyListState,
    maxPullStretchHeight: Dp,
    maxBottomBounceHeight: Dp = artistDetailMaxBottomBounceHeight,
): ArtistDetailPullStretchState {
    val density = LocalDensity.current
    val maxPullStretchPx: Float = with(density) { maxPullStretchHeight.toPx() }
    val maxBottomBouncePx: Float = with(density) { maxBottomBounceHeight.toPx() }
    val pullStretch: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) }
    val bottomBounce: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) }
    val scope: CoroutineScope = rememberCoroutineScope()
    val nestedScrollConnection: NestedScrollConnection =
        remember(
            listState,
            maxPullStretchPx,
            maxBottomBouncePx,
            pullStretch,
            bottomBounce,
            scope,
        ) {
            createArtistDetailPullStretchConnection(
                listState = listState,
                pullStretch = pullStretch,
                bottomBounce = bottomBounce,
                maxPullStretchPx = maxPullStretchPx,
                maxBottomBouncePx = maxBottomBouncePx,
                scope = scope,
            )
        }
    return ArtistDetailPullStretchState(
        pullStretchHeight = with(density) { pullStretch.value.toDp() },
        bottomBounceOffset = with(density) { bottomBounce.value.toDp() },
        nestedScrollConnection = nestedScrollConnection,
    )
}

// 创建 NestedScroll 连接，让边界回弹不干扰普通列表滚动。
private fun createArtistDetailPullStretchConnection(
    listState: LazyListState,
    pullStretch: Animatable<Float, AnimationVector1D>,
    bottomBounce: Animatable<Float, AnimationVector1D>,
    maxPullStretchPx: Float,
    maxBottomBouncePx: Float,
    scope: CoroutineScope,
): NestedScrollConnection {
    return object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput) {
                return Offset.Zero
            }
            return consumeArtistDetailBoundaryDrag(
                available = available,
                listState = listState,
                pullStretch = pullStretch,
                bottomBounce = bottomBounce,
                maxPullStretchPx = maxPullStretchPx,
                maxBottomBouncePx = maxBottomBouncePx,
                scope = scope,
            )
        }

        override suspend fun onPreFling(available: Velocity): Velocity =
            resetBoundaryStretchAfterGesture(
                available = available,
                pullStretch = pullStretch,
                bottomBounce = bottomBounce,
            )

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity,
        ): Velocity {
            resetBoundaryStretchAfterGesture(
                available = available,
                pullStretch = pullStretch,
                bottomBounce = bottomBounce,
            )
            return Velocity.Zero
        }
    }
}

// 根据当前滚动位置决定是否消费边界拖拽或释放已有拉伸。
private fun consumeArtistDetailBoundaryDrag(
    available: Offset,
    listState: LazyListState,
    pullStretch: Animatable<Float, AnimationVector1D>,
    bottomBounce: Animatable<Float, AnimationVector1D>,
    maxPullStretchPx: Float,
    maxBottomBouncePx: Float,
    scope: CoroutineScope,
): Offset {
    val dragY: Float = available.y
    if (dragY > 0f && bottomBounce.value > 0f) {
        return releaseBottomBounceByDrag(
            dragY = dragY,
            bottomBounce = bottomBounce,
            scope = scope,
        )
    }
    if (dragY < 0f && pullStretch.value > 0f) {
        return releaseHeaderByDrag(
            dragY = dragY,
            pullStretch = pullStretch,
            scope = scope,
        )
    }
    if (dragY > 0f && !listState.canScrollBackward) {
        return stretchHeaderByDrag(
            dragY = dragY,
            pullStretch = pullStretch,
            maxPullStretchPx = maxPullStretchPx,
            scope = scope,
        )
    }
    if (dragY < 0f && !listState.canScrollForward) {
        return stretchBottomByDrag(
            dragY = dragY,
            bottomBounce = bottomBounce,
            maxBottomBouncePx = maxBottomBouncePx,
            scope = scope,
        )
    }
    return Offset.Zero
}

// 向下拖动时使用阻尼增加头图高度，保留参考视频里的慢放大手感。
private fun stretchHeaderByDrag(
    dragY: Float,
    pullStretch: Animatable<Float, AnimationVector1D>,
    maxPullStretchPx: Float,
    scope: CoroutineScope,
): Offset {
    val previous: Float = pullStretch.value
    val next: Float = (previous + dragY * 0.52f).coerceAtMost(maximumValue = maxPullStretchPx)
    val consumed: Float = if (next == previous) 0f else (next - previous) / 0.52f
    snapBoundaryStretch(
        boundaryStretch = pullStretch,
        value = next,
        scope = scope,
    )
    return Offset(x = 0f, y = consumed)
}

// 向上拖动时先收回头图拉伸，再把剩余手势交还给列表滚动。
private fun releaseHeaderByDrag(
    dragY: Float,
    pullStretch: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
): Offset {
    val previous: Float = pullStretch.value
    val next: Float = (previous + dragY).coerceAtLeast(minimumValue = 0f)
    snapBoundaryStretch(
        boundaryStretch = pullStretch,
        value = next,
        scope = scope,
    )
    return Offset(x = 0f, y = next - previous)
}

// 到达底部后继续上拉时让列表轻微跟手上移，形成底部回弹。
private fun stretchBottomByDrag(
    dragY: Float,
    bottomBounce: Animatable<Float, AnimationVector1D>,
    maxBottomBouncePx: Float,
    scope: CoroutineScope,
): Offset {
    val previous: Float = bottomBounce.value
    val next: Float = (previous + -dragY * 0.52f).coerceAtMost(maximumValue = maxBottomBouncePx)
    val consumedDistance: Float = if (next == previous) 0f else (next - previous) / 0.52f
    snapBoundaryStretch(
        boundaryStretch = bottomBounce,
        value = next,
        scope = scope,
    )
    return Offset(x = 0f, y = -consumedDistance)
}

// 反向拖动时先收回底部回弹，再把剩余手势交还给列表。
private fun releaseBottomBounceByDrag(
    dragY: Float,
    bottomBounce: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
): Offset {
    val previous: Float = bottomBounce.value
    val next: Float = (previous - dragY).coerceAtLeast(minimumValue = 0f)
    snapBoundaryStretch(
        boundaryStretch = bottomBounce,
        value = next,
        scope = scope,
    )
    return Offset(x = 0f, y = previous - next)
}

// 拖动过程需要立即跟手，因此用 snapTo 而不是补间动画。
private fun snapBoundaryStretch(
    boundaryStretch: Animatable<Float, AnimationVector1D>,
    value: Float,
    scope: CoroutineScope,
) {
    scope.launch {
        boundaryStretch.stop()
        boundaryStretch.snapTo(targetValue = value)
    }
}

// 手势结束后把顶部拉伸和底部回弹都复位，避免边界状态残留。
private suspend fun resetBoundaryStretchAfterGesture(
    available: Velocity,
    pullStretch: Animatable<Float, AnimationVector1D>,
    bottomBounce: Animatable<Float, AnimationVector1D>,
): Velocity {
    val shouldConsumeVelocity: Boolean = pullStretch.value > 0f || bottomBounce.value > 0f
    if (!shouldConsumeVelocity) {
        return Velocity.Zero
    }
    resetBoundaryAnimatable(boundaryStretch = pullStretch)
    resetBoundaryAnimatable(
        boundaryStretch = bottomBounce,
        dampingRatio = Spring.DampingRatioMediumBouncy,
    )
    return available
}

// 不同边界可以复用同一段弹回动画，只保留阻尼差异。
private suspend fun resetBoundaryAnimatable(
    boundaryStretch: Animatable<Float, AnimationVector1D>,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
) {
    if (boundaryStretch.value <= 0f) {
        return
    }
    boundaryStretch.animateTo(
        targetValue = 0f,
        animationSpec =
            spring(
                dampingRatio = dampingRatio,
                stiffness = Spring.StiffnessMediumLow,
            ),
    )
}
