package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song

// 歌手详情页歌曲行只用文字颜色表达播放态，不显示等化器或封面动画。
@Composable
internal fun ArtistDetailSongRow(
    rowState: ArtistDetailSongRowState,
    artistSongs: List<Song>,
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(size = 16.dp),
        color = rowState.containerColor,
        onClick = {
            when (
                resolveArtistDetailSongClickAction(
                    isCurrentSong = isCurrentSong,
                    currentPlaybackStatus = currentPlaybackStatus,
                )
            ) {
                ArtistDetailSongClickAction.ToggleCurrentPlayback -> onCurrentSongToggle()
                ArtistDetailSongClickAction.PlaySong -> onSongPlay(rowState.song, artistSongs)
            }
        },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rowState.indexLabel,
                color = rowState.indexColor,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(width = 32.dp),
            )
            ArtistDetailSongText(
                rowState = rowState,
                modifier = Modifier.weight(weight = 1f),
            )
            ArtistDetailSongActions(
                song = rowState.song,
                onLike = onLike,
                onMore = onMore,
            )
        }
    }
}

// 歌名和歌手时长复用行状态颜色，播放中同步变红。
@Composable
private fun ArtistDetailSongText(
    rowState: ArtistDetailSongRowState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Text(
            text = rowState.song.title,
            color = rowState.titleColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${rowState.song.artist} · ${rowState.song.duration}",
            color = rowState.metaColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 操作区保留收藏和更多入口，避免详情页丢失已有歌曲操作能力。
@Composable
private fun ArtistDetailSongActions(
    song: Song,
    onLike: (String) -> Unit,
    onMore: (Song) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(size = 36.dp),
            onClick = { onLike(song.id) },
        ) {
            Icon(
                imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (song.isLiked) "取消收藏" else "收藏",
                tint = if (song.isLiked) MusicColors.Accent else artistDetailMetaColor,
            )
        }
        IconButton(
            modifier = Modifier.size(size = 32.dp),
            onClick = { onMore(song) },
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "${song.title} 更多操作",
                tint = artistDetailMetaColor,
            )
        }
    }
}
