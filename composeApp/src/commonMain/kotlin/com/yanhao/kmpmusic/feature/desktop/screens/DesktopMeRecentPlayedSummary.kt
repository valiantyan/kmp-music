package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
 */
internal data class DesktopMeRecentPlayedSongDisplayModel(
    val song: Song,
    val title: String,
    val subtitle: String,
    val duration: String,
)

/**
 * 构造桌面最近播放摘要；调用方必须传入统一过滤后的最近播放歌曲列表。
 */
internal fun buildDesktopMeRecentPlayedSummaryDisplayModel(
    recentSongs: List<Song>,
): DesktopMeRecentPlayedSummaryDisplayModel {
    val rows: List<DesktopMeRecentPlayedSongDisplayModel> = recentSongs
        .take(n = RECENT_PLAYED_SUMMARY_COUNT)
        .map { song: Song ->
            DesktopMeRecentPlayedSongDisplayModel(
                song = song,
                title = song.title,
                subtitle = "${song.artist} · ${song.album}",
                duration = song.duration,
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
 * 桌面最近播放摘要只做展示和完整页入口，歌曲点击和更多菜单留给后续动作反馈切片。
 */
@Composable
internal fun DesktopMeRecentPlayedSummary(
    recentSongs: List<Song>,
    onViewAll: () -> Unit,
) {
    val displayModel: DesktopMeRecentPlayedSummaryDisplayModel =
        buildDesktopMeRecentPlayedSummaryDisplayModel(recentSongs = recentSongs)
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
                    DesktopMeRecentPlayedSongRow(row = row)
                }
            }
        }
    }
}

/**
 * 桌面摘要歌曲行保留封面、标题、歌手专辑和时长，不提前接入播放动作。
 */
@Composable
private fun DesktopMeRecentPlayedSongRow(
    row: DesktopMeRecentPlayedSongDisplayModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArtImage(
            coverArt = row.song.coverArt,
            coverImageUri = row.song.coverImageUri,
            contentDescription = "${row.title} 封面",
            modifier = Modifier
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
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.StatTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.subtitle,
                color = DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = row.duration,
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
