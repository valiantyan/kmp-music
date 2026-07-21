package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证新版 Figma 搜索页的轻量展示规则，避免 UI 直接消费空 query 的全量搜索结果。
 */
class SearchScreenDisplayModelTest {
    @Test
    fun emptyQueryKeepsResultContentHiddenUntilUserTypes() {
        assertFalse(actual = shouldShowSearchResultContent(query = "", activeQuery = ""))
        assertFalse(actual = shouldShowSearchResultContent(query = "   ", activeQuery = ""))
        assertTrue(actual = shouldShowSearchResultContent(query = "周杰伦", activeQuery = "周杰伦"))
    }

    @Test
    fun pendingDebouncedQueryKeepsResultContentHidden() {
        assertFalse(actual = shouldShowSearchResultContent(query = "雨", activeQuery = ""))
        assertFalse(actual = shouldShowSearchResultContent(query = "雨声", activeQuery = "雨"))
    }

    @Test
    fun pendingDebouncedQueryShowsPendingStateInsteadOfEmptySuggestion() {
        assertTrue(actual = shouldShowSearchPendingState(query = "雨", activeQuery = ""))
        assertTrue(actual = shouldShowSearchPendingState(query = "雨声", activeQuery = "雨"))
        assertFalse(actual = shouldShowSearchPendingState(query = "", activeQuery = ""))
        assertFalse(actual = shouldShowSearchPendingState(query = "雨", activeQuery = "雨"))
    }

    @Test
    fun hiddenResultContentDoesNotBuildLazyRows() {
        val rows: List<SearchResultLazyRow> =
            buildVisibleSearchResultLazyRows(
                query = "雨",
                activeQuery = "",
                selectedTab = SearchResultTab.Songs,
                result =
                    SearchResult(
                        songs = listOf(testSong(id = "song-1", title = "雨声")),
                        albums = emptyList(),
                        artists = emptyList(),
                    ),
            )

        assertEquals(expected = emptyList(), actual = rows)
    }

    @Test
    fun allScopeUsesSongsTabAsFigmaDefaultTab() {
        assertEquals(
            expected = SearchResultTab.Songs,
            actual = visibleSearchResultTab(scope = SearchScope.All),
        )
        assertEquals(
            expected = SearchResultTab.Songs,
            actual = visibleSearchResultTab(scope = SearchScope.Songs),
        )
    }

    @Test
    fun playlistTabIsVisualOnlyUntilDomainSupportsPlaylistSearch() {
        assertNull(actual = SearchResultTab.Playlists.scope)
    }

    @Test
    fun searchHistorySectionShowsWheneverRealHistoryExists() {
        assertFalse(actual = shouldShowSearchHistorySection(history = emptyList()))
        assertTrue(actual = shouldShowSearchHistorySection(history = listOf("One Summer")))
    }

    @Test
    fun emptyHistoryDoesNotUseFigmaFallbackChips() {
        assertEquals(
            expected = emptyList(),
            actual = visibleSearchHistoryChips(history = emptyList()),
        )
        assertEquals(
            expected = listOf("One Summer"),
            actual = visibleSearchHistoryChips(history = listOf("One Summer")),
        )
    }

    @Test
    fun songSearchResultsBuildOneHomeSongLazyRowPerSong() {
        val songs: List<Song> =
            (1..3).map { index: Int ->
                testSong(id = "song-$index", title = "雨声 $index")
            }
        val rows: List<SearchResultLazyRow> =
            buildSearchResultLazyRows(
                selectedTab = SearchResultTab.Songs,
                result =
                    SearchResult(
                        songs = songs,
                        albums = emptyList(),
                        artists = emptyList(),
                    ),
            )

        assertEquals(expected = 3, actual = rows.size)
        assertEquals(expected = "song-1", actual = assertIs<SearchResultLazyRow.HomeSongItem>(rows[0]).song.id)
        assertEquals(expected = "song-3", actual = assertIs<SearchResultLazyRow.HomeSongItem>(rows[2]).song.id)
        assertEquals(expected = "search-home-song", actual = searchResultLazyRowContentType(row = rows[0]))
    }

    @Test
    fun albumSearchResultsBuildHomeAlbumRows() {
        val albums: List<Album> =
            (1..3).map { index: Int ->
                testAlbum(id = "album-$index", title = "雨季 $index")
            }
        val rows: List<SearchResultLazyRow> =
            buildSearchResultLazyRows(
                selectedTab = SearchResultTab.Albums,
                result =
                    SearchResult(
                        songs = emptyList(),
                        albums = albums,
                        artists = emptyList(),
                    ),
            )

        assertEquals(expected = 2, actual = rows.size)
        assertEquals(expected = listOf("album-1", "album-2"), actual = assertIs<SearchResultLazyRow.HomeAlbumRow>(rows[0]).albums.map { album: Album -> album.id })
        assertEquals(expected = listOf("album-3"), actual = assertIs<SearchResultLazyRow.HomeAlbumRow>(rows[1]).albums.map { album: Album -> album.id })
    }

    @Test
    fun artistSearchResultsBuildOneHomeArtistLazyRowPerArtist() {
        val artists: List<Artist> =
            (1..2).map { index: Int ->
                testArtist(id = "artist-$index", name = "雨声歌手 $index")
            }
        val rows: List<SearchResultLazyRow> =
            buildSearchResultLazyRows(
                selectedTab = SearchResultTab.Artists,
                result =
                    SearchResult(
                        songs = emptyList(),
                        albums = emptyList(),
                        artists = artists,
                    ),
            )

        assertEquals(expected = 2, actual = rows.size)
        assertEquals(expected = "artist-1", actual = assertIs<SearchResultLazyRow.HomeArtistItem>(rows[0]).artist.id)
        assertEquals(expected = "artist-2", actual = assertIs<SearchResultLazyRow.HomeArtistItem>(rows[1]).artist.id)
    }

    /** 构造搜索展示模型测试用歌曲，避免依赖扫描器演示数据。 */
    private fun testSong(
        id: String,
        title: String,
    ): Song =
        Song(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            coverArt = CoverArt.CoverSeaDream,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
        )

    /** 构造搜索展示模型测试用专辑，避免依赖首页演示数据。 */
    private fun testAlbum(
        id: String,
        title: String,
    ): Album =
        Album(
            id = id,
            title = title,
            artist = "Artist",
            songCount = 3,
            coverArt = CoverArt.CoverSeaDream,
            mood = "雨天",
            year = "2026",
        )

    /** 构造搜索展示模型测试用歌手，避免依赖首页演示数据。 */
    private fun testArtist(
        id: String,
        name: String,
    ): Artist =
        Artist(
            id = id,
            name = name,
            songCount = 4,
            albumCount = 2,
            coverArt = CoverArt.CoverSeaDream,
            tag = "本地",
        )
}
