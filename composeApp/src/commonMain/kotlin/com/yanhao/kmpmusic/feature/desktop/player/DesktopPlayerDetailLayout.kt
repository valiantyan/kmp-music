package com.yanhao.kmpmusic.feature.desktop.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.defaultPlayerPagePalette
import com.yanhao.kmpmusic.feature.components.rememberPlayerPagePalette
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

// 当前歌曲封面驱动整页背景，空态通过共享默认 palette 保持移动端与桌面回退一致。
@Composable
internal fun rememberDesktopPlayerPagePalette(song: Song?): PlayerPagePalette {
    if (song == null) {
        return defaultPlayerPagePalette()
    }
    return rememberPlayerPagePalette(
        coverArt = song.coverArt,
        coverImageUri = song.coverImageUri,
    )
}

// 顶栏避开 macOS traffic lights，只保留页面返回和标题。
@Composable
internal fun DesktopPlayerTopBar(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopRoundIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            Text(
                text = "正在播放",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.SidebarTitle,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// 左封面右控制的桌面布局，避免把移动播放页简单放大。
@Composable
internal fun DesktopPlayerContent(
    song: Song,
    queueRows: List<DesktopPlayerQueueRowState>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    playbackMode: PlaybackMode,
    volume: Float,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onQueueIndexClick: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val coverSize =
            (maxWidth * 0.36f).coerceIn(
                minimumValue = 340.dp,
                maximumValue = 520.dp,
            )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArtImage(
                coverArt = song.coverArt,
                coverImageUri = song.coverImageUri,
                contentDescription = "${song.title} 封面",
                modifier =
                    Modifier
                        .size(coverSize)
                        .clip(RoundedCornerShape(34.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                DesktopPlayerMetadata(song = song)
                Spacer(modifier = Modifier.height(34.dp))
                DesktopPlayerProgress(
                    song = song,
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    playbackDurationMs = playbackDurationMs,
                    onSeek = onSeek,
                )
                Spacer(modifier = Modifier.height(28.dp))
                DesktopPlayerControlRow(
                    song = song,
                    isPlaying = isPlaying,
                    playbackMode = playbackMode,
                    onToggle = onToggle,
                    onPrev = onPrev,
                    onNext = onNext,
                    onMode = onMode,
                    onLike = onLike,
                )
                Spacer(modifier = Modifier.height(28.dp))
                DesktopPlayerQueuePreview(
                    queueRows = queueRows,
                    currentSongId = song.id,
                    onQueueIndexClick = onQueueIndexClick,
                )
                Spacer(modifier = Modifier.height(22.dp))
                DesktopPlayerVolume(
                    volume = volume,
                    onVolumeChange = onVolumeChange,
                )
            }
        }
    }
}

// 歌曲信息区只展示元数据，主要动作统一收敛到播放控制条。
@Composable
private fun DesktopPlayerMetadata(
    song: Song,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = song.title,
            color = DesktopMusicColors.Ink,
            fontSize = 41.sp,
            lineHeight = 45.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${song.artist} · ${song.album}",
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.SidebarTitle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DesktopPlayerBadge(text = "本地音乐")
            DesktopPlayerBadge(text = song.quality)
        }
    }
}

// 桌面播放页暴露音量，匹配桌面用户对完整控制的预期。
@Composable
private fun DesktopPlayerVolume(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.width(260.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = "音量",
            tint = DesktopMusicColors.MutedStrong,
            modifier = Modifier.size(22.dp),
        )
        DesktopThinSlider(
            value = volume,
            valueRange = 0f..1f,
            enabled = true,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
        )
    }
}

// 空态也占据全窗口，避免进入播放页后退回普通工作区的割裂感。
@Composable
internal fun DesktopPlayerEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暂无播放",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.PageTitle,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "播放一首本地歌曲后会显示完整播放页。",
                color = DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.SidebarTitle,
            )
        }
    }
}

// 圆形图标按钮统一尺寸，避免不同图标造成控制区跳动。
@Composable
internal fun DesktopRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = DesktopMusicColors.Ink,
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.64f),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}

// 徽标只承载元信息，不参与主要操作。
@Composable
private fun DesktopPlayerBadge(text: String) {
    Surface(
        shape = CircleShape,
        color = DesktopMusicColors.AccentSoft,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = DesktopMusicColors.AccentDeep,
            fontSize = DesktopMusicType.Body,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
