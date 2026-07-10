package com.yanhao.kmpmusic.feature.app.surfaces

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 全局面板测试，确保各列表入口都复用同一个单曲更多面板解析逻辑。
 */
class AppPanelsTest {
    /**
     * 添加到歌单弹窗需要区分数据库为空和搜索无结果，避免空库时误导用户。
     */
    @Test
    fun addToPlaylistEmptyStateDistinguishesEmptyDatabaseFromNoSearchResult(): Unit {
        assertEquals(
            expected = "暂无歌单",
            actual = resolveAddToPlaylistEmptyStateText(
                flow = AddToPlaylistFlowState(
                    songId = "song-1",
                    hasAnyPlaylist = false,
                    playlistSearchQuery = "road",
                ),
            ),
        )
        assertEquals(
            expected = "未找到相关歌单",
            actual = resolveAddToPlaylistEmptyStateText(
                flow = AddToPlaylistFlowState(
                    songId = "song-1",
                    hasAnyPlaylist = true,
                    playlistSearchQuery = "road",
                ),
            ),
        )
    }

    /**
     * 最近播放歌曲即使不在当前队列或完整曲库缓存中，也要能打开既有更多面板。
     */
    @Test
    fun resolveMorePanelSongFindsRecentPlayedSong(): Unit {
        val recentSong: Song = testSong(
            id = "recent-song",
            title = "Recent Song",
        )
        val state: MusicAppUiState = testState().copy(
            recentSongs = listOf(recentSong),
        )

        val resolvedSong: Song? = resolveMorePanelSong(
            state = state,
            songId = "recent-song",
        )

        assertEquals(expected = recentSong, actual = resolvedSong)
    }

    /**
     * 不存在的歌曲不展示更多面板，避免空 id 打开错误歌曲操作。
     */
    @Test
    fun resolveMorePanelSongReturnsNullForUnknownSong(): Unit {
        val state: MusicAppUiState = testState().copy(
            recentSongs = listOf(testSong(id = "recent-song", title = "Recent Song")),
        )

        val resolvedSong: Song? = resolveMorePanelSong(
            state = state,
            songId = "missing-song",
        )

        assertNull(actual = resolvedSong)
    }

    // 构造最小 App 状态，让测试只关注更多面板的歌曲解析顺序。
    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }

    // 构造最近播放歌曲实体，避免测试依赖仓库或 demo catalog。
    private fun testSong(
        id: String,
        title: String,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
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
}
