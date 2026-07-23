package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopAlbumDetailTokens
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAutoHideLazyScrollbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopBackTitleToolbar

/** Desktop 专辑详情将 Figma 导航、专辑头部与长曲目列表组合为一个可滚动的二级页面。 */
@Composable
internal fun DesktopAlbumDetailScreen(
    album: Album?,
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, PlaybackMode) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val displayModel: DesktopAlbumDetailDisplayModel =
        buildDesktopAlbumDetailDisplayModel(
            album = album,
            songs = songs,
        )
    val listState: LazyListState = rememberLazyListState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DesktopAlbumDetailTokens.Background),
    ) {
        DesktopBackTitleToolbar(
            title = "专辑详情",
            onBack = onBack,
        )
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(bottom = 24.dp),
            ) {
                item(key = "album-detail-header") {
                    DesktopAlbumDetailHeader(
                        displayModel = displayModel,
                        onPlayAll = {
                            onPlaySongs(
                                displayModel.songs,
                                PlaybackMode.LoopAll,
                            )
                        },
                        onShuffle = {
                            onPlaySongs(
                                displayModel.songs,
                                PlaybackMode.Shuffle,
                            )
                        },
                    )
                }
                item(key = "album-detail-table-header") {
                    DesktopAlbumDetailTableHeader()
                }
                itemsIndexed(
                    items = displayModel.songs,
                    key = { _: Int, song: Song -> song.id },
                ) { index: Int, song: Song ->
                    DesktopAlbumDetailSongRow(
                        index = index,
                        song = song,
                        songs = displayModel.songs,
                        isCurrentSong = song.id == currentSongId,
                        isPlaying = isPlaying,
                        hasTopSpacing = index == 0,
                        onSongPlay = onSongPlay,
                        onCurrentSongToggle = onCurrentSongToggle,
                        onMore = onMore,
                        onLike = onLike,
                    )
                }
            }
            DesktopAutoHideLazyScrollbar(
                listState = listState,
                modifier =
                    Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .padding(end = DesktopAlbumDetailTokens.ContentPadding),
            )
        }
    }
}
