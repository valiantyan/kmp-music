package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.AppHeader

/**
 * 移动端最近播放页骨架，完整歌曲列表会在后续切片接入。
 */
@Composable
fun RecentPlayedScreen(
    songs: List<Song>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayModel: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(songs = songs)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
    ) {
        AppHeader(
            title = "最近播放",
            subtitle = "按最近播放倒序展示",
            onBack = onBack,
        )
        RecentPlayedPageMessage(displayModel = displayModel)
    }
}

/**
 * 最近播放页展示模型，隔离空态文案并避免本切片提前渲染完整列表。
 */
internal data class RecentPlayedPageDisplayModel(
    val title: String,
    val detail: String,
)

/**
 * 构造最近播放页当前切片的展示文案，后续列表接入时可以保留空态分支。
 */
internal fun buildRecentPlayedPageDisplayModel(songs: List<Song>): RecentPlayedPageDisplayModel {
    if (songs.isEmpty()) {
        return RecentPlayedPageDisplayModel(
            title = "暂无最近播放",
            detail = "播放歌曲后才会产生最近播放记录。",
        )
    }
    return RecentPlayedPageDisplayModel(
        title = "最近播放记录已准备好",
        detail = "完整最近播放列表将在后续切片接入。",
    )
}

// 页面消息保持轻量，避免把最近播放页做成完整列表或日志管理页。
@Composable
private fun RecentPlayedPageMessage(displayModel: RecentPlayedPageDisplayModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = displayModel.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = scaledSp(value = 18.sp),
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = displayModel.detail,
            color = MusicColors.Muted,
            fontSize = scaledSp(value = 15.sp),
            lineHeight = scaledSp(value = 22.sp),
            fontWeight = FontWeight.Medium,
        )
    }
}
