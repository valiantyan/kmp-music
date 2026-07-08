package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.SongRow

// 根据当前 tab 渲染既有搜索结果组件，保持播放、收藏和详情入口逻辑不分叉。
@Composable
internal fun SearchResultContent(
    selectedTab: SearchResultTab,
    result: SearchResult,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    when (selectedTab) {
        SearchResultTab.Songs -> SearchSongResults(
            songs = result.songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
        SearchResultTab.Albums -> SearchAlbumResults(
            albums = result.albums,
            onAlbumOpen = onAlbumOpen,
        )
        SearchResultTab.Artists -> SearchArtistResults(
            artists = result.artists,
            onArtistOpen = onArtistOpen,
        )
        SearchResultTab.Playlists -> SearchNoResultState(message = "当前版本暂不支持歌单搜索")
    }
}

// 歌曲结果复用全局歌曲行，保证当前播放态和 more 面板语义一致。
@Composable
private fun SearchSongResults(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        SearchNoResultState(message = "没有找到歌曲")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
        songs.forEach { song: Song ->
            SongRow(
                song = song,
                isCurrentSong = song.id == currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                onPlay = { selectedSong: Song -> onSongPlay(selectedSong, songs) },
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
                dense = true,
            )
        }
    }
}

// 专辑结果沿用现有专辑卡片，以双列网格贴近移动端浏览密度。
@Composable
private fun SearchAlbumResults(
    albums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    if (albums.isEmpty()) {
        SearchNoResultState(message = "没有找到专辑")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
        albums.chunked(size = 2).forEach { rowAlbums: List<Album> ->
            Row(horizontalArrangement = Arrangement.spacedBy(space = 14.dp)) {
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
}

// 歌手结果复用全局歌手行，避免详情入口和首页口径分叉。
@Composable
private fun SearchArtistResults(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        SearchNoResultState(message = "没有找到歌手")
        return
    }
    Column {
        artists.forEach { artist: Artist ->
            ArtistRow(artist = artist, onOpen = onArtistOpen)
        }
    }
}
