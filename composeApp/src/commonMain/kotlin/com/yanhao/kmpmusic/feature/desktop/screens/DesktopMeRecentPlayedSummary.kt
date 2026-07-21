package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader

/**
 * 桌面“我的”页最近播放摘要只露出最新三首，完整列表由二级页承载。
 */
private const val RECENT_PLAYED_SUMMARY_COUNT = 3

/**
 * 桌面“我的”页最近播放摘要展示模型，只消费已统一过滤的最近播放歌曲列表。
 *
 * @property title 区块标题。
 * @property actionLabel 进入完整最近播放页的入口文案。
 * @property isActionEnabled 是否启用查看全部入口。
 * @property rows 摘要歌曲行。
 * @property emptyMessage 空列表时的轻量提示。
 */
internal data class DesktopMeRecentPlayedSummaryDisplayModel(
    val title: String,
    val actionLabel: String,
    val isActionEnabled: Boolean,
    val rows: List<DesktopMeRecentPlayedSongDisplayModel>,
    val emptyMessage: String,
)

/**
 * 桌面“我的”页最近播放摘要歌曲行展示模型。
 *
 * @property song 统一过滤后的歌曲实体。
 * @property title 歌曲标题。
 * @property subtitle 歌手和专辑组合文案。
 * @property duration 歌曲时长。
 * @property isCurrentSong 是否为全局当前播放歌曲。
 * @property playingIndicatorLabel 当前播放辅助标识文案。
 * @property hasPlaybackAction 是否允许行点击播放。
 * @property hasMoreAction 是否显示单曲更多入口。
 */
internal data class DesktopMeRecentPlayedSongDisplayModel(
    val song: Song,
    val title: String,
    val subtitle: String,
    val duration: String,
    val isCurrentSong: Boolean,
    val playingIndicatorLabel: String?,
    val hasPlaybackAction: Boolean,
    val hasMoreAction: Boolean,
)

/**
 * 构造桌面最近播放摘要；调用方必须传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildDesktopMeRecentPlayedSummaryDisplayModel(
    recentSongs: List<Song>,
    currentSongId: String? = null,
): DesktopMeRecentPlayedSummaryDisplayModel {
    val rows: List<DesktopMeRecentPlayedSongDisplayModel> =
        recentSongs
            .take(n = RECENT_PLAYED_SUMMARY_COUNT)
            .map { song: Song ->
                val isCurrentSong: Boolean = song.id == currentSongId
                DesktopMeRecentPlayedSongDisplayModel(
                    song = song,
                    title = song.title,
                    subtitle = "${song.artist} · ${song.album}",
                    duration = song.duration,
                    isCurrentSong = isCurrentSong,
                    playingIndicatorLabel = if (isCurrentSong) "播放中" else null,
                    hasPlaybackAction = true,
                    hasMoreAction = true,
                )
            }
    return DesktopMeRecentPlayedSummaryDisplayModel(
        title = "最近播放",
        actionLabel = "查看全部",
        isActionEnabled = true,
        rows = rows,
        emptyMessage = "播放歌曲后会显示最近听过的音乐。",
    )
}

/**
 * 桌面最近播放摘要行复用最近播放专用播放入口，避免 Top3 摘要截断播放队列。
 */
@Composable
internal fun DesktopMeRecentPlayedSummary(
    recentSongs: List<Song>,
    currentSongId: String?,
    onViewAll: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val displayModel: DesktopMeRecentPlayedSummaryDisplayModel =
        buildDesktopMeRecentPlayedSummaryDisplayModel(
            recentSongs = recentSongs,
            currentSongId = currentSongId,
        )
    DesktopSectionHeader(
        title = displayModel.title,
        actionLabel = displayModel.actionLabel,
        onAction = if (displayModel.isActionEnabled) onViewAll else null,
    )
    Spacer(modifier = Modifier.height(14.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (displayModel.rows.isEmpty()) {
                DesktopSectionEmptyMessage(message = displayModel.emptyMessage)
            } else {
                displayModel.rows.forEach { row: DesktopMeRecentPlayedSongDisplayModel ->
                    DesktopMeRecentPlayedSongRow(
                        row = row,
                        onSongPlay = onSongPlay,
                        onSongMore = onSongMore,
                    )
                }
            }
        }
    }
}

/**
 * 桌面摘要歌曲行展示播放反馈，并把行点击和更多入口交回全局控制器。
 */
@Composable
private fun DesktopMeRecentPlayedSongRow(
    row: DesktopMeRecentPlayedSongDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val titleColor: Color = if (row.isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable(enabled = row.hasPlaybackAction) { onSongPlay(row.song) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArtImage(
            coverArt = row.song.coverArt,
            coverImageUri = row.song.coverImageUri,
            contentDescription = "${row.title} 封面",
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = row.title,
                color = titleColor,
                fontSize = DesktopMusicType.StatTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DesktopMeRecentPlayedSongMeta(row = row)
        }
        DesktopMeRecentPlayedSongActions(
            row = row,
            onSongMore = onSongMore,
        )
    }
}

/**
 * 当前播放行在副信息里附加“播放中”，避免只依赖标题颜色表达状态。
 */
@Composable
private fun DesktopMeRecentPlayedSongMeta(
    row: DesktopMeRecentPlayedSongDisplayModel,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.playingIndicatorLabel?.let { label: String ->
            Text(
                text = label,
                color = DesktopMusicColors.PlayerRed,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
        Text(
            text = row.subtitle,
            modifier = Modifier.weight(weight = 1f, fill = false),
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 摘要歌曲行尾只放时长和三点按钮，标题栏查看全部入口不复用这里的动作区。
 */
@Composable
private fun DesktopMeRecentPlayedSongActions(
    row: DesktopMeRecentPlayedSongDisplayModel,
    onSongMore: (Song) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.duration,
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (row.hasMoreAction) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(9.dp),
                color = Color.Transparent,
                onClick = { onSongMore(row.song) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "•••",
                        color = Color(0xFF475364),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
