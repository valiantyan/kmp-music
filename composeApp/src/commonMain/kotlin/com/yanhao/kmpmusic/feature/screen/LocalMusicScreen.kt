package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 本地音乐二级页，承载首页“查看全部”后的全量曲库浏览。
 */
@Composable
fun LocalMusicScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
    initialSection: LocalMusicSection,
    currentSongId: String?,
    onBack: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    var section: LocalMusicSection by remember(initialSection) {
        mutableStateOf(value = initialSection)
    }
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(18.dp))) {
        AppHeader(
            title = "本地音乐",
            subtitle = "${songs.size} 首可播放歌曲",
            onBack = onBack,
        )
        LocalMusicSectionTabs(
            selectedSection = section,
            onSection = { nextSection: LocalMusicSection -> section = nextSection },
        )
        when (section) {
            LocalMusicSection.Songs -> SongSection(songs, currentSongId, onSongOpen, onSongPlay, onMore)
            LocalMusicSection.Albums -> AlbumSection(albums = albums, onAlbumOpen = onAlbumOpen)
            LocalMusicSection.Artists -> ArtistSection(artists = artists, onArtistOpen = onArtistOpen)
            LocalMusicSection.Sources -> SourceSection(sources = sources, problems = problems)
        }
    }
}

// 分段入口使用横向列表，后续扩展平台来源分段时不会挤压页面宽度。
@Composable
private fun LocalMusicSectionTabs(
    selectedSection: LocalMusicSection,
    onSection: (LocalMusicSection) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp))) {
        items(items = LocalMusicSection.entries) { item ->
            FilterChip(
                selected = selectedSection == item,
                onClick = { onSection(item) },
                label = { Text(text = item.label()) },
            )
        }
    }
}

// 渲染全量歌曲列表，复用全局播放中红色规则。
@Composable
private fun SongSection(
    songs: List<Song>,
    currentSongId: String?,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyLocalMusicState(text = "扫描本地音乐后，歌曲会出现在这里。")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
        songs.forEach { song ->
            SongRow(
                song = song,
                isCurrentSong = song.id == currentSongId,
                onOpen = onSongOpen,
                onPlay = onSongPlay,
                onMore = onMore,
                dense = true,
            )
        }
    }
}

// 渲染真实曲库聚合出的专辑。
@Composable
private fun AlbumSection(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyLocalMusicState(text = "扫描后会按专辑自动聚合。")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
        albums.chunked(size = 2).forEach { rowAlbums ->
            Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
                rowAlbums.forEach { album ->
                    AlbumCard(
                        album = album,
                        onOpen = onAlbumOpen,
                        modifier = Modifier.weight(weight = 1f),
                    )
                }
                if (rowAlbums.size == 1) {
                    Spacer(modifier = Modifier.weight(weight = 1f))
                }
            }
        }
    }
}

// 渲染真实曲库聚合出的歌手。
@Composable
private fun ArtistSection(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyLocalMusicState(text = "扫描后会按歌手自动聚合。")
        return
    }
    Column(modifier = Modifier.padding(top = scaledDp(4.dp))) {
        artists.forEach { artist ->
            ArtistRow(artist = artist, onOpen = onArtistOpen)
        }
    }
}

// 本地音乐空态保持轻量，避免在扫描前误导用户已有本地数据。
@Composable
private fun EmptyLocalMusicState(text: String) {
    Text(text = text, color = MusicColors.Muted, fontSize = scaledSp(15.sp), fontWeight = FontWeight.SemiBold)
}

// 本地音乐分段中文名。
private fun LocalMusicSection.label(): String {
    return when (this) {
        LocalMusicSection.Songs -> "歌曲"
        LocalMusicSection.Albums -> "专辑"
        LocalMusicSection.Artists -> "歌手"
        LocalMusicSection.Sources -> "来源"
    }
}
