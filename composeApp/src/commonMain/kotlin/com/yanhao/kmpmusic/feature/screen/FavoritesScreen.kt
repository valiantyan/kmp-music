package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.FavoriteSection
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 收藏页，聚合收藏歌曲、专辑和歌手。
 */
@Composable
fun FavoritesScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    currentSongId: String,
    section: FavoriteSection,
    onSection: (FavoriteSection) -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val likedSongs: List<Song> = songs.filter { song -> song.isLiked }
    val likedAlbums: List<Album> = albums.filter { album ->
        likedSongs.any { song -> song.album == album.title }
    }
    val likedArtists: List<Artist> = artists.filter { artist ->
        likedSongs.any { song -> song.artist == artist.name }
    }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppHeader(title = "收藏", subtitle = "喜欢的音乐都在这里")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FavoriteSection.entries.forEach { item ->
                FilterChip(
                    selected = section == item,
                    onClick = { onSection(item) },
                    label = { Text(text = item.label()) },
                )
            }
        }
        when (section) {
            FavoriteSection.Songs -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                likedSongs.forEach { song ->
                    SongRow(song, song.id == currentSongId, onSongOpen, onSongPlay, onMore, onLike, dense = true)
                }
            }
            FavoriteSection.Albums -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                likedAlbums.take(2).forEach { album ->
                    AlbumCard(album = album, onOpen = onAlbumOpen, modifier = Modifier.weight(weight = 1f))
                }
            }
            FavoriteSection.Artists -> Column(modifier = Modifier.padding(top = 8.dp)) {
                likedArtists.forEach { artist -> ArtistRow(artist = artist, onOpen = onArtistOpen) }
            }
        }
    }
}

/**
 * 收藏分段中文名。
 */
private fun FavoriteSection.label(): String {
    return when (this) {
        FavoriteSection.Songs -> "歌曲"
        FavoriteSection.Albums -> "专辑"
        FavoriteSection.Artists -> "歌手"
    }
}
