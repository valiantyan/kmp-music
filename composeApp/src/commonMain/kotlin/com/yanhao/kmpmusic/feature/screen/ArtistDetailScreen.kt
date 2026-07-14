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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
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
) {
    val content: ArtistDetailContent = remember(
        artist,
        songs,
        currentSongId,
        currentPlaybackStatus,
    ) {
        buildArtistDetailContent(
            artist = artist,
            songs = songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
        )
    }
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
        val layoutState: ArtistDetailLayoutState = calculateArtistDetailLayoutState(
            spec = scrollSpec,
            pullOffset = pullStretchState.pullStretchHeight,
        )
        val scrollState: State<ArtistDetailScrollState> = rememberArtistDetailScrollState(
            listState = listState,
            density = density,
            scrollSpec = scrollSpec,
            pullOffset = layoutState.pullStretchHeight,
        )
        ArtistDetailBackground(
            artist = artist,
            palette = palette,
        )
        ArtistDetailHeroChrome(
            artist = artist,
            palette = palette,
            scrollState = scrollState,
            layoutState = layoutState,
            modifier = Modifier.zIndex(zIndex = 0f),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = layoutState.contentTopBarrier)
                .clipToBounds()
                .zIndex(zIndex = 1f)
                .graphicsLayer {
                    translationY = with(density) { -pullStretchState.bottomBounceOffset.toPx() }
                }
                .nestedScroll(connection = pullStretchState.nestedScrollConnection),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "artist-content-spacer", contentType = "artist-detail-content-spacer") {
                Spacer(modifier = Modifier.height(height = layoutState.contentGroupSpacerHeight))
            }
            item(key = "artist-expanded-title") {
                ArtistDetailExpandedTitle(
                    artistName = artist.name,
                    scrollState = scrollState,
                )
            }
            item(key = "artist-play-all-anchor-spacer") {
                Spacer(modifier = Modifier.height(height = artistDetailPlayAllScrollHeight))
            }
            item(key = "artist-play-all-section-header") {
                AlbumDetailPlayAllButton(
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
            item(key = "artist-play-all-list-gap", contentType = "artist-detail-gap") {
                Spacer(modifier = Modifier.height(height = artistDetailPlayAllListGapHeight))
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
            collapsedToolbarHeight = layoutState.collapsedToolbarHeight,
            onBack = onBack,
            modifier = Modifier.zIndex(zIndex = 2f),
        )
    }
}

// 列表滚动偏移只驱动头图和 Toolbar，避免普通滚动重组整页和歌曲列表。
@Composable
private fun rememberArtistDetailScrollState(
    listState: LazyListState,
    density: Density,
    scrollSpec: ArtistDetailScrollSpec,
    pullOffset: Dp,
): State<ArtistDetailScrollState> {
    return remember(
        listState,
        density,
        scrollSpec,
        pullOffset,
    ) {
        derivedStateOf {
            calculateArtistDetailScrollState(
                spec = scrollSpec,
                scrollOffset = calculateArtistDetailScrollOffset(
                    listState = listState,
                    density = density,
                    scrollSpec = scrollSpec,
                ),
                pullOffset = pullOffset,
            )
        }
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
