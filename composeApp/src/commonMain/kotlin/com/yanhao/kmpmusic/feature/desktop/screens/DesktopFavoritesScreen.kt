package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.FavoriteSection
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAlbumGrid
import com.yanhao.kmpmusic.feature.desktop.components.DesktopArtistStrip
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRow
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSegmentedControl
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSongTable
import com.yanhao.kmpmusic.feature.desktop.components.DesktopToolbar

private const val FAVORITE_ALBUM_PREVIEW_COUNT = 4
private const val ARTIST_STRIP_COUNT = 4

/**
 * 收藏根页面保留歌曲、专辑、歌手分段，所有动作继续由外层控制器注入。
 */
@Composable
fun DesktopFavoritesRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    section: FavoriteSection,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSection: (FavoriteSection) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val likedSongs: List<Song> = songs.filter { song: Song -> song.isLiked }
    val playAllLabel: String = rootPlayAllLabel(
        songs = likedSongs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "收藏",
            eyebrow = "喜欢的音乐都在这里",
        ) {
            DesktopSegmentedControl(
                labels = listOf("歌曲", "专辑", "歌手"),
                selectedIndex = section.ordinal,
                onSelect = { index: Int -> onSection(FavoriteSection.entries[index]) },
            )
        }
        DesktopThreeStatRow(
            firstTitle = "收藏歌曲",
            firstValue = likedSongs.size.toString(),
            secondTitle = "收藏专辑",
            secondValue = albums.size.toString(),
            thirdTitle = "收藏歌手",
            thirdValue = artists.size.toString(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        when (section) {
            FavoriteSection.Songs -> {
                DesktopToolbar(
                    playAllLabel = playAllLabel,
                    sortLabel = "排序：最近收藏",
                    onPlayAll = {
                        playOrToggleRootCollection(
                            songs = likedSongs,
                            currentSongId = currentSongId,
                            currentPlaybackStatus = currentPlaybackStatus,
                            onSongPlay = onSongPlay,
                            onCurrentSongToggle = onCurrentSongToggle,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopSongTable(
                    songs = likedSongs,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    showFavoriteColumn = true,
                    trailingDateLabel = "收藏时间",
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                )
                if (albums.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DesktopSectionHeader(
                        title = "收藏的专辑",
                        actionLabel = "查看全部",
                        onAction = { onSection(FavoriteSection.Albums) },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DesktopAlbumGrid(
                        albums = albums.take(FAVORITE_ALBUM_PREVIEW_COUNT),
                        onAlbumOpen = onAlbumOpen,
                    )
                }
            }
            FavoriteSection.Albums -> {
                DesktopSectionHeader(
                    title = "收藏的专辑",
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopAlbumGrid(
                    albums = albums,
                    onAlbumOpen = onAlbumOpen,
                )
            }
            FavoriteSection.Artists -> {
                DesktopSectionHeader(
                    title = "收藏的歌手",
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopArtistStrip(
                    artists = artists.take(ARTIST_STRIP_COUNT),
                    onArtistOpen = onArtistOpen,
                )
                if (artists.size > ARTIST_STRIP_COUNT) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        artists.drop(ARTIST_STRIP_COUNT).forEach { artist: Artist ->
                            DesktopContentRow(
                                icon = Icons.Rounded.Person,
                                title = artist.name,
                                subtitle = "${artist.songCount} 首歌曲 · ${artist.tag}",
                                actionLabel = "打开",
                                onClick = { onArtistOpen(artist) },
                            )
                        }
                    }
                }
            }
        }
    }
}
