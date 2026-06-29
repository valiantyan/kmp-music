package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.core.theme.extractPlayerPagePalette
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.coverArtResource
import com.yanhao.kmpmusic.feature.components.coverArtPainter
import org.jetbrains.compose.resources.imageResource

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
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var volume: Float by remember { mutableStateOf(value = 0.68f) }
    val palette: PlayerPagePalette = rememberPlayerPagePalette(song = song)
    LaunchedEffect(Unit) {
        onVolumeChange(volume)
    }
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
                    queueSongs = buildPlayerQueueRows(
                        song = song,
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
                    onVolumeChange = { nextVolume: Float ->
                        volume = nextVolume
                        onVolumeChange(nextVolume)
                    },
                )
            }
        }
    }
}

// 当前歌曲封面驱动整页背景，空态回退到桌面默认纸色。
@Composable
private fun rememberPlayerPagePalette(song: Song?): PlayerPagePalette {
    if (song == null) {
        return PlayerPagePalette(
            backgroundColor = DesktopMusicColors.Paper,
            ambientColor = DesktopMusicColors.Accent,
        )
    }
    val coverImage: ImageBitmap = imageResource(resource = coverArtResource(coverArt = song.coverArt))
    return remember(song.coverArt, coverImage) {
        extractPlayerPagePalette(imageBitmap = coverImage)
    }
}

// 顶栏避开 macOS traffic lights，只保留页面返回和标题。
@Composable
private fun DesktopPlayerTopBar(
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
private fun DesktopPlayerContent(
    song: Song,
    queueSongs: List<Song>,
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
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val coverSize = (maxWidth * 0.36f).coerceIn(
            minimumValue = 340.dp,
            maximumValue = 520.dp,
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = coverArtPainter(song.coverArt),
                contentDescription = "${song.title} 封面",
                modifier = Modifier
                    .size(coverSize)
                    .clip(RoundedCornerShape(34.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                DesktopPlayerMetadata(song = song)
                Spacer(modifier = Modifier.height(34.dp))
                DesktopPlayerProgress(
                    song = song,
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
                    queueSongs = queueSongs,
                    currentSongId = song.id,
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

// 进度条始终使用真实播放时长，缺失时回退到歌曲元数据时长。
@Composable
private fun DesktopPlayerProgress(
    song: Song,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onSeek: (Long) -> Unit,
) {
    val durationMs: Long = playbackDurationMs ?: song.durationMs ?: 0L
    val safePositionMs: Long = playbackPositionMs.coerceIn(
        minimumValue = 0L,
        maximumValue = durationMs.takeIf { value: Long -> value > 0L } ?: playbackPositionMs.coerceAtLeast(
            minimumValue = 0L,
        ),
    )
    DesktopThinSlider(
        value = if (durationMs > 0L) safePositionMs.toFloat() else 0f,
        valueRange = 0f..durationMs.coerceAtLeast(minimumValue = 1L).toFloat(),
        enabled = durationMs > 0L,
        onValueChange = { value: Float -> onSeek(value.toLong()) },
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DesktopPlayerTimeText(text = formatDesktopPlayerTime(valueMs = safePositionMs))
        DesktopPlayerTimeText(text = formatDesktopPlayerTime(valueMs = durationMs))
    }
}

// 主控制区只放播放核心动作，模式和切歌使用相同触控尺寸保证布局稳定。
@Composable
private fun DesktopPlayerControlRow(
    song: Song,
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onLike: (String) -> Unit,
) {
    val modeIcon: DesktopPlayerModeIcon = playbackMode.toDesktopPlayerModeIcon()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopRoundIconButton(
            icon = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (song.isLiked) "取消收藏" else "收藏",
            tint = if (song.isLiked) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
            onClick = { onLike(song.id) },
        )
        DesktopRoundIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "上一首",
            onClick = onPrev,
        )
        Surface(
            modifier = Modifier.size(74.dp),
            shape = CircleShape,
            color = DesktopMusicColors.Ink,
            onClick = onToggle,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        DesktopRoundIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "下一首",
            onClick = onNext,
        )
        DesktopRoundIconButton(
            icon = modeIcon.icon,
            contentDescription = modeIcon.contentDescription,
            tint = DesktopMusicColors.Accent,
            onClick = onMode,
        )
    }
}

// 队列预览用单个轻量分组承载，避免播放页变成多层卡片。
@Composable
private fun DesktopPlayerQueuePreview(
    queueSongs: List<Song>,
    currentSongId: String,
) {
    Column {
        Text(
            text = "播放队列",
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.SidebarTitle,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.58f)),
        ) {
            itemsIndexed(items = queueSongs) { index: Int, song: Song ->
                DesktopPlayerQueueRow(
                    index = index,
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                )
            }
            if (queueSongs.isEmpty()) {
                item {
                    Text(
                        text = "当前队列没有更多歌曲",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        color = DesktopMusicColors.Muted,
                        fontSize = DesktopMusicType.Body,
                    )
                }
            }
        }
    }
}

// 队列行保持紧凑高度，让右侧控制区仍以当前歌曲为视觉中心。
@Composable
private fun DesktopPlayerQueueRow(
    index: Int,
    song: Song,
    isCurrentSong: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString().padStart(length = 2, padChar = '0'),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
            fontWeight = FontWeight.Bold,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.TableHeader,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDesktopPlayerTime(valueMs = song.durationMs ?: 0L),
            color = DesktopMusicColors.Muted,
            fontSize = DesktopMusicType.Body,
        )
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
private fun DesktopPlayerEmptyState() {
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
private fun DesktopRoundIconButton(
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

// 时间文本集中设置，保证进度条两端对齐一致。
@Composable
private fun DesktopPlayerTimeText(text: String) {
    Text(
        text = text,
        color = DesktopMusicColors.MutedStrong,
        fontSize = DesktopMusicType.Body,
        fontWeight = FontWeight.SemiBold,
    )
}

// 队列从当前歌曲开始展示完整播放顺序，列表本身负责滚动承载全部数据。
private fun buildPlayerQueueRows(
    song: Song,
    queueSongs: List<Song>,
): List<Song> {
    if (queueSongs.isEmpty()) {
        return emptyList()
    }
    val currentIndex: Int = queueSongs.indexOfFirst { candidate: Song -> candidate.id == song.id }
    if (currentIndex < 0) {
        return queueSongs
    }
    return queueSongs.drop(n = currentIndex) + queueSongs.take(n = currentIndex)
}

// 播放模式映射集中在播放页内部，避免 UI 分支散落在控件调用处。
private fun PlaybackMode.toDesktopPlayerModeIcon(): DesktopPlayerModeIcon {
    return when (this) {
        PlaybackMode.LoopAll -> DesktopPlayerModeIcon(
            icon = Icons.Rounded.Repeat,
            contentDescription = "顺序播放",
        )
        PlaybackMode.LoopOne -> DesktopPlayerModeIcon(
            icon = Icons.Rounded.RepeatOne,
            contentDescription = "单曲循环",
        )
        PlaybackMode.Shuffle -> DesktopPlayerModeIcon(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "随机播放",
        )
    }
}

// 播放模式图标和文案作为一组返回，保证可访问文案随图标同步变化。
private data class DesktopPlayerModeIcon(
    val icon: ImageVector,
    val contentDescription: String,
)

// 播放时间统一按 mm:ss 输出，避免未知时长或负值显示异常。
private fun formatDesktopPlayerTime(valueMs: Long): String {
    val totalSeconds: Long = (valueMs / 1000L).coerceAtLeast(minimumValue = 0L)
    val minutes: Long = totalSeconds / 60L
    val seconds: Long = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
}
