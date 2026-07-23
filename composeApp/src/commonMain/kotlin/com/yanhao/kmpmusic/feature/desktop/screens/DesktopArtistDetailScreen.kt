package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopArtistDetailTokens
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAutoHideLazyScrollbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopBackTitleToolbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopBackTitleToolbarStyle

/** Desktop 歌手详情将沉浸式 hero、五列表格和固定返回入口组合为一个长列表页面。 */
@Composable
internal fun DesktopArtistDetailScreen(
    artist: Artist?,
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
    val displayModel: DesktopArtistDetailDisplayModel =
        buildDesktopArtistDetailDisplayModel(
            artist = artist,
            songs = songs,
        )
    val listState: LazyListState = rememberLazyListState()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DesktopArtistDetailTokens.Background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "artist-detail-hero") {
                DesktopArtistDetailHero(
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
            item(key = "artist-detail-table-header") {
                DesktopArtistDetailTableHeader()
            }
            if (displayModel.songs.isEmpty()) {
                item(key = "artist-detail-empty") {
                    DesktopArtistDetailEmptyState(message = displayModel.emptyMessage.orEmpty())
                }
            } else {
                itemsIndexed(
                    items = displayModel.songs,
                    key = { _: Int, song: Song -> song.id },
                ) { index: Int, song: Song ->
                    DesktopArtistDetailSongRow(
                        index = index,
                        song = song,
                        songs = displayModel.songs,
                        isCurrentSong = song.id == currentSongId,
                        isPlaying = isPlaying,
                        onSongPlay = onSongPlay,
                        onCurrentSongToggle = onCurrentSongToggle,
                        onMore = onMore,
                        onLike = onLike,
                    )
                }
            }
        }
        DesktopArtistDetailNavigationToolbar(
            listState = listState,
            onBack = onBack,
            modifier = Modifier.align(alignment = Alignment.TopStart),
        )
        DesktopAutoHideLazyScrollbar(
            listState = listState,
            modifier =
                Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .padding(end = 24.dp),
        )
    }
}

/** 返回入口仅在 hero 可见时使用悬浮样式，滚到曲目区后切为内容背景保证对比度。 */
@Composable
private fun DesktopArtistDetailNavigationToolbar(
    listState: LazyListState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style: DesktopBackTitleToolbarStyle =
        if (listState.firstVisibleItemIndex == 0) {
            DesktopBackTitleToolbarStyle.Overlay
        } else {
            DesktopBackTitleToolbarStyle.Content
        }
    DesktopBackTitleToolbar(
        title = null,
        onBack = onBack,
        style = style,
        contentBackgroundColor = DesktopArtistDetailTokens.Background,
        modifier = modifier,
    )
}
