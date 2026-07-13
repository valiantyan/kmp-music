package com.yanhao.kmpmusic.feature.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledSp

/**
 * 标准二级页壳层，统一处理系统安全区并让正文独立于固定 Toolbar 滚动。
 */
@Composable
fun MobileSecondaryPage(
    title: String,
    onBack: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MobileSecondaryToolbar(
            title = title,
            onBack = onBack,
        )
        content()
    }
}

/**
 * 二级页原有副标题迁移到正文后的统一文字样式。
 */
@Composable
fun MobileSecondaryPageSubtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MusicColors.Muted,
        fontSize = scaledSp(value = 16.sp),
        lineHeight = scaledSp(value = 22.sp),
    )
}
