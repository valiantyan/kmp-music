package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.FavoriteSection
import com.yanhao.kmpmusic.feature.app.library.MusicLibraryProjector
import com.yanhao.kmpmusic.feature.components.MobilePrimaryToolbar

/**
 * 收藏页，按 Figma 节点 899:1147 还原一级页面视觉。
 */
@Composable
fun FavoritesScreen(
    songs: List<Song>,
    currentSongId: String?,
    section: FavoriteSection,
    onSection: (FavoriteSection) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onSearch: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val likedSongs: List<Song> = remember(songs) {
        songs.filter { song: Song -> song.isLiked }
    }
    val likedAlbums: List<Album> = remember(likedSongs, section) {
        if (section == FavoriteSection.Albums) {
            MusicLibraryProjector.buildAlbums(songs = likedSongs)
        } else {
            emptyList()
        }
    }
    val likedArtists: List<Artist> = remember(likedSongs, section) {
        if (section == FavoriteSection.Artists) {
            MusicLibraryProjector.buildArtists(songs = likedSongs)
        } else {
            emptyList()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = favoritesBackgroundColor),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = MusicDimens.MobileToolbarHeight + MusicDimens.MobileToolbarBodySpacing,
                bottom = contentPadding.calculateBottomPadding() + 40.dp,
            ),
        ) {
            item(key = "favorites-action-header", contentType = "favorites-action-header") {
                FavoritesActionHeader(
                    songCount = likedSongs.size,
                    section = section,
                    onPlayAll = {
                        likedSongs.firstOrNull()?.let { song: Song ->
                            onSongPlay(song, likedSongs)
                        }
                    },
                    onSection = onSection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = favoritesHorizontalPadding),
                )
            }
            item(key = "favorites-action-list-gap", contentType = "favorites-gap") {
                Spacer(modifier = Modifier.height(height = favoritesActionToListGap))
            }
            when (section) {
                FavoriteSection.Songs -> favoriteSongItems(
                    likedSongs = likedSongs,
                    currentSongId = currentSongId,
                    onSongPlay = onSongPlay,
                    onMore = onMore,
                    onLike = onLike,
                )
                FavoriteSection.Albums -> favoriteAlbumItems(
                    likedAlbums = likedAlbums,
                    onAlbumOpen = onAlbumOpen,
                )
                FavoriteSection.Artists -> favoriteArtistItems(
                    likedArtists = likedArtists,
                    onArtistOpen = onArtistOpen,
                )
            }
        }
        MobilePrimaryToolbar(
            title = "收藏",
            onSearch = onSearch,
            modifier = Modifier.align(alignment = Alignment.TopCenter),
        )
    }
}

// 歌曲分段是 Figma 的主还原对象，列表尺寸直接跟随节点 899:1161。
private fun LazyListScope.favoriteSongItems(
    likedSongs: List<Song>,
    currentSongId: String?,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    if (likedSongs.isEmpty()) {
        item(key = "favorites-empty-songs", contentType = "favorites-empty") {
            FavoritesEmptyState(message = "收藏喜欢的歌曲后会显示在这里")
        }
        return
    }
    items(
        items = likedSongs,
        key = { song: Song -> song.id },
        contentType = { "favorites-song" },
    ) { song: Song ->
        FavoritesSongRow(
            song = song,
            isCurrentSong = song.id == currentSongId,
            queueSongs = likedSongs,
            onSongPlay = onSongPlay,
            onMore = onMore,
            onLike = onLike,
            modifier = Modifier.padding(horizontal = favoritesHorizontalPadding),
        )
        Spacer(modifier = Modifier.height(height = favoritesSongRowGap))
    }
}
