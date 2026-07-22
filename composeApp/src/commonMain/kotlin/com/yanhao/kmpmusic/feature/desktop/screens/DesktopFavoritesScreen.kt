package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAutoHideLazyScrollbar

/**
 * Desktop 收藏根页面按 Figma `1102:879` 只展示歌曲收藏，所有动作继续由外层控制器注入。
 */
@Composable
fun DesktopFavoritesRootScreen(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onPlaySongs: (List<Song>, PlaybackMode) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val displayModel: DesktopFavoritesDisplayModel = buildDesktopFavoritesDisplayModel(songs = songs)
    val listState: LazyListState = rememberLazyListState()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FAVORITES_PAGE_BACKGROUND),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp,
                    top = 32.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
        ) {
            item(key = "favorites-header") {
                DesktopFavoritesHeader(
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
            item(key = "favorites-table-header") {
                DesktopFavoritesTableHeader(modifier = Modifier.padding(top = 40.dp))
            }
            if (displayModel.songs.isEmpty()) {
                item(key = "favorites-empty") {
                    DesktopFavoritesEmptyState(message = displayModel.emptyMessage.orEmpty())
                }
            } else {
                itemsIndexed(
                    items = displayModel.songs,
                    key = { _: Int, song: Song -> song.id },
                ) { index: Int, song: Song ->
                    DesktopFavoritesSongRow(
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
        }
        DesktopAutoHideLazyScrollbar(
            listState = listState,
            modifier =
                Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .padding(end = 24.dp),
        )
    }
}

/** Figma 收藏页使用与本地音乐页一致的浅蓝灰工作区背景。 */
private val FAVORITES_PAGE_BACKGROUND: Color = Color(0xFFF9F9FF)
