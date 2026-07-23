package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopAlbumDetailTokens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

/** 专辑曲目表头和歌曲行共享固定时长、操作列，防止标题与时长因歌曲名称变化而错位。 */
@Composable
internal fun DesktopAlbumDetailTableHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(41.dp)
                .drawBehind {
                    drawLine(
                        color = DesktopAlbumDetailTokens.Line,
                        start = Offset(x = 0f, y = size.height),
                        end = Offset(x = size.width, y = size.height),
                    )
                }.padding(horizontal = DesktopAlbumDetailTokens.ContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopAlbumDetailTableHeaderText(
            text = "编号",
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center,
        )
        DesktopAlbumDetailTableHeaderText(
            text = "标题",
            modifier = Modifier.weight(1f),
        )
        DesktopAlbumDetailTableHeaderText(
            text = "艺术家",
            modifier = Modifier.weight(1f),
        )
        DesktopAlbumDetailTableHeaderText(
            text = "时长",
            modifier = Modifier.width(DesktopAlbumDetailTokens.DurationColumnWidth),
            textAlign = TextAlign.End,
        )
        Box(modifier = Modifier.width(DesktopAlbumDetailTokens.ActionColumnWidth))
    }
}

/** 表头文本使用同一颜色和行高，单独保留对齐参数以映射每个固定列。 */
@Composable
private fun DesktopAlbumDetailTableHeaderText(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = DesktopAlbumDetailTokens.MutedText,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
        maxLines = 1,
    )
}

/** 专辑歌曲行复用首页的播放、收藏和更多动作，同时按 Figma 保持无缩略图四列表格。 */
@Composable
internal fun DesktopAlbumDetailSongRow(
    index: Int,
    song: Song,
    songs: List<Song>,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    hasTopSpacing: Boolean,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val textColor: Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopAlbumDetailTokens.Title
    val metaColor: Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopAlbumDetailTokens.SupportingText
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = if (hasTopSpacing) 8.dp else 0.dp)
                .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentSong) DesktopAlbumDetailTokens.ActiveRow else Color.Transparent,
        onClick = {
            if (shouldToggleDesktopHomeSongCardPlayback(isCurrentSong = isCurrentSong)) {
                onCurrentSongToggle()
            } else {
                onSongPlay(song, songs)
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesktopAlbumDetailTokens.ContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopAlbumDetailTrackIndex(
                index = index,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
            )
            Text(
                text = song.title,
                modifier = Modifier.weight(1f),
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                modifier = Modifier.weight(1f),
                color = metaColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.duration,
                modifier = Modifier.width(DesktopAlbumDetailTokens.DurationColumnWidth),
                color = metaColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.width(DesktopAlbumDetailTokens.ActionColumnWidth),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopAlbumDetailIconAction(
                    icon = resolveDesktopFavoritesLikeIcon(isLiked = song.isLiked),
                    contentDescription = if (song.isLiked) "取消收藏 ${song.title}" else "收藏 ${song.title}",
                    onClick = { onLike(song.id) },
                )
                DesktopAlbumDetailIconAction(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = "${song.title} 更多操作",
                    onClick = { onMore(song) },
                )
            }
        }
    }
}

/** 当前播放曲目在编号槽位复用首页等高器，暂停时保留红色两位编号。 */
@Composable
private fun DesktopAlbumDetailTrackIndex(
    index: Int,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
) {
    Box(
        modifier = Modifier.width(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (shouldShowDesktopHomePlayingIndicator(isCurrentSong = isCurrentSong, isPlaying = isPlaying)) {
            DesktopHomePlayingIndicator(modifier = Modifier.size(width = 20.dp, height = 16.dp))
            return@Box
        }
        Text(
            text = (index + 1).toString().padStart(length = 2, padChar = '0'),
            color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopAlbumDetailTokens.MutedText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** 行尾动作固定占据 40dp 点击区，保证收藏状态切换不影响时长列位置。 */
@Composable
private fun DesktopAlbumDetailIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = DesktopAlbumDetailTokens.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
