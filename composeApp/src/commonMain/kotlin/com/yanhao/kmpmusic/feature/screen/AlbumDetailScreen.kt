package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.MobileSecondaryPage

/**
 * 移动端专辑详情页，按 Figma 展示专辑封面、播放全部入口和专辑歌曲列表。
 */
@Composable
fun AlbumDetailScreen(
    album: Album,
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val albumSongs: List<Song> = remember(
        album,
        songs,
    ) {
        buildAlbumDetailSongs(
            album = album,
            songs = songs,
        )
    }
    val content: AlbumDetailContent = remember(
        albumSongs,
    ) {
        buildAlbumDetailContent(albumSongs = albumSongs)
    }
    val playAlbumSong: (Song) -> Unit = remember(
        albumSongs,
        onSongPlay,
    ) {
        { song: Song -> onSongPlay(song, albumSongs) }
    }
    MobileSecondaryPage(
        title = album.title,
        onBack = onBack,
        backgroundColor = albumDetailBackgroundColor,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(weight = 1f),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 40.dp,
            ),
        ) {
        item(key = "album-header", contentType = "album-header") {
            AlbumDetailHeader(album = album)
        }
        item(key = "album-play-all", contentType = "album-play-all") {
            AlbumDetailPlayAllButton(
                text = content.playAllText,
                countText = content.playAllCountText,
                enabled = content.albumSongs.isNotEmpty(),
                onClick = {
                    content.albumSongs.firstOrNull()?.let { song: Song ->
                        onSongPlay(song, content.albumSongs)
                    }
                },
            )
        }
        item(key = "album-song-list-top-gap", contentType = "album-gap") {
            Spacer(modifier = Modifier.height(height = 32.dp))
        }
        itemsIndexed(
            items = content.albumSongs,
            key = { _: Int, song: Song -> song.id },
            contentType = { _: Int, _: Song -> "album-detail-song" },
        ) { index: Int, song: Song ->
            val rowState: AlbumDetailSongRowState = buildAlbumDetailSongRowState(
                index = index,
                song = song,
                isCurrentSong = song.id == currentSongId,
            )
            AlbumDetailSongRow(
                rowState = rowState,
                isCurrentSong = song.id == currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
                onSongPlay = playAlbumSong,
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onMore,
            )
        }
        }
    }
}
