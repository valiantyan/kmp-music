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
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
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
        val horizontalPadding: Dp = desktopPageHorizontalPadding(width = maxWidth)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.Paper)
                .padding(
                    start = horizontalPadding,
                    top = DesktopMusicDimens.PagePaddingTop,
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
