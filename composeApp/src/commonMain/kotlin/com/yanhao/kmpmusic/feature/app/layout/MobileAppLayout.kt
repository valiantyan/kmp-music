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
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform

/**
 * 手机端 App 外层布局，统一承接宽度、背景、缩放、内容、固定底栏和全局 surfaces。
 */
@Composable
fun MobileAppLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(color = MusicColors.PageBackground),
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
                    .background(color = MaterialTheme.colorScheme.background),
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
                    discoveryPlatform = discoveryPlatform,
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
                    integratesBottomNavigationInset = discoveryPlatform == LocalMusicDiscoveryPlatform.Ios,
                )
                MobileChromeOverlay(
                    state = state,
                    controller = controller,
                    discoveryPlatform = discoveryPlatform,
                    onScanLocalMusic = onScanLocalMusic,
                    modifier = Modifier.fillMaxSize(),
                )
                AppDialogs(state = state, controller = controller)
                AppPanels(state = state, controller = controller)
            }
        }
    }
}
