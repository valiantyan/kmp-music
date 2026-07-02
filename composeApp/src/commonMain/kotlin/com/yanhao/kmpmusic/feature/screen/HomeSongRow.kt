package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 歌曲行保持 82dp 高度和 350dp 内容宽度节奏，对齐 Figma 列表项。
@Composable
internal fun HomeSongRow(
    song: Song,
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
    queueSongs: List<Song>,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val rowStyle: HomeSongRowStyle = resolveHomeSongRowStyle(
        isCurrentSong = isCurrentSong,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = rowStyle.containerColor,
        border = rowStyle.border,
        shadowElevation = rowStyle.shadowElevation,
        onClick = { onSongPlay(song, queueSongs) },
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeSongCover(
                song = song,
            )
            HomeSongText(
                song = song,
                textColor = rowStyle.textColor,
                modifier = Modifier.weight(weight = 1f),
            )
            HomeSongMoreButton(
                song = song,
                onMore = onMore,
            )
        }
    }
}

// 封面保留 56dp 正方形和 12dp 圆角，播放状态改由文字颜色表达。
@Composable
private fun HomeSongCover(
    song: Song,
) {
    Box(modifier = Modifier.size(56.dp)) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

// 歌曲标题和歌手名共享同一颜色，播放中由 [HomeSongRowStyle] 切到红色。
@Composable
private fun HomeSongText(
    song: Song,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = song.title,
            color = textColor,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 更多按钮延续全局歌曲操作入口，但尺寸按 Figma 压缩到 40dp 宽。
@Composable
private fun HomeSongMoreButton(
    song: Song,
    onMore: (Song) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 38.dp)
            .clip(CircleShape)
            .clickable { onMore(song) },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "${song.title} 更多操作",
            modifier = Modifier.size(width = 20.dp, height = 20.dp),
            tint = homeMutedColor,
        )
    }
}
