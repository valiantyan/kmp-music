package com.yanhao.kmpmusic.feature.app.playerbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MiniPlayerPalette
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.rememberMiniPlayerPalette

/** 全局迷你播放器，展示当前歌曲、播放控制和只读进度。 */
@Composable
fun MobileMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val miniPlayerPalette: MiniPlayerPalette = rememberMiniPlayerPalette(
        coverArt = song.coverArt,
        coverImageUri = song.coverImageUri,
    )
    val containerColor: Color by animateColorAsState(
        targetValue = miniPlayerPalette.containerColor,
        animationSpec = tween(durationMillis = 260),
        label = "MiniPlayerContainerColor",
    )
    val progressFraction: Float = calculateMiniPlayerProgressFraction(
        playbackPositionMs = playbackPositionMs,
        playbackDurationMs = playbackDurationMs ?: song.durationMs,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = scaledDp(MusicDimens.PagePaddingHorizontal))
            .height(scaledDp(MusicDimens.MiniPlayerHeight)),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        onClick = onOpen,
    ) {
        Box(modifier = Modifier.height(scaledDp(MusicDimens.MiniPlayerHeight))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledDp(MusicDimens.MiniPlayerHeight))
                    .padding(start = scaledDp(10.dp), top = scaledDp(8.dp), end = scaledDp(17.dp), bottom = scaledDp(7.dp)),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(weight = 1f),
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(11.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArtImage(
                        coverArt = song.coverArt,
                        coverImageUri = song.coverImageUri,
                        contentDescription = "${song.title} 封面",
                        modifier = Modifier.size(scaledDp(45.dp)).clip(RoundedCornerShape(scaledDp(8.dp))),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier.weight(weight = 1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = song.title,
                            fontSize = scaledSp(20.sp),
                            lineHeight = scaledSp(24.sp),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            color = MusicColors.Muted,
                            fontSize = scaledSp(16.sp),
                            lineHeight = scaledSp(19.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(14.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniControlButton(onClick = onPrev) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "上一首",
                            tint = MusicColors.Ink,
                        )
                    }
                    MiniControlButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = MusicColors.Ink,
                        )
                    }
                    MiniControlButton(onClick = onQueue) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.List,
                            contentDescription = "播放队列",
                            tint = MusicColors.Ink,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(scaledDp(3.dp)).background(MusicColors.Line.copy(alpha = 0.65f)),
            )
            Box(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(fraction = progressFraction).height(scaledDp(3.dp)).background(MusicColors.Accent),
            )
        }
    }
}

/** 迷你播放器只展示只读进度，所有数值必须来自真实播放状态而不是视觉占位。 */
private fun calculateMiniPlayerProgressFraction(
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
): Float {
    val safeDurationMs: Long = playbackDurationMs?.takeIf { durationMs -> durationMs > 0L } ?: return 0f
    return playbackPositionMs
        .coerceIn(minimumValue = 0L, maximumValue = safeDurationMs)
        .toFloat() / safeDurationMs.toFloat()
}

/** 迷你播放器控制按钮使用固定触控区，避免图标变化造成布局跳动。 */
@Composable
private fun MiniControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = scaledDp(28.dp), height = scaledDp(42.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
