package com.yanhao.kmpmusic.feature.app.playerbar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MobileFixedBarPlacement
import com.yanhao.kmpmusic.feature.app.RootTab

/**
 * 普通底部固定栏切换时长(300ms)，用于一级页和仅迷你播放器页面之间的轻量移动。
 */
private const val MOBILE_FIXED_BAR_PARTIAL_TRANSITION_MILLIS = 300

/**
 * 完全隐藏或恢复底部固定栏的切换时长(500ms)，避免沉浸页面转场显得过快。
 */
private const val MOBILE_FIXED_BAR_HIDDEN_TRANSITION_MILLIS = 500

/**
 * 全局手机端固定底栏容器，迷你播放器与 Tab 保持相对位置并整体移动。
 */
@Composable
fun MobileFixedPlayerBar(
    song: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    placement: MobileFixedBarPlacement,
    showsBottomNavigation: Boolean,
    rootTab: RootTab,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationBarHeight: Dp = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(density = this).toDp()
    }
    val stackHeight: Dp = if (song == null) {
        scaledDp(MusicDimens.BottomNavHeight)
    } else {
        scaledDp(MusicDimens.MiniPlayerHeight + MusicDimens.BottomNavHeight)
    }
    val fixedBarTransition = updateTransition(
        targetState = placement,
        label = "MobileFixedBarPlacement",
    )
    val stackOffset: Dp by fixedBarTransition.animateDp(
        transitionSpec = {
            val durationMillis: Int = if (
                initialState == MobileFixedBarPlacement.Hidden ||
                    targetState == MobileFixedBarPlacement.Hidden
            ) {
                MOBILE_FIXED_BAR_HIDDEN_TRANSITION_MILLIS
            } else {
                MOBILE_FIXED_BAR_PARTIAL_TRANSITION_MILLIS
            }
            tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        },
        label = "MobileFixedBarOffset",
    ) { targetPlacement: MobileFixedBarPlacement ->
        when (targetPlacement) {
            MobileFixedBarPlacement.TopLevel -> 0.dp
            MobileFixedBarPlacement.MiniPlayerOnly -> if (song == null) {
                stackHeight + navigationBarHeight
            } else {
                scaledDp(MusicDimens.BottomNavHeight)
            }
            MobileFixedBarPlacement.Hidden -> stackHeight + navigationBarHeight
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(stackHeight + navigationBarHeight),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(navigationBarHeight)
                .background(MusicColors.Paper),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stackHeight)
                .clipToBounds()
                .align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stackHeight)
                    .offset(y = stackOffset),
            ) {
                if (song != null) {
                    MobileMiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        playbackPositionMs = playbackPositionMs,
                        playbackDurationMs = playbackDurationMs,
                        onOpen = onOpen,
                        onToggle = onToggle,
                        onPrev = onPrev,
                        onQueue = onQueue,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
                MobileBottomNavigation(
                    rootTab = rootTab,
                    isEnabled = showsBottomNavigation,
                    onRootTab = onRootTab,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
