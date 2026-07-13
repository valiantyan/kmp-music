package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPage
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPageSubtitle

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

/**
 * 普通二级页的曲库缺失态，复用固定 Toolbar 并让说明内容独立滚动。
 */
@Composable
fun StandardMissingLibraryItemScreen(
    title: String,
    subtitle: String = "重新扫描后再试",
    message: String = "当前曲库中找不到这个条目。",
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    MobileSecondaryPage(
        title = title,
        onBack = onBack,
        backgroundColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(weight = 1f)
                .verticalScroll(state = rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        ) {
            MobileSecondaryPageSubtitle(text = subtitle)
            Text(
                text = message,
                color = MusicColors.Muted,
            )
        }
    }
}
