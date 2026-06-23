package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var section: LocalMusicSection by remember(initialSection) {
        mutableStateOf(value = initialSection)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(scaledDp(18.dp)),
    ) {
        item(key = "header") {
            AppHeader(
                title = "本地音乐",
                subtitle = "${songs.size} 首可播放歌曲",
                onBack = onBack,
            )
        }
        item(key = "tabs") {
            LocalMusicSectionTabs(
                selectedSection = section,
                onSection = { nextSection: LocalMusicSection -> section = nextSection },
            )
        }
        when (section) {
            LocalMusicSection.Songs -> songSectionItems(
                songs = songs,
                currentSongId = currentSongId,
                onSongOpen = onSongOpen,
                onSongPlay = onSongPlay,
                onMore = onMore,
            )
            LocalMusicSection.Albums -> albumSectionItems(
                albums = albums,
                onAlbumOpen = onAlbumOpen,
            )
            LocalMusicSection.Artists -> artistSectionItems(
                artists = artists,
                onArtistOpen = onArtistOpen,
            )
            LocalMusicSection.Sources -> item(key = "sources") {
                SourceSection(sources = sources, problems = problems)
            }
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
// 渲染歌曲条目时只组合可见行，避免真实曲库进入页面时阻塞主线程。
private fun LazyListScope.songSectionItems(
    songs: List<Song>,
    currentSongId: String?,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        item(key = "empty-songs") {
            EmptyLocalMusicState(text = "扫描本地音乐后，歌曲会出现在这里。")
        }
        return
    }
    items(
        items = songs,
        key = { song: Song -> song.id },
        contentType = { "local-song" },
    ) { song: Song ->
        SongRow(
            song = song,
            isCurrentSong = song.id == currentSongId,
            onOpen = { selectedSong: Song -> onSongOpen(selectedSong, songs) },
            onPlay = { selectedSong: Song -> onSongPlay(selectedSong, songs) },
            onMore = onMore,
            dense = true,
        )
    }
}
// 渲染专辑网格行，保持两列布局并让行级组合按需发生。
private fun LazyListScope.albumSectionItems(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    if (albums.isEmpty()) {
        item(key = "empty-albums") {
            EmptyLocalMusicState(text = "扫描后会按专辑自动聚合。")
        }
        return
    }
    items(
        items = albums.chunked(size = 2),
        key = { rowAlbums: List<Album> -> rowAlbums.joinToString(separator = "|") { album: Album -> album.id } },
        contentType = { "local-album-row" },
    ) { rowAlbums: List<Album> ->
        Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(14.dp))) {
            rowAlbums.forEach { album: Album ->
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
// 渲染歌手条目时按 [Artist.id] 复用节点，降低分段切换后的重组成本。
private fun LazyListScope.artistSectionItems(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        item(key = "empty-artists") {
            EmptyLocalMusicState(text = "扫描后会按歌手自动聚合。")
        }
        return
    }
    items(
        items = artists,
        key = { artist: Artist -> artist.id },
        contentType = { "local-artist" },
    ) { artist: Artist ->
        ArtistRow(artist = artist, onOpen = onArtistOpen)
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
