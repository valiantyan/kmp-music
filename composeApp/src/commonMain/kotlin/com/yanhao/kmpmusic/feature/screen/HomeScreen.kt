package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.PrimaryPill
import com.yanhao.kmpmusic.feature.components.SectionTitle
import com.yanhao.kmpmusic.feature.components.SongRow
import com.yanhao.kmpmusic.feature.components.coverArtPainter

/**
 * 首页，复刻原型的本地曲库卡、最近播放和本地专辑。
 */
@Composable
fun HomeScreen(
    songs: List<Song>,
    albums: List<Album>,
    currentSongId: String,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onLocalFolder: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        AppHeader(
            title = "首页",
            subtitle = "本地音乐 · 随时随地畅听",
            onSearch = onSearch,
        )
        LibraryCard(
            onScan = onScan,
            onLocalFolder = onLocalFolder,
        )
        SectionTitle(
            title = "最近播放",
            actionLabel = "全部",
            onAction = onSearch,
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            songs.take(2).forEach { song ->
                SongRow(
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                    onOpen = onSongOpen,
                    onPlay = onSongPlay,
                    onMore = onMore,
                )
            }
        }
        SectionTitle(
            title = "本地专辑",
            actionLabel = "更多",
            onAction = { albums.firstOrNull()?.let(onAlbumOpen) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            albums.take(3).forEach { album ->
                AlbumCard(
                    album = album,
                    onOpen = onAlbumOpen,
                    modifier = Modifier.weight(weight = 1f),
                )
            }
        }
    }
}

/**
 * 本地音乐库概览卡，保留原型的薄荷色文件夹视觉。
 */
@Composable
private fun LibraryCard(
    onScan: () -> Unit,
    onLocalFolder: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MusicColors.Paper,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(ColorWithAlpha(MusicColors.Accent, 0.12f), MusicColors.Paper),
                    ),
                )
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = "本地音乐库", color = MusicColors.Muted, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "1,248", style = MaterialTheme.typography.headlineLarge)
                    Text(text = " 首歌曲", modifier = Modifier.padding(bottom = 4.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "86 张专辑 · 128 位歌手", color = MusicColors.Muted, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryPill(text = "扫描本地音乐", onClick = onScan)
                    Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Soft) {
                        IconButton(onClick = onLocalFolder) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "打开本地文件夹")
                        }
                    }
                }
            }
            Image(
                painter = coverArtPainter(CoverArt.HeroLocalFolder),
                contentDescription = "本地音乐库文件夹插画",
                modifier = Modifier.size(126.dp).clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * 给颜色添加透明度，集中处理 Compose 颜色转换。
 */
private fun ColorWithAlpha(color: androidx.compose.ui.graphics.Color, alpha: Float): androidx.compose.ui.graphics.Color {
    return color.copy(alpha = alpha)
}
