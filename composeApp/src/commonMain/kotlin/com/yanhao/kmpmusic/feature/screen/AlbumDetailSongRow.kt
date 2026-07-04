package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.PlayingGlyph

// 专辑详情页歌曲行圆角，固定尺寸避免当前播放状态切换时行高跳动。
private val albumDetailSongRowShape: RoundedCornerShape = RoundedCornerShape(size = 16.dp)

/**
 * 专辑详情页歌曲行，按 Figma 展示曲序、歌曲信息、时长和更多入口。
 */
@Composable
internal fun AlbumDetailSongRow(
    rowState: AlbumDetailSongRowState,
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = albumDetailSongRowShape,
        color = Color.Transparent,
        onClick = {
            when (
                resolveAlbumDetailSongClickAction(
                    isCurrentSong = isCurrentSong,
                    currentPlaybackStatus = currentPlaybackStatus,
                )
            ) {
                AlbumDetailSongClickAction.ToggleCurrentPlayback -> onCurrentSongToggle()
                AlbumDetailSongClickAction.PlaySong -> onSongPlay(rowState.song)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumDetailSongIndex(rowState = rowState)
            AlbumDetailSongText(
                rowState = rowState,
                modifier = Modifier.weight(weight = 1f),
            )
            AlbumDetailSongActions(
                rowState = rowState,
                onMore = onMore,
            )
        }
    }
}

// 当前播放行用辅助标识替代序号，普通行保持两位曲序。
@Composable
private fun AlbumDetailSongIndex(rowState: AlbumDetailSongRowState) {
    Box(
        modifier = Modifier.width(width = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rowState.showsPlaybackGlyph) {
            PlayingGlyph(color = MusicColors.PlayingRed)
        } else {
            Text(
                text = rowState.indexLabel,
                color = rowState.indexColor,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

// 歌名和专辑元信息复用行状态颜色，当前歌曲同步变红。
@Composable
private fun AlbumDetailSongText(
    rowState: AlbumDetailSongRowState,
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
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${rowState.song.artist} · ${rowState.song.album}",
            color = rowState.metaColor.copy(alpha = 0.80f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 行尾只保留 Figma 中的时长和更多入口，收藏继续从更多面板进入。
@Composable
private fun AlbumDetailSongActions(
    rowState: AlbumDetailSongRowState,
    onMore: (Song) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rowState.song.duration,
            color = rowState.durationColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        IconButton(
            modifier = Modifier.size(size = 32.dp),
            onClick = { onMore(rowState.song) },
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "${rowState.song.title} 更多操作",
                tint = if (rowState.showsPlaybackGlyph) MusicColors.PlayingRed else albumDetailMutedColor,
                modifier = Modifier.size(size = 18.dp),
            )
        }
    }
}
