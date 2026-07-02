package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 移动端歌手详情页，使用 Figma 沉浸式视觉并只围绕歌手歌曲列表展开。
 */
@Composable
fun ArtistDetailScreen(
    artist: Artist,
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val content: ArtistDetailContent = buildArtistDetailContent(
        artist = artist,
        songs = songs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xFFF8FAFB)),
    ) {
        ArtistDetailBackground(artist = artist)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            item(key = "artist-top-bar") {
                ArtistDetailTopBar(onBack = onBack)
            }
            item(key = "artist-header") {
                ArtistDetailHeader(artist = artist)
            }
            item(key = "artist-play-all") {
                ArtistDetailPlayAllButton(
                    text = content.playAllText,
                    enabled = content.artistSongs.isNotEmpty(),
                    onClick = {
                        content.artistSongs.firstOrNull()?.let { song: Song ->
                            onSongPlay(song, content.artistSongs)
                        }
                    },
                )
            }
            item(key = "artist-song-title") {
                ArtistDetailSectionTitle()
            }
            items(
                items = content.songRows,
                key = { rowState: ArtistDetailSongRowState -> rowState.song.id },
                contentType = { "artist-detail-song" },
            ) { rowState: ArtistDetailSongRowState ->
                ArtistDetailSongRow(
                    rowState = rowState,
                    artistSongs = content.artistSongs,
                    isCurrentSong = rowState.song.id == currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                )
            }
        }
    }
}
