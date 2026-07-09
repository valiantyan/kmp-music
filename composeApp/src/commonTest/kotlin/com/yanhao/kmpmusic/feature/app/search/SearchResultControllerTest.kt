package com.yanhao.kmpmusic.feature.app.search

import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.SearchResult
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchResultControllerTest {
    /**
     * 防抖词未追上输入词时必须返回空结果，不能把空 active query 派生成全量曲库。
     */
    @Test
    fun pendingQueryReturnsEmptyResult(): Unit {
        val controller = SearchResultController(
            musicLibraryRepository = InMemoryMusicLibraryRepository(),
        )
        val result: SearchResult = controller.search(
            state = baseState().copy(
                searchQuery = "river",
                activeSearchQuery = "",
                searchScope = SearchScope.All,
            ),
        )
        assertTrue(actual = result.songs.isEmpty())
        assertTrue(actual = result.albums.isEmpty())
        assertTrue(actual = result.artists.isEmpty())
    }

    /**
     * 收藏搜索只能读取收藏投影，不应回退到完整曲库。
     */
    @Test
    fun favoritesSearchUsesFavoriteProjectionOnly(): Unit {
        val favoriteSong: Song = testSong(
            id = "external-favorite",
            title = "Only In Favorites",
        ).copy(isLiked = true)
        val controller = SearchResultController(
            musicLibraryRepository = InMemoryMusicLibraryRepository(),
        )
        val result: SearchResult = controller.search(
            state = baseState().copy(
                favoriteSongs = listOf(favoriteSong),
                searchContext = SearchContext.Favorites,
                searchQuery = favoriteSong.title,
                activeSearchQuery = favoriteSong.title,
                searchScope = SearchScope.Songs,
            ),
        )
        assertEquals(
            expected = listOf(favoriteSong.id),
            actual = result.songs.map { song: Song -> song.id },
        )
    }
}

/** 构造收藏搜索测试所需的最小歌曲实体。 */
private fun testSong(id: String, title: String): Song {
    return Song(
        id = id,
        title = title,
        artist = "收藏歌手",
        album = "收藏专辑",
        duration = "03:00",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "",
        quality = "Lossless",
        lyric = "",
        trackNumber = 1,
        durationMs = 180_000L,
    )
}

/** 构造搜索派生测试的基础 UI 状态。 */
private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
