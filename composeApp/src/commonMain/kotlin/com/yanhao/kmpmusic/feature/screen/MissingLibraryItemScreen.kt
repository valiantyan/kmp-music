package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.feature.components.AppHeader

/**
 * 曲库条目缺失时的二级页兜底，避免空库状态崩溃。
 */
@Composable
fun MissingLibraryItemScreen(
    title: String,
    subtitle: String = "重新扫描后再试",
    message: String = "当前曲库中找不到这个条目。",
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 20.dp)) {
        AppHeader(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
        )
        Text(
            text = message,
            color = MusicColors.Muted,
        )
    }
}
