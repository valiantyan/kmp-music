package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader

/**
 * 空路由占位只渲染页面标题，避免二级路由为空时复用任意业务页面。
 */
@Composable
fun DesktopEmptyStateScreen(
    title: String,
    subtitle: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        DesktopPageHeader(
            title = title,
            eyebrow = subtitle,
        )
    }
}
