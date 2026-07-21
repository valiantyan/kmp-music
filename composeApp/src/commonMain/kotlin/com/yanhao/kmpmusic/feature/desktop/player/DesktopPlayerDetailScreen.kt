package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 桌面沉浸式播放页，进入后接管整个窗口内容区，避免与底部播放器重复呈现控制。
 */
@Composable
fun DesktopPlayerDetailScreen(
    song: Song?,
    queueSongs: List<Song>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    volume: Float,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onQueueIndexClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette: PlayerPagePalette = rememberDesktopPlayerPagePalette(song = song)
    Box(
        modifier = modifier
            .background(palette.backgroundColor)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.ambientColor.copy(alpha = if (song == null) 0.22f else 0.48f),
                        palette.backgroundColor,
                    ),
                    center = Offset(x = 320f, y = 260f),
                    radius = 980f,
                ),
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.58f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 72.dp, top = 28.dp, end = 72.dp, bottom = 48.dp),
        ) {
            DesktopPlayerTopBar(onBack = onBack)
            Spacer(modifier = Modifier.height(42.dp))
            if (song == null) {
                DesktopPlayerEmptyState()
            } else {
                DesktopPlayerContent(
                    song = song,
                    queueRows = buildPlayerQueueRowStates(
                        queueSongs = queueSongs,
                    ),
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    playbackDurationMs = playbackDurationMs,
                    playbackMode = playbackMode,
                    volume = volume,
                    onToggle = onToggle,
                    onPrev = onPrev,
                    onNext = onNext,
                    onMode = onMode,
                    onLike = onLike,
                    onSeek = onSeek,
                    onVolumeChange = onVolumeChange,
                    onQueueIndexClick = onQueueIndexClick,
                )
            }
        }
    }
}
