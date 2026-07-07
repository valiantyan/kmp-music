package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.yanhao.kmpmusic.feature.components.PlayingGlyph

/**
 * 移动端最近播放页只消费统一过滤后的最近播放歌曲列表，不自行扫描曲库或回退 demo 数据。
 */
@Composable
fun RecentPlayedScreen(
    songs: List<Song>,
    currentSongId: String?,
    onBack: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayModel: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(
        songs = songs,
        currentSongId = currentSongId,
    )
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
            RecentPlayedPageSongList(
                songRows = displayModel.songRows,
                onSongPlay = onSongPlay,
                onSongMore = onSongMore,
            )
        }
    }
}

/**
 * 最近播放页展示模型保留完整歌曲列表，空态文案只在列表为空时使用。
 *
 * @property songRows 完整最近播放歌曲行，已附带当前播放标识和更多入口状态。
 * @property emptyTitle 空态标题。
 * @property emptyDetail 空态说明。
 */
internal data class RecentPlayedPageDisplayModel(
    val songRows: List<RecentPlayedSongRowDisplayModel>,
    val emptyTitle: String,
    val emptyDetail: String,
) {
    /**
     * 兼容既有测试和调用方的歌曲列表视图，真实渲染状态以 [songRows] 为准。
     */
    val songs: List<Song>
        get() = songRows.map { row: RecentPlayedSongRowDisplayModel -> row.song }
}

/**
 * 构造最近播放页展示数据，调用方负责传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildRecentPlayedPageDisplayModel(
    songs: List<Song>,
    currentSongId: String? = null,
): RecentPlayedPageDisplayModel {
    return RecentPlayedPageDisplayModel(
        songRows = buildRecentPlayedSongRowDisplayModels(
            songs = songs,
            currentSongId = currentSongId,
        ),
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

// 列表直接展示完整入参，播放队列选择继续交给控制器统一处理。
@Composable
private fun RecentPlayedPageSongList(
    songRows: List<RecentPlayedSongRowDisplayModel>,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(size = 20.dp),
        color = MusicColors.Paper,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(all = 18.dp),
            verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        ) {
            songRows.forEach { row: RecentPlayedSongRowDisplayModel ->
                RecentPlayedPageSongRow(
                    row = row,
                    onSongPlay = onSongPlay,
                    onSongMore = onSongMore,
                )
            }
        }
    }
}

// 歌曲行接入播放点击、当前播放反馈和既有全局更多面板入口。
@Composable
private fun RecentPlayedPageSongRow(
    row: RecentPlayedSongRowDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val song: Song = row.song
    val titleColor: Color = if (row.isCurrentSong) MusicColors.PlayingRed else MusicColors.Ink
    val metaColor: Color = if (row.isCurrentSong) MusicColors.PlayingRed else MusicColors.Muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable { onSongPlay(song) },
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
                color = titleColor,
                fontSize = scaledSp(value = 15.sp),
                lineHeight = scaledSp(value = 19.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.playingIndicatorLabel?.let { label: String ->
                    PlayingGlyph(color = MusicColors.PlayingRed)
                    Text(
                        text = label,
                        color = MusicColors.PlayingRed,
                        fontSize = scaledSp(value = 12.sp),
                        lineHeight = scaledSp(value = 15.sp),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "${song.artist} · ${song.album}",
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    color = metaColor,
                    fontSize = scaledSp(value = 13.sp),
                    lineHeight = scaledSp(value = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        RecentPlayedPageSongActions(
            row = row,
            metaColor = metaColor,
            onSongMore = onSongMore,
        )
    }
}

// 行尾操作区只属于歌曲行，不用于标题栏的“查看全部”入口。
@Composable
private fun RecentPlayedPageSongActions(
    row: RecentPlayedSongRowDisplayModel,
    metaColor: Color,
    onSongMore: (Song) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.song.duration,
            color = metaColor,
            fontSize = scaledSp(value = 12.sp),
            lineHeight = scaledSp(value = 16.sp),
            maxLines = 1,
        )
        if (row.hasMoreAction) {
            IconButton(
                modifier = Modifier.size(size = 36.dp),
                onClick = { onSongMore(row.song) },
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "${row.song.title} 更多操作",
                    modifier = Modifier.size(size = 20.dp),
                    tint = MusicColors.Muted,
                )
            }
        }
    }
}
