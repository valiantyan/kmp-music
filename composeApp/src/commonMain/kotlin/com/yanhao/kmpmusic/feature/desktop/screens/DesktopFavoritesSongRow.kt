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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

/** 歌曲行沿用本地音乐页的整行播放切换规则，收藏和更多动作独立消费点击。 */
@Composable
internal fun DesktopFavoritesSongRow(
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
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = if (hasTopSpacing) 8.dp else 0.dp)
                .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentSong) Color(0x0D006B5C) else Color.Transparent,
        onClick = {
            if (shouldToggleDesktopHomeSongCardPlayback(isCurrentSong = isCurrentSong)) {
                onCurrentSongToggle()
            } else {
                onSongPlay(song, songs)
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopFavoritesSongIndex(index = index, isCurrentSong = isCurrentSong, isPlaying = isPlaying)
            DesktopFavoritesSongTitle(song = song, isCurrentSong = isCurrentSong, modifier = Modifier.weight(1f))
            Text(
                text = song.artist,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                color = resolveFavoritesRowTextColor(isCurrentSong = isCurrentSong),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.duration,
                modifier = Modifier.width(100.dp),
                color = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0x993C4A46),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            DesktopFavoritesIconAction(
                icon = resolveDesktopFavoritesLikeIcon(isLiked = song.isLiked),
                contentDescription = "取消收藏 ${song.title}",
                onClick = { onLike(song.id) },
            )
            DesktopFavoritesIconAction(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = "${song.title} 更多操作",
                onClick = { onMore(song) },
            )
        }
    }
}

/** 当前歌曲播放时在序号位显示本地页同款等高器，暂停时保留红色序号。 */
@Composable
private fun DesktopFavoritesSongIndex(
    index: Int,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
) {
    Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        if (isCurrentSong && isPlaying) {
            DesktopHomePlayingIndicator(modifier = Modifier.size(width = 20.dp, height = 16.dp))
            return@Box
        }
        Text(
            text = (index + 1).toString(),
            color = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0x663C4A46),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** 标题列固定 40dp 封面和 16dp 间距，长标题只在自身列内省略。 */
@Composable
private fun DesktopFavoritesSongTitle(
    song: Song,
    isCurrentSong: Boolean,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = song.title,
            modifier = Modifier.weight(1f),
            color = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0xFF111C2D),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 行尾动作统一占据 48dp 槽位，避免收藏状态变化移动表格列。 */
@Composable
private fun DesktopFavoritesIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(48.dp),
        shape = CircleShape,
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF008F7B),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 收藏歌曲使用实心图标，状态变化时仍保留完整的收藏语义映射。 */
internal fun resolveDesktopFavoritesLikeIcon(isLiked: Boolean): ImageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder

/** 当前歌曲沿用全局播放红，普通艺术家文本使用 Figma 的深灰绿。 */
private fun resolveFavoritesRowTextColor(isCurrentSong: Boolean): Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0xFF3C4A46)
