package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
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
    currentSongId: String?,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onLocalFolder: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    val recentSongs: List<Song> = buildHomeRecentSongs(songs = songs)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        AppHeader(
            title = "首页",
            subtitle = "本地音乐 · 随时随地畅听",
            onSearch = onSearch,
        )
        Spacer(modifier = Modifier.height(scaledDp(10.dp)))
        LibraryCard(
            onScan = onScan,
            onLocalFolder = onLocalFolder,
        )
        Spacer(modifier = Modifier.height(scaledDp(24.dp)))
        SectionTitle(
            title = "最近播放",
            actionLabel = "全部",
            onAction = onSearch,
        )
        Spacer(modifier = Modifier.height(scaledDp(14.dp)))
        Column(verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
            recentSongs.forEach { song ->
                SongRow(
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                    onOpen = onSongOpen,
                    onPlay = onSongPlay,
                    onMore = onMore,
                )
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(18.dp)))
        SectionTitle(
            title = "本地专辑",
            actionLabel = "更多",
            onAction = { albums.firstOrNull()?.let(onAlbumOpen) },
        )
        Spacer(modifier = Modifier.height(scaledDp(14.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
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
 * 首页最近播放遵循高保真稿的展示顺序，不复用完整曲库目录顺序。
 */
private fun buildHomeRecentSongs(songs: List<Song>): List<Song> {
    val preferredIds: List<String> = listOf("sea-dream", "summer-waltz")
    val preferredSongs: List<Song> = preferredIds.mapNotNull { songId ->
        songs.firstOrNull { song -> song.id == songId }
    }
    if (preferredSongs.size == preferredIds.size) {
        return preferredSongs
    }
    return songs.take(n = 2)
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
        modifier = Modifier
            .fillMaxWidth()
            .height(scaledDp(MusicDimens.LibraryCardHeight))
            .shadow(elevation = scaledDp(12.dp), shape = RoundedCornerShape(scaledDp(MusicDimens.LibraryCardRadius)), clip = false)
            .border(
                width = scaledDp(1.dp),
                color = Color(0x73ADD6D5),
                shape = RoundedCornerShape(scaledDp(MusicDimens.LibraryCardRadius)),
            ),
        shape = RoundedCornerShape(scaledDp(MusicDimens.LibraryCardRadius)),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MusicColors.Accent.copy(alpha = 0.13f), Color.Transparent),
                        radius = 360f,
                    ),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xE6EDFFFD), Color(0xF2FFFFFF)),
                    ),
                )
                .padding(start = scaledDp(22.dp), top = scaledDp(24.dp), end = scaledDp(22.dp), bottom = scaledDp(20.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = "本地音乐库", color = Color(0xFF666F7A), fontSize = scaledSp(21.sp), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(scaledDp(14.dp)))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(11.dp)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "1,248",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = scaledSp(34.sp),
                            lineHeight = scaledSp(38.sp),
                        ),
                    )
                    Text(text = " 首歌曲", modifier = Modifier.padding(bottom = scaledDp(4.dp)), fontSize = scaledSp(17.sp), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(scaledDp(5.dp)))
                Text(text = "86 张专辑 · 128 位歌手", color = Color(0xFF7C8490), fontSize = scaledSp(17.sp))
                Spacer(modifier = Modifier.height(scaledDp(18.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(12.dp)), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryPill(text = "扫描本地音乐", onClick = onScan)
                    Surface(
                        modifier = Modifier.size(scaledDp(46.dp)),
                        shape = RoundedCornerShape(scaledDp(20.dp)),
                        color = MusicColors.Soft.copy(alpha = 0.92f),
                    ) {
                        IconButton(
                            modifier = Modifier.size(scaledDp(46.dp)),
                            onClick = onLocalFolder,
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "打开本地文件夹")
                        }
                    }
                }
            }
            Image(
                painter = coverArtPainter(CoverArt.HeroLocalFolder),
                contentDescription = "本地音乐库文件夹插画",
                modifier = Modifier
                    .size(scaledDp(MusicDimens.HeroFolderSize))
                    .clip(RoundedCornerShape(scaledDp(28.dp))),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
