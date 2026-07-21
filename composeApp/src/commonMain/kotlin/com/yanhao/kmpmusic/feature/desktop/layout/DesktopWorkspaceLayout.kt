package com.yanhao.kmpmusic.feature.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.desktopPageHorizontalPadding
import com.yanhao.kmpmusic.feature.desktop.navigation.DesktopRootScreenRoute
import com.yanhao.kmpmusic.feature.desktop.navigation.DesktopSecondaryScreenRoute

/**
 * 桌面工作区统一承接一级页和二级页，并在非播放器页面保持可保存状态隔离。
 */
@Composable
fun DesktopWorkspaceLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    saveableStateHolder: SaveableStateHolder,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val usesHomeFigmaLayout: Boolean = state.usesDesktopHomeFigmaLayout()
        val horizontalPadding: Dp =
            if (usesHomeFigmaLayout) {
                0.dp
            } else {
                desktopPageHorizontalPadding(width = maxWidth)
            }
        val topPadding: Dp =
            if (usesHomeFigmaLayout) {
                0.dp
            } else {
                DesktopMusicDimens.PagePaddingTop
            }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(DesktopMusicColors.Paper)
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                    ),
        ) {
            saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
                if (state.navigationState.secondaryScreen == null) {
                    DesktopRootScreenRoute(
                        state = state,
                        controller = controller,
                        onScanLocalMusic = onScanLocalMusic,
                    )
                } else {
                    DesktopSecondaryScreenRoute(
                        state = state,
                        controller = controller,
                        onScanLocalMusic = onScanLocalMusic,
                    )
                }
            }
        }
    }
}

/** 首页新版设计自己控制固定搜索栏和标题区，外层工作区不再额外加旧 padding。 */
private fun MusicAppUiState.usesDesktopHomeFigmaLayout(): Boolean = navigationState.secondaryScreen == null && navigationState.rootTab == RootTab.Home
