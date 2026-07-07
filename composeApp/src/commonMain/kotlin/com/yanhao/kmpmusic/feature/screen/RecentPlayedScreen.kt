package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 移动端最近播放页只消费统一过滤后的最近播放歌曲列表，不自行扫描曲库或回退 demo 数据。
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
        if (displayModel.songs.isEmpty()) {
            RecentPlayedPageEmptyState(displayModel = displayModel)
        } else {
            RecentPlayedPageSongList(songs = displayModel.songs)
        }
    }
}

/**
 * 最近播放页展示模型保留完整歌曲列表，空态文案只在列表为空时使用。
 */
internal data class RecentPlayedPageDisplayModel(
    val songs: List<Song>,
    val emptyTitle: String,
    val emptyDetail: String,
)

/**
 * 构造最近播放页展示数据，调用方负责传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildRecentPlayedPageDisplayModel(songs: List<Song>): RecentPlayedPageDisplayModel {
    return RecentPlayedPageDisplayModel(
        songs = songs,
        emptyTitle = "暂无最近播放",
        emptyDetail = "播放歌曲后才会产生最近播放记录。",
    )
}

// 空态继续使用轻量文案，避免没有历史时出现白屏或误导成加载失败。
@Composable
private fun RecentPlayedPageEmptyState(displayModel: RecentPlayedPageDisplayModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = displayModel.emptyTitle,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = scaledSp(value = 18.sp),
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = displayModel.emptyDetail,
            color = MusicColors.Muted,
            fontSize = scaledSp(value = 15.sp),
            lineHeight = scaledSp(value = 22.sp),
            fontWeight = FontWeight.Medium,
        )
    }
}

// 列表直接展示完整入参，避免复用“我的”页摘要 Top3 的截断规则。
@Composable
private fun RecentPlayedPageSongList(songs: List<Song>) {
    Surface(
        shape = RoundedCornerShape(size = 20.dp),
        color = MusicColors.Paper,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(all = 18.dp),
            verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        ) {
            songs.forEach { song: Song ->
                RecentPlayedPageSongRow(song = song)
            }
        }
    }
}

// 歌曲行只展示信息，不在本切片提前接入点击播放、高亮或更多菜单。
@Composable
private fun RecentPlayedPageSongRow(song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier
                .size(size = 48.dp)
                .clip(shape = RoundedCornerShape(size = 12.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 3.dp),
        ) {
            Text(
                text = song.title,
                color = MusicColors.Ink,
                fontSize = scaledSp(value = 15.sp),
                lineHeight = scaledSp(value = 19.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                color = MusicColors.Muted,
                fontSize = scaledSp(value = 13.sp),
                lineHeight = scaledSp(value = 17.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.duration,
            color = MusicColors.Muted,
            fontSize = scaledSp(value = 12.sp),
            lineHeight = scaledSp(value = 16.sp),
            maxLines = 1,
        )
    }
}
