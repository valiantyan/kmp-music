package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.shouldShowPauseControl
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 桌面歌曲表格复用统一表头和行高，保证首页与收藏页切换时视觉稳定。
 */
@Composable
fun DesktopSongTable(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DesktopSongTableHeader(
            showFavoriteColumn = showFavoriteColumn,
            trailingDateLabel = trailingDateLabel,
        )
        songs.forEachIndexed { index: Int, song: Song ->
            DesktopSongTableRow(
                index = index,
                song = song,
                songs = songs,
                isCurrentSong = song.id == currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                showFavoriteColumn = showFavoriteColumn,
                trailingDateLabel = trailingDateLabel,
                onSongPlay = onSongPlay,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
                onLike = onLike,
            )
        }
    }
}

/**
 * 表头列宽与内容列权重需要固定，避免不同数据集造成表格整体抖动。
 */
@Composable
private fun DesktopSongTableHeader(
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFavoriteColumn) {
            Text(text = "", modifier = Modifier.width(36.dp))
        }
        Text(
            text = "#",
            modifier = Modifier.width(42.dp),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "标题",
            modifier = Modifier
                .weight(2.4f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "歌手",
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "专辑",
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "时长",
            modifier = Modifier.width(72.dp),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = trailingDateLabel,
            modifier = Modifier.width(98.dp),
            color = Color(0xFF7D8795),
            fontSize = DesktopMusicType.TableHeader,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = "", modifier = Modifier.width(40.dp))
    }
}

/**
 * 行内只消费控制器传入的歌曲和动作，避免桌面表格偷偷持有额外播放状态。
 */
@Composable
private fun DesktopSongTableRow(
    index: Int,
    song: Song,
    songs: List<Song>,
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
    showFavoriteColumn: Boolean,
    trailingDateLabel: String,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)?,
) {
    val isCurrentSongPlaying: Boolean =
        isCurrentSong && currentPlaybackStatus.shouldShowPauseControl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopMusicDimens.TableRowHeight)
            .background(if (isCurrentSong) DesktopMusicColors.Accent.copy(alpha = 0.10f) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFavoriteColumn) {
            Text(
                text = if (song.isLiked) "♥" else "♡",
                modifier = Modifier
                    .width(36.dp)
                    .clickable { onLike?.invoke(song.id) },
                color = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Muted,
            )
        }
        Surface(
            modifier = Modifier.width(42.dp),
            color = Color.Transparent,
            onClick = {
                if (isCurrentSong) {
                    onCurrentSongToggle()
                } else {
                    onSongPlay(song, songs)
                }
            },
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isCurrentSongPlaying) "Ⅱ" else if (isCurrentSong) "▶" else (index + 1).toString(),
                    color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Muted,
                    fontSize = DesktopMusicType.Body,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(2.4f)
                .padding(end = DesktopMusicDimens.TableColumnGap)
                .clickable { onSongPlay(song, songs) },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = song.coverArt,
                coverImageUri = song.coverImageUri,
                contentDescription = "${song.title} 封面",
                modifier = Modifier
                    .size(DesktopMusicDimens.TableCoverSize)
                    .clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = song.title,
                modifier = Modifier.weight(1f),
                color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.artist,
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.TableTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.album,
            modifier = Modifier
                .weight(1.2f)
                .padding(end = DesktopMusicDimens.TableColumnGap),
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.TableTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.duration,
            modifier = Modifier.width(72.dp),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
        )
        Text(
            text = desktopSongTableTrailingValue(
                trailingDateLabel = trailingDateLabel,
                song = song,
            ),
            modifier = Modifier.width(98.dp),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(9.dp),
            color = Color.Transparent,
            onClick = { onMore(song) },
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

private fun desktopSongTableTrailingValue(
    trailingDateLabel: String,
    song: Song,
): String {
    return when (trailingDateLabel) {
        "收藏时间" -> "已收藏"
        "添加时间" -> song.modifiedAt?.let(::formatDesktopModifiedDate) ?: "最近添加"
        else -> "最近"
    }
}

private fun formatDesktopModifiedDate(timestampMillis: Long): String {
    val epochDay: Long = floorDivByDay(timestampMillis)
    val civilDate: CivilDate = civilDateFromEpochDay(epochDay)
    val month: String = civilDate.month.toString().padStart(length = 2, padChar = '0')
    val day: String = civilDate.day.toString().padStart(length = 2, padChar = '0')
    return "${civilDate.year}-$month-$day"
}

private fun floorDivByDay(timestampMillis: Long): Long {
    val dayMillis: Long = 86_400_000L
    return if (timestampMillis >= 0L) {
        timestampMillis / dayMillis
    } else {
        ((timestampMillis + 1L) / dayMillis) - 1L
    }
}

private fun civilDateFromEpochDay(epochDay: Long): CivilDate {
    val shiftedDay: Long = epochDay + 719_468L
    val era: Long = if (shiftedDay >= 0L) shiftedDay else shiftedDay - 146_096L
    val eraIndex: Long = era / 146_097L
    val dayOfEra: Long = shiftedDay - eraIndex * 146_097L
    val yearOfEra: Long = (
        dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L
        ) / 365L
    val year: Long = yearOfEra + eraIndex * 400L
    val dayOfYear: Long = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime: Long = (5L * dayOfYear + 2L) / 153L
    val day: Int = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month: Int = (monthPrime + if (monthPrime < 10L) 3L else -9L).toInt()
    val resolvedYear: Int = (year + if (month <= 2) 1L else 0L).toInt()
    return CivilDate(
        year = resolvedYear,
        month = month,
        day = day,
    )
}

private data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)
