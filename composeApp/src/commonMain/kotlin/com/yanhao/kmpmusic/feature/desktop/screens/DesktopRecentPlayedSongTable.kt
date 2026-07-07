package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 完整列表使用桌面表格密度，行点击和更多入口都回调给外层控制器。
 */
@Composable
internal fun DesktopRecentPlayedSongTable(
    rows: List<DesktopRecentPlayedSongDisplayModel>,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(
            width = 1.dp,
            color = DesktopMusicColors.Line,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            DesktopRecentPlayedTableHeader()
            rows.forEach { row: DesktopRecentPlayedSongDisplayModel ->
                DesktopRecentPlayedSongRow(
                    row = row,
                    onSongPlay = onSongPlay,
                    onSongMore = onSongMore,
                )
            }
        }
    }
}

/**
 * 表头列宽沿用桌面歌曲表格权重，保证 workspace 宽屏阅读密度稳定。
 */
@Composable
private fun DesktopRecentPlayedTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopRecentPlayedHeaderText(
            text = "#",
            modifier = Modifier.width(42.dp),
        )
        DesktopRecentPlayedHeaderText(
            text = "标题",
            modifier = Modifier
                .weight(2.4f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
        )
        DesktopRecentPlayedHeaderText(
            text = "歌手",
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
        )
        DesktopRecentPlayedHeaderText(
            text = "专辑",
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
        )
        DesktopRecentPlayedHeaderText(
            text = "时长",
            modifier = Modifier.width(72.dp),
        )
        Text(text = "", modifier = Modifier.width(40.dp))
    }
}

/**
 * 表头文本统一样式，减少列定义重复。
 */
@Composable
private fun DesktopRecentPlayedHeaderText(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = Color(0xFF7D8795),
        fontSize = DesktopMusicType.TableHeader,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * 行内容接入播放、高亮和更多菜单，但不提供任何历史管理动作。
 */
@Composable
private fun DesktopRecentPlayedSongRow(
    row: DesktopRecentPlayedSongDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val titleColor: Color = if (row.isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink
    val indexLabel: String = if (row.isCurrentSong) "▶" else row.indexLabel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableRowHeight)
            .background(if (row.isCurrentSong) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(enabled = row.hasPlaybackAction) { onSongPlay(row.song) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = indexLabel,
            modifier = Modifier.width(42.dp),
            color = if (row.isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            fontWeight = if (row.isCurrentSong) FontWeight.Bold else FontWeight.Normal,
        )
        Row(
            modifier = Modifier
                .weight(2.4f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = row.song.coverArt,
                coverImageUri = row.song.coverImageUri,
                contentDescription = "${row.title} 封面",
                modifier = Modifier
                    .size(DesktopMusicDimens.TableCoverSize)
                    .clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.title,
                    modifier = Modifier.weight(1f),
                    color = titleColor,
                    fontSize = DesktopMusicType.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.playingIndicatorLabel?.let { label: String ->
                    Text(
                        text = label,
                        color = DesktopMusicColors.PlayerRed,
                        fontSize = DesktopMusicType.TableHeader,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
            }
        }
        DesktopRecentPlayedCellText(
            text = row.artist,
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
        )
        DesktopRecentPlayedCellText(
            text = row.album,
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
        )
        DesktopRecentPlayedCellText(
            text = row.duration,
            modifier = Modifier.width(72.dp),
        )
        DesktopRecentPlayedMoreButton(
            row = row,
            onSongMore = onSongMore,
        )
    }
}

/**
 * 行尾三点按钮只打开既有单曲更多面板，不承载最近播放管理能力。
 */
@Composable
private fun DesktopRecentPlayedMoreButton(
    row: DesktopRecentPlayedSongDisplayModel,
    onSongMore: (Song) -> Unit,
) {
    if (!row.hasMoreAction) {
        Text(text = "", modifier = Modifier.width(40.dp))
        return
    }
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

/**
 * 普通单元格统一截断规则，避免长歌手或专辑名撑破表格。
 */
@Composable
private fun DesktopRecentPlayedCellText(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = DesktopMusicColors.Ink,
        fontSize = DesktopMusicType.TableTitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
