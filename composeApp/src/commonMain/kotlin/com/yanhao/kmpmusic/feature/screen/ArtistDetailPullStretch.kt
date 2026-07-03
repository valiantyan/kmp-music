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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 歌手详情页顶部下拉放大状态。
 *
 * @property pullStretchHeight 当前下拉放大高度。
 * @property nestedScrollConnection 只在列表回到顶部后消费下拉手势。
 */
internal data class ArtistDetailPullStretchState(
    val pullStretchHeight: Dp,
    val nestedScrollConnection: NestedScrollConnection,
)

/**
 * 记住歌手详情页下拉放大状态，避免把 overscroll 逻辑散进页面结构。
 */
@Composable
internal fun rememberArtistDetailPullStretchState(
    listState: LazyListState,
    maxPullStretchHeight: Dp,
): ArtistDetailPullStretchState {
    val density = LocalDensity.current
    val maxPullStretchPx: Float = with(density) { maxPullStretchHeight.toPx() }
    val pullStretch: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) }
    val scope: CoroutineScope = rememberCoroutineScope()
    val nestedScrollConnection: NestedScrollConnection = remember(
        listState,
        maxPullStretchPx,
        pullStretch,
        scope,
    ) {
        createArtistDetailPullStretchConnection(
            listState = listState,
            pullStretch = pullStretch,
            maxPullStretchPx = maxPullStretchPx,
            scope = scope,
        )
    }
    return ArtistDetailPullStretchState(
        pullStretchHeight = with(density) { pullStretch.value.toDp() },
        nestedScrollConnection = nestedScrollConnection,
    )
}

// 创建 NestedScroll 连接，让顶部下拉只影响头图，不干扰普通列表滚动。
private fun createArtistDetailPullStretchConnection(
    listState: LazyListState,
    pullStretch: Animatable<Float, AnimationVector1D>,
    maxPullStretchPx: Float,
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
            return consumePullStretchDrag(
                available = available,
                listState = listState,
                pullStretch = pullStretch,
                maxPullStretchPx = maxPullStretchPx,
                scope = scope,
            )
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return resetPullStretchAfterGesture(
                available = available,
                pullStretch = pullStretch,
            )
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            resetPullStretchAfterGesture(
                available = available,
                pullStretch = pullStretch,
            )
            return Velocity.Zero
        }
    }
}

// 根据当前滚动位置决定是否消费下拉或释放已有拉伸。
private fun consumePullStretchDrag(
    available: Offset,
    listState: LazyListState,
    pullStretch: Animatable<Float, AnimationVector1D>,
    maxPullStretchPx: Float,
    scope: CoroutineScope,
): Offset {
    val dragY: Float = available.y
    if (dragY > 0f && !listState.canScrollBackward) {
        return stretchHeaderByDrag(
            dragY = dragY,
            pullStretch = pullStretch,
            maxPullStretchPx = maxPullStretchPx,
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
    snapPullStretch(
        pullStretch = pullStretch,
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
    snapPullStretch(
        pullStretch = pullStretch,
        value = next,
        scope = scope,
    )
    return Offset(x = 0f, y = next - previous)
}

// 拖动过程需要立即跟手，因此用 snapTo 而不是补间动画。
private fun snapPullStretch(
    pullStretch: Animatable<Float, AnimationVector1D>,
    value: Float,
    scope: CoroutineScope,
) {
    scope.launch {
        pullStretch.stop()
        pullStretch.snapTo(targetValue = value)
    }
}

// 手势结束后弹回展开高度，形成自然回弹。
private suspend fun resetPullStretchAfterGesture(
    available: Velocity,
    pullStretch: Animatable<Float, AnimationVector1D>,
): Velocity {
    if (pullStretch.value <= 0f) {
        return Velocity.Zero
    }
    pullStretch.animateTo(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )
    return available
}
