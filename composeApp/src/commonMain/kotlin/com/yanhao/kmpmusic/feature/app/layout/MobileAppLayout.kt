package com.yanhao.kmpmusic.feature.app.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.yanhao.kmpmusic.core.theme.LocalMusicScale
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.feature.app.MobileFixedBarMode
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.playerbar.MobileFixedPlayerBar
import com.yanhao.kmpmusic.feature.app.surfaces.AppDialogs
import com.yanhao.kmpmusic.feature.app.surfaces.AppPanels

/**
 * 手机端 App 外层布局，统一承接宽度、背景、缩放、内容、固定底栏和全局 surfaces。
 */
@Composable
fun MobileAppLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEFF3F5))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(MusicColors.Accent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(x = 120f, y = 110f),
                    radius = 520f,
                ),
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        val appWidth: Dp = if (maxWidth < MusicDimens.AppMaxWidth) {
            maxWidth
        } else {
            MusicDimens.AppMaxWidth
        }
        val visualScale: Float = (appWidth.value / MusicDimens.AppMaxWidth.value).coerceAtMost(maximumValue = 1f)
        val currentDensity: Density = LocalDensity.current
        CompositionLocalProvider(
            LocalMusicScale provides visualScale,
            LocalDensity provides Density(
                density = currentDensity.density,
                fontScale = 1f,
            ),
        ) {
            Box(
                modifier = Modifier
                    .width(appWidth)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(MusicColors.Accent.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(x = 135f * visualScale, y = 130f * visualScale),
                            radius = 420f * visualScale,
                        ),
                    )
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
            ) {
                val fixedBarMode: MobileFixedBarMode = state.navigationState.chromeUnderlayFixedBarMode
                val hasChromeOverlay: Boolean = state.navigationState.chromeOverlayScreen != null
                val underlayModifier: Modifier = if (hasChromeOverlay) {
                    Modifier.clearAndSetSemantics {}
                } else {
                    Modifier
                }
                MobileContentLayout(
                    state = state,
                    controller = controller,
                    fixedBarMode = fixedBarMode,
                    onScanLocalMusic = onScanLocalMusic,
                    modifier = underlayModifier,
                )
                MobileFixedPlayerBar(
                    song = state.currentSong,
                    isPlaying = state.shouldShowPauseControl,
                    playbackPositionMs = state.playbackPositionMs,
                    playbackDurationMs = state.playbackDurationMs,
                    placement = fixedBarMode.fixedBarPlacement,
                    showsBottomNavigation = fixedBarMode.showsBottomNavigation,
                    rootTab = state.navigationState.rootTab,
                    onOpen = controller::openPlayer,
                    onToggle = controller::togglePlayback,
                    onPrev = { controller.moveTrack(direction = -1) },
                    onQueue = controller::openQueue,
                    onRootTab = controller::navigateToRoot,
                    modifier = underlayModifier.align(Alignment.BottomCenter),
                )
                MobileChromeOverlay(
                    state = state,
                    controller = controller,
                    onScanLocalMusic = onScanLocalMusic,
                    modifier = Modifier.fillMaxSize(),
                )
                AppDialogs(state = state, controller = controller)
                AppPanels(state = state, controller = controller)
            }
        }
    }
}
