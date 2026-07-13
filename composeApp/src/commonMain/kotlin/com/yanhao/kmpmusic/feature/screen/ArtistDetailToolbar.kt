package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.yanhao.kmpmusic.feature.components.MobileSecondaryToolbar

// 固定 Toolbar 始终覆盖状态栏区域，背景和标题随折叠进度渐显。
@Composable
internal fun ArtistDetailToolbar(
    artistName: String,
    scrollState: State<ArtistDetailScrollState>,
    collapsedToolbarHeight: Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height = collapsedToolbarHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = scrollState.value.toolbarAlpha
                }
                .background(color = artistDetailToolbarColor),
        )
        MobileSecondaryToolbar(
            title = artistName,
            onBack = onBack,
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .fillMaxWidth(),
            backgroundAlpha = 0f,
            titleAlpha = scrollState.value.toolbarTitleAlpha,
        )
    }
}
