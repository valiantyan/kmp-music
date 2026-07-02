package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 手机首页歌曲页，按 Figma 节点 `871:477` 复刻歌曲分段的首个完成状态。
 */
@Composable
fun HomeScreen(
    songs: List<Song>,
    libraryStats: LibraryStats,
    scanState: LocalMusicScanState,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 80.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "home-filter-chips") {
                HomeFilterChips()
            }
            item(key = "home-filter-title-gap") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "home-title-row") {
                HomeSongSectionHeader(
                    songCountText = formatHomeSongCount(
                        songs = songs,
                        libraryStats = libraryStats,
                    ),
                )
            }
            item(key = "home-song-list-gap") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (songs.isEmpty()) {
                item(key = "home-empty-songs") {
                    HomeEmptySongsCard(
                        scanState = scanState,
                        onScan = onScan,
                    )
                }
            } else {
                items(
                    items = songs,
                    key = { song: Song -> song.id },
                    contentType = { "home-song" },
                ) { song: Song ->
                    HomeSongRow(
                        song = song,
                        isCurrentSong = song.id == currentSongId,
                        currentPlaybackStatus = currentPlaybackStatus,
                        queueSongs = songs,
                        onSongOpen = onSongOpen,
                        onMore = onMore,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        HomeTopAppBar(
            onSearch = onSearch,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

// 首页歌曲统计优先使用曲库统计，扫描前或测试数据缺失时退回当前列表数量。
private fun formatHomeSongCount(
    songs: List<Song>,
    libraryStats: LibraryStats,
): String {
    val songCount: Int = if (libraryStats.songCount > 0) {
        libraryStats.songCount
    } else {
        songs.size
    }
    return "$songCount 首歌曲"
}
