package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Song

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
    contentPadding: PaddingValues = PaddingValues(),
) {
    val displayModel: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(
        songs = songs,
        currentSongId = currentSongId,
    )
    Column(
        modifier = modifier
            .background(color = favoritesBackgroundColor)
            .padding(bottom = contentPadding.calculateBottomPadding() + 40.dp),
    ) {
        RecentPlayedTopAppBar(onBack = onBack)
        if (displayModel.songs.isEmpty()) {
            RecentPlayedPageEmptyState(displayModel = displayModel)
            return@Column
        }
        displayModel.songRows.forEach { row: RecentPlayedSongRowDisplayModel ->
            RecentPlayedPageSongRow(
                row = row,
                queueSongs = displayModel.songs,
                onSongPlay = onSongPlay,
                onSongMore = onSongMore,
            )
            Spacer(modifier = Modifier.height(height = favoritesSongRowGap))
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

// 二级页工具栏采用移动端官方导航栏节奏，避免大标题挤占最近播放列表首屏。
@Composable
private fun RecentPlayedTopAppBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(size = 40.dp),
            shape = CircleShape,
            color = MusicColors.Soft.copy(alpha = 0.94f),
            onClick = onBack,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(size = 20.dp),
                    tint = favoritesTitleColor,
                )
            }
        }
        Spacer(modifier = Modifier.width(width = 16.dp))
        Text(
            text = "最近播放",
            color = favoritesTitleColor,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 空态继续使用轻量文案，避免没有历史时出现白屏或误导成加载失败。
@Composable
private fun RecentPlayedPageEmptyState(displayModel: RecentPlayedPageDisplayModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = favoritesHorizontalPadding, vertical = 24.dp),
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

// 歌曲行复用首页本地音乐样式，只隐藏收藏并把时长放到歌手后面。
@Composable
private fun RecentPlayedPageSongRow(
    row: RecentPlayedSongRowDisplayModel,
    queueSongs: List<Song>,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    HomeSongRow(
        song = row.song,
        isCurrentSong = row.isCurrentSong,
        queueSongs = queueSongs,
        metaText = formatRecentPlayedSongMeta(song = row.song),
        onSongPlay = { selectedSong: Song, _: List<Song> -> onSongPlay(selectedSong) },
        onMore = if (row.hasMoreAction) onSongMore else null,
    )
}

// 最近播放页不展示专辑名，时长直接跟在歌手后面便于列表扫读。
private fun formatRecentPlayedSongMeta(song: Song): String {
    return "${song.artist} · ${song.duration}"
}
