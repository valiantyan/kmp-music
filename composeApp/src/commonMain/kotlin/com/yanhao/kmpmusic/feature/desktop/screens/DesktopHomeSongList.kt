package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAutoHideLazyScrollbar

// 歌曲列表独立滚动，保留顶部固定搜索与标题区域。
@Composable
internal fun DesktopHomeSongList(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            itemsIndexed(
                items = songs,
                key = { _: Int, song: Song -> song.id },
            ) { index: Int, song: Song ->
                DesktopHomeSongCard(
                    index = index,
                    song = song,
                    songs = songs,
                    isCurrentSong = song.id == currentSongId,
                    isPlaying = isPlaying,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                )
            }
        }
        DesktopAutoHideLazyScrollbar(
            listState = listState,
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
        )
    }
}

// 首页卡片把整行点击、收藏、更多和时长拆开，避免动作点击误触发播放。
@Composable
private fun DesktopHomeSongCard(
    index: Int,
    song: Song,
    songs: List<Song>,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val visualSpec: DesktopHomeSongCardVisualSpec =
        resolveDesktopHomeSongCardVisualSpec(
            isCurrentSong = isCurrentSong,
            isEvenRow = index % 2 == 0,
        )
    val isCurrentSongPlaying: Boolean =
        shouldShowDesktopHomePlayingIndicator(
            isCurrentSong = isCurrentSong,
            isPlaying = isPlaying,
        )
    val textColor: Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0xFF111C2D)
    val metaColor: Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0xFF3C4A46)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(82.dp),
        shape = RoundedCornerShape(16.dp),
        color = visualSpec.cardColor,
        border = BorderStroke(width = 1.dp, color = visualSpec.cardBorderColor),
        shadowElevation = visualSpec.cardShadowElevation,
        onClick = {
            if (shouldToggleDesktopHomeSongCardPlayback(isCurrentSong = isCurrentSong)) {
                onCurrentSongToggle()
            } else {
                onSongPlay(song, songs)
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(all = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopHomeSongArtwork(
                song = song,
                isCurrentSong = isCurrentSong,
                visualSpec = visualSpec,
            )
            DesktopHomeSongText(
                song = song,
                textColor = textColor,
                metaColor = metaColor,
                isCurrentSong = isCurrentSong,
                modifier = Modifier.weight(1f),
            )
            DesktopHomeSongActions(
                song = song,
                isCurrentSong = isCurrentSong,
                isCurrentSongPlaying = isCurrentSongPlaying,
                visualSpec = visualSpec,
                onMore = onMore,
                onLike = onLike,
            )
            Text(
                text = song.duration,
                modifier = Modifier.width(48.dp),
                color = metaColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
            )
        }
    }
}

// 首页卡片当前歌曲点击始终交给全局播放切换，符合整行点击暂停/继续的约定。
internal fun shouldToggleDesktopHomeSongCardPlayback(isCurrentSong: Boolean): Boolean = isCurrentSong

// 播放动画只跟随当前歌曲的真实播放态，暂停时保留固定位置但不跳动。
internal fun shouldShowDesktopHomePlayingIndicator(
    isCurrentSong: Boolean,
    isPlaying: Boolean,
): Boolean = isCurrentSong && isPlaying
