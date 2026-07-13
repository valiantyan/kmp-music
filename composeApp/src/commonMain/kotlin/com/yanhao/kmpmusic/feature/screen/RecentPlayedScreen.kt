package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPage

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
    MobileSecondaryPage(
        title = "最近播放",
        onBack = onBack,
        backgroundColor = favoritesBackgroundColor,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(weight = 1f),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 40.dp,
            ),
        ) {
            if (displayModel.songs.isEmpty()) {
                item(key = "recent-played-empty", contentType = "recent-played-empty") {
                    RecentPlayedPageEmptyState(displayModel = displayModel)
                }
                return@LazyColumn
            }
            items(
                items = displayModel.songRows,
                key = { row: RecentPlayedSongRowDisplayModel -> row.song.id },
                contentType = { "recent-played-song" },
            ) { row: RecentPlayedSongRowDisplayModel ->
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
