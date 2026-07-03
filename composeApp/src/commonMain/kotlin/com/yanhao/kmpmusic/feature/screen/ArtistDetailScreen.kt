package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yanhao.kmpmusic.core.theme.ArtistDetailPalette
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.rememberArtistDetailPalette

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
    demoSongCount: Int = 0,
) {
    val content: ArtistDetailContent = buildArtistDetailContent(
        artist = artist,
        songs = songs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
        demoSongCount = demoSongCount,
    )
    val listState: LazyListState = rememberLazyListState()
    val density: Density = LocalDensity.current
    val statusBarInset: Dp = with(density) {
        WindowInsets.statusBars.getTop(density = this).toDp()
    }
    val palette: ArtistDetailPalette = rememberArtistDetailPalette(
        coverArt = artist.coverArt,
        coverImageUri = artist.coverImageUri,
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(color = palette.backgroundColor),
    ) {
        val scrollSpec: ArtistDetailScrollSpec = createArtistDetailScrollSpec(
            statusBarInset = statusBarInset,
            viewportHeight = maxHeight,
        )
        val pullStretchState: ArtistDetailPullStretchState = rememberArtistDetailPullStretchState(
            listState = listState,
            maxPullStretchHeight = scrollSpec.maxPullStretchHeight,
        )
        val scrollState: ArtistDetailScrollState = calculateArtistDetailScrollState(
            spec = scrollSpec,
            scrollOffset = calculateArtistDetailScrollOffset(
                listState = listState,
                density = density,
                scrollSpec = scrollSpec,
            ),
            pullOffset = pullStretchState.pullStretchHeight,
        )
        ArtistDetailBackground(
            artist = artist,
            palette = palette,
        )
        ArtistDetailHeroChrome(
            artist = artist,
            palette = palette,
            scrollState = scrollState,
            modifier = Modifier.zIndex(zIndex = 0f),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scrollState.contentTopBarrier)
                .clipToBounds()
                .zIndex(zIndex = 1f)
                .nestedScroll(connection = pullStretchState.nestedScrollConnection),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "artist-content-spacer", contentType = "artist-detail-content-spacer") {
                Spacer(modifier = Modifier.height(height = scrollState.contentGroupSpacerHeight))
            }
            item(key = "artist-expanded-title") {
                ArtistDetailExpandedTitle(
                    artistName = artist.name,
                    alpha = scrollState.expandedContentAlpha,
                )
            }
            item(key = "artist-play-all-anchor-spacer") {
                Spacer(modifier = Modifier.height(height = artistDetailPlayAllScrollHeight))
            }
            item(key = "artist-play-all-section-header") {
                ArtistDetailPlayAllSectionHeader(
                    text = content.playAllText,
                    countText = content.playAllCountText,
                    enabled = content.artistSongs.isNotEmpty(),
                    onClick = {
                        content.artistSongs.firstOrNull()?.let { song: Song ->
                            onSongPlay(song, content.artistSongs)
                        }
                    },
                )
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
        ArtistDetailToolbar(
            artistName = artist.name,
            scrollState = scrollState,
            onBack = onBack,
            modifier = Modifier.zIndex(zIndex = 2f),
        )
    }
}

// 只需要折叠区间内的滚动距离，超过后模型会保持完全折叠。
private fun calculateArtistDetailScrollOffset(
    listState: LazyListState,
    density: Density,
    scrollSpec: ArtistDetailScrollSpec,
): Dp {
    val firstVisibleItemScrollOffset: Dp = with(density) {
        listState.firstVisibleItemScrollOffset.toDp()
    }
    return calculateArtistDetailScrollOffsetFromListPosition(
        firstVisibleItemIndex = listState.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        scrollSpec = scrollSpec,
    )
}
