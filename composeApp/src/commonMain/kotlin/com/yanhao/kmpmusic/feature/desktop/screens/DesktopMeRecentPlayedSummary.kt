package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage

/** Figma 网格在内容区同时露出四首真实最近播放歌曲。 */
private const val RECENT_PLAYED_SUMMARY_COUNT = 4

/** 桌面最近播放摘要的显示模型，只消费控制器已过滤的真实歌曲列表。 */
internal data class DesktopMeRecentPlayedSummaryDisplayModel(
    val title: String,
    val actionLabel: String,
    val isActionEnabled: Boolean,
    val rows: List<DesktopMeRecentPlayedSongDisplayModel>,
    val emptyMessage: String,
)

/** 桌面最近播放卡显示模型，保留播放、当前项和更多操作的既有语义。 */
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

/** 从统一的最近播放列表构造卡片，避免回退到样例曲目或未过滤历史。 */
internal fun buildDesktopMeRecentPlayedSummaryDisplayModel(
    recentSongs: List<Song>,
    currentSongId: String? = null,
    isPlaying: Boolean = true,
): DesktopMeRecentPlayedSummaryDisplayModel {
    val rows: List<DesktopMeRecentPlayedSongDisplayModel> =
        recentSongs.take(n = RECENT_PLAYED_SUMMARY_COUNT).map { song: Song ->
            val isCurrentSong: Boolean = isPlaying && song.id == currentSongId
            DesktopMeRecentPlayedSongDisplayModel(
                song = song,
                title = song.title,
                subtitle = song.artist,
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

/** 用 Figma 的四列封面网格呈现最近播放，并保留完整列表入口。 */
@Composable
internal fun DesktopMeRecentPlayedSummary(
    recentSongs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onViewAll: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayModel: DesktopMeRecentPlayedSummaryDisplayModel =
        buildDesktopMeRecentPlayedSummaryDisplayModel(
            recentSongs = recentSongs,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Text(
                text = displayModel.title,
                modifier = Modifier.weight(1f),
                color = DesktopMeFigmaTokens.Muted.copy(alpha = 0.4f),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
            androidx.compose.material3.Text(
                text = displayModel.actionLabel,
                modifier =
                    if (displayModel.isActionEnabled) {
                        Modifier.clickable(onClick = onViewAll)
                    } else {
                        Modifier
                    },
                color = DesktopMeFigmaTokens.Accent,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (displayModel.rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(194.dp), contentAlignment = Alignment.Center) {
                DesktopSectionEmptyMessage(message = displayModel.emptyMessage)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                displayModel.rows.forEach { row: DesktopMeRecentPlayedSongDisplayModel ->
                    DesktopMeRecentPlayedCard(
                        row = row,
                        onSongPlay = onSongPlay,
                        onSongMore = onSongMore,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
