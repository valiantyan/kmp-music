package com.yanhao.kmpmusic.feature.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.rememberPlayerPagePalette

/**
 * 播放页，按当前歌曲封面生成全屏沉浸背景，并承载完整播放控制。
 */
@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    playbackError: PlaybackError?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration: Long = playbackDurationMs ?: song.durationMs ?: 0L
    val safeProgress: Float = calculatePlaybackProgress(
        positionMs = playbackPositionMs,
        durationMs = duration,
    )
    val palette: PlayerPagePalette = rememberPlayerPagePalette(
        coverArt = song.coverArt,
        coverImageUri = song.coverImageUri,
    )
    val backgroundColor: Color by animateColorAsState(
        targetValue = palette.backgroundColor,
        animationSpec = tween(durationMillis = PLAYER_SCREEN_COLOR_ANIMATION_MILLIS),
        label = "PlayerScreenBackgroundColor",
    )
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val coverSize: Dp = calculatePlayerCoverSize(
            viewportWidth = maxWidth,
            viewportHeight = maxHeight,
        )
        val topSpacerHeight: Dp = minOf(
            scaledDp(74.dp),
            maxHeight * 0.08f,
        )
        val sectionSpacerHeight: Dp = minOf(
            scaledDp(32.dp),
            maxHeight * 0.035f,
        )
        val controlSpacerHeight: Dp = minOf(
            scaledDp(28.dp),
            maxHeight * 0.03f,
        )
        PlayerBackground(
            song = song,
            palette = palette,
            backgroundColor = backgroundColor,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = scaledDp(32.dp),
                    top = scaledDp(12.dp),
                    end = scaledDp(32.dp),
                    bottom = scaledDp(28.dp),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerTopBar(onBack = onBack)
            Spacer(modifier = Modifier.height(height = topSpacerHeight))
            PlayerCoverArt(
                song = song,
                coverSize = coverSize,
            )
            Spacer(modifier = Modifier.height(height = sectionSpacerHeight))
            PlayerMetadata(
                song = song,
                playbackError = playbackError,
            )
            Spacer(modifier = Modifier.height(height = sectionSpacerHeight))
            PlayerProgress(
                value = safeProgress,
                durationMs = duration,
                playbackPositionMs = playbackPositionMs,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(height = controlSpacerHeight))
            PlayerControlRow(
                isPlaying = isPlaying,
                playbackMode = playbackMode,
                onToggle = onToggle,
                onPrev = onPrev,
                onNext = onNext,
                onMode = onMode,
                onQueue = onQueue,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(weight = 1f))
            PlayerAuxiliaryActions(
                song = song,
                onLike = onLike,
            )
        }
    }
}

// 播放页颜色切换跟随封面异步取色，保持切歌时的轻量过渡。
private const val PLAYER_SCREEN_COLOR_ANIMATION_MILLIS = 260

// 进度条数值需要在未知时长和异常进度下保持可渲染。
private fun calculatePlaybackProgress(
    positionMs: Long,
    durationMs: Long,
): Float {
    if (durationMs <= 0L) {
        return 0f
    }
    return positionMs.coerceIn(
        minimumValue = 0L,
        maximumValue = durationMs,
    ).toFloat()
}

// 封面尺寸跟随视口收敛，防止小屏把控制区挤出屏幕。
@Composable
private fun calculatePlayerCoverSize(
    viewportWidth: Dp,
    viewportHeight: Dp,
): Dp {
    val targetSize: Dp = minOf(
        viewportWidth - scaledDp(96.dp),
        viewportHeight * 0.32f,
        scaledDp(292.dp),
    )
    return targetSize.coerceAtLeast(minimumValue = scaledDp(220.dp))
}

// 播放时间统一按 mm:ss 输出，避免未知时长和拖动进度出现负值文本。
internal fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds: Long = (positionMs / 1_000L).coerceAtLeast(minimumValue = 0L)
    val minutes: Long = totalSeconds / 60L
    val seconds: Long = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
