package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.PlayingGlyph

/**
 * “我的”页最近播放摘要最多展示最新 3 条，点击和播放反馈复用完整最近播放语义。
 */
private const val RECENT_PLAYED_SUMMARY_PREVIEW_COUNT = 3

/**
 * “我的”页最近播放摘要入口文案，后接右箭头图标并进入完整最近播放页。
 */
private const val RECENT_PLAYED_SUMMARY_ACTION_LABEL = "查看全部"

/**
 * 最近播放摘要展示模型，只接收外部已过滤的最近播放歌曲列表。
 *
 * @property title 区块标题。
 * @property actionLabel 查看完整最近播放页入口文案。
 * @property emptyMessage 空态提示。
 * @property songRows 可见最近播放歌曲行，已附带当前播放标识和更多入口状态。
 * @property isActionEnabled 查看全部入口是否可点击。
 */
internal data class RecentPlayedSummaryDisplayModel(
    val title: String,
    val actionLabel: String,
    val emptyMessage: String,
    val songRows: List<RecentPlayedSongRowDisplayModel>,
    val isActionEnabled: Boolean,
) {
    /**
     * 兼容既有测试和调用方的歌曲列表视图，真实渲染状态以 [songRows] 为准。
     */
    val songs: List<Song>
        get() = songRows.map { row: RecentPlayedSongRowDisplayModel -> row.song }
}

/**
 * 构造“我的”页最近播放摘要，调用方负责传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildRecentPlayedSummaryDisplayModel(
    recentSongs: List<Song>,
    currentSongId: String? = null,
): RecentPlayedSummaryDisplayModel {
    val visibleSongs: List<Song> = recentSongs.take(n = RECENT_PLAYED_SUMMARY_PREVIEW_COUNT)
    return RecentPlayedSummaryDisplayModel(
        title = "最近播放",
        actionLabel = RECENT_PLAYED_SUMMARY_ACTION_LABEL,
        emptyMessage = "播放歌曲后，最近听过的音乐会出现在这里。",
        songRows = buildRecentPlayedSongRowDisplayModels(
            songs = visibleSongs,
            currentSongId = currentSongId,
        ),
        isActionEnabled = true,
    )
}

/**
 * 最近播放摘要按 Figma 使用裸列表，不再放入旧版卡片容器。
 */
@Composable
internal fun MeRecentPlayedSummarySection(
    recentSongs: List<Song>,
    currentSongId: String?,
    onViewAll: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val displayModel: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
        recentSongs = recentSongs,
        currentSongId = currentSongId,
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = meSectionTitleGap)) {
        MeRecentPlayedHeader(
            displayModel = displayModel,
            onViewAll = onViewAll,
        )
        if (displayModel.songs.isEmpty()) {
            MeRecentPlayedEmptyState(message = displayModel.emptyMessage)
        } else {
            MeRecentPlayedSongList(
                songRows = displayModel.songRows,
                onSongPlay = onSongPlay,
                onSongMore = onSongMore,
            )
        }
    }
}

// 标题行右侧只保留 Figma 的“查看全部”文字入口。
@Composable
private fun MeRecentPlayedHeader(
    displayModel: RecentPlayedSummaryDisplayModel,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeSectionTitle(title = displayModel.title)
        Text(
            text = displayModel.actionLabel,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .clickable(
                    enabled = displayModel.isActionEnabled,
                    onClick = onViewAll,
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            color = meAccentDarkColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// 最近播放列表保留 Top3 规则，行距按 Figma 节点 919:481 渲染。
@Composable
private fun MeRecentPlayedSongList(
    songRows: List<RecentPlayedSongRowDisplayModel>,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = meRecentSongGap)) {
        songRows.forEach { row: RecentPlayedSongRowDisplayModel ->
            MeRecentPlayedSongRow(
                row = row,
                onSongPlay = onSongPlay,
                onSongMore = onSongMore,
            )
        }
    }
}

// 单行按 Figma 展示封面、歌曲信息和三点菜单，当前播放态仍遵守全局红色规则。
@Composable
private fun MeRecentPlayedSongRow(
    row: RecentPlayedSongRowDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val song: Song = row.song
    val titleColor: Color = if (row.isCurrentSong) MusicColors.PlayingRed else meTextColor
    val metaColor: Color = if (row.isCurrentSong) MusicColors.PlayingRed else meMetaColor
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(size = 16.dp),
        color = meBackgroundColor,
        border = BorderStroke(width = 1.dp, color = meOutlineColor),
        onClick = { onSongPlay(song) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = meRecentSongPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = song.coverArt,
                coverImageUri = song.coverImageUri,
                contentDescription = "${song.title} 封面",
                modifier = Modifier
                    .size(size = meRecentCoverSize)
                    .clip(shape = RoundedCornerShape(size = meRecentCoverRadius)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
            ) {
                Text(
                    text = song.title,
                    color = titleColor,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MeRecentPlayedSongMeta(
                    row = row,
                    metaColor = metaColor,
                )
            }
            MeRecentPlayedMoreButton(
                row = row,
                onSongMore = onSongMore,
            )
        }
    }
}

// 当前播放辅助标识只在命中全局当前歌曲时出现，普通行保持 Figma 的简洁副标题。
@Composable
private fun MeRecentPlayedSongMeta(
    row: RecentPlayedSongRowDisplayModel,
    metaColor: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.playingIndicatorLabel?.let { label: String ->
            PlayingGlyph(color = MusicColors.PlayingRed)
            Text(
                text = label,
                color = MusicColors.PlayingRed,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Text(
            text = row.song.artist,
            modifier = Modifier.weight(weight = 1f, fill = false),
            color = metaColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 三点按钮继续打开全局单曲更多面板，但视觉尺寸按 Figma 收窄。
@Composable
private fun MeRecentPlayedMoreButton(
    row: RecentPlayedSongRowDisplayModel,
    onSongMore: (Song) -> Unit,
) {
    if (!row.hasMoreAction) {
        return
    }
    Box(
        modifier = Modifier
            .size(size = 40.dp)
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .clickable { onSongMore(row.song) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "${row.song.title} 更多操作",
            modifier = Modifier.size(size = 20.dp),
            tint = meMetaColor,
        )
    }
}

// 空态借用最近播放行的稳定高度，避免无历史时破坏全局播放器避让。
@Composable
private fun MeRecentPlayedEmptyState(
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(shape = RoundedCornerShape(size = 16.dp))
            .background(color = meBackgroundColor)
            .padding(all = meRecentSongPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            color = meMetaColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
