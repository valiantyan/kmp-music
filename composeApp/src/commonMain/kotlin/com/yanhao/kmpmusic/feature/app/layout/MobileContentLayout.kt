package com.yanhao.kmpmusic.feature.app.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.feature.app.ContentBottomSpace
import com.yanhao.kmpmusic.feature.app.MobileFixedBarMode
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.routes.MobileRootScreenRoute
import com.yanhao.kmpmusic.feature.app.routes.MobileSecondaryScreenRoute

/**
 * 根据导航状态渲染手机端页面内容，并为固定底栏预留共享底部空间。
 */
@Composable
fun MobileContentLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    fixedBarMode: MobileFixedBarMode,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomPadding: Dp = getContentBottomPadding(contentBottomSpace = fixedBarMode.contentBottomSpace)
    val pagePadding: PaddingValues = PaddingValues(
        start = scaledDp(MusicDimens.PagePaddingHorizontal),
        top = scaledDp(MusicDimens.PagePaddingTop),
        end = scaledDp(MusicDimens.PagePaddingHorizontal),
        bottom = bottomPadding,
    )
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
        val secondaryScreen: SecondaryScreen? = state.navigationState.secondaryScreen
        val isHomeRoot: Boolean = secondaryScreen == null && state.navigationState.rootTab == RootTab.Home
        if (
            secondaryScreen is SecondaryScreen.LocalMusic ||
            secondaryScreen == SecondaryScreen.ArtistDetail ||
            secondaryScreen == SecondaryScreen.Player
        ) {
            MobileSecondaryScreenRoute(
                secondaryScreen = secondaryScreen,
                state = state,
                controller = controller,
                onScanLocalMusic = onScanLocalMusic,
                modifier = modifier.fillMaxSize(),
                contentPadding = pagePadding,
            )
        } else if (isHomeRoot) {
            MobileRootScreenRoute(
                state = state,
                controller = controller,
                onScanLocalMusic = onScanLocalMusic,
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = bottomPadding),
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(pagePadding),
            ) {
                if (secondaryScreen == null) {
                    MobileRootScreenRoute(
                        state = state,
                        controller = controller,
                        onScanLocalMusic = onScanLocalMusic,
                    )
                } else {
                    MobileSecondaryScreenRoute(
                        secondaryScreen = secondaryScreen,
                        state = state,
                        controller = controller,
                        onScanLocalMusic = onScanLocalMusic,
                        contentPadding = pagePadding,
                    )
                }
            }
        }
    }
}

/**
 * 根据固定底栏策略计算页面底部避让空间，避免隐藏播放器后留下空白。
 */
@Composable
private fun getContentBottomPadding(contentBottomSpace: ContentBottomSpace): Dp {
    return when (contentBottomSpace) {
        ContentBottomSpace.TopLevel -> scaledDp(MusicDimens.TopLevelContentBottom)
        ContentBottomSpace.SecondaryWithMiniPlayer -> scaledDp(MusicDimens.SecondaryContentBottom)
        ContentBottomSpace.Fullscreen -> scaledDp(MusicDimens.FullscreenContentBottom)
    }
}
