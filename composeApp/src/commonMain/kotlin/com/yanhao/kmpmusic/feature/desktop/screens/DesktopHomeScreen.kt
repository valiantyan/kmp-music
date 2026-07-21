package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.components.DesktopStatCard

/**
 * 桌面首页按新版 Figma 首页渲染，顶部搜索和标题固定，只有歌曲列表独立滚动。
 */
@Composable
fun DesktopLocalMusicRootScreen(
    songs: List<Song>,
    songCount: Int,
    scanState: LocalMusicScanState,
    currentSongId: String?,
    isPlaying: Boolean,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF9F9FF)),
    ) {
        DesktopHomeSearchBar(onSearch = onSearch)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 32.dp, end = 24.dp),
        ) {
            DesktopHomeTitle(songCount = songCount)
            Spacer(modifier = Modifier.height(24.dp))
            if (songs.isEmpty()) {
                DesktopHomeEmptyState(
                    scanState = scanState,
                    onScan = onScan,
                    modifier = Modifier.weight(1f),
                )
            } else {
                DesktopHomeSongList(
                    songs = songs,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 三列统计行在桌面首页与收藏页保持相同权重，避免页面切换时卡片宽度跳动。
 */
@Composable
internal fun DesktopThreeStatRow(
    firstTitle: String,
    firstValue: String,
    secondTitle: String,
    secondValue: String,
    thirdTitle: String,
    thirdValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DesktopStatCard(
            icon = "♫",
            title = firstTitle,
            value = firstValue,
            modifier = Modifier.weight(1f),
        )
        DesktopStatCard(
            icon = "●",
            title = secondTitle,
            value = secondValue,
            modifier = Modifier.weight(1f),
        )
        DesktopStatCard(
            icon = "♟",
            title = thirdTitle,
            value = thirdValue,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 根页面播放全部按钮根据当前队列归属显示可预期的动作文案。 */
internal fun rootPlayAllLabel(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
): String {
    val containsCurrentSong: Boolean = songs.any { song: Song -> song.id == currentSongId }
    if (!containsCurrentSong) {
        return "播放全部"
    }
    return when (currentPlaybackStatus) {
        PlaybackStatus.Playing,
        PlaybackStatus.Buffering,
        PlaybackStatus.Loading,
        -> "暂停播放"

        PlaybackStatus.Paused,
        PlaybackStatus.Ended,
        PlaybackStatus.Idle,
        PlaybackStatus.Error,
        -> "继续播放"
    }
}

/** 根页面播放全部按钮优先切换当前队列，未命中时从列表首曲开始播放。 */
internal fun playOrToggleRootCollection(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
) {
    val containsCurrentSong: Boolean = songs.any { song: Song -> song.id == currentSongId }
    if (containsCurrentSong && currentPlaybackStatus != PlaybackStatus.Error) {
        onCurrentSongToggle()
        return
    }
    songs.firstOrNull()?.let { song: Song -> onSongPlay(song, songs) }
}
