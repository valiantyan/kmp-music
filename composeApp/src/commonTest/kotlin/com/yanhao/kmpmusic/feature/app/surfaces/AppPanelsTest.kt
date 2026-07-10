package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.feature.app.LocalPlaylistDetailDisplayModel
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SongMoreSourceContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 全局面板测试，确保各列表入口都复用同一个单曲更多面板解析逻辑。
 */
class AppPanelsTest {
    /**
     * 添加到歌单弹窗的关键视觉约束来自 Figma 节点 982:881，集中测试避免后续改动漂移。
     */
    @Test
    fun addToPlaylistDialogDesignSpecMatchesFigmaNode(): Unit {
        assertEquals(expected = 358.dp, actual = AddToPlaylistDialogDesignSpec.width)
        assertEquals(expected = 652.dp, actual = AddToPlaylistDialogDesignSpec.height)
        assertEquals(expected = 24.dp, actual = AddToPlaylistDialogDesignSpec.cornerRadius)
        assertEquals(expected = 68.dp, actual = AddToPlaylistDialogDesignSpec.headerHeight)
        assertEquals(expected = 40.dp, actual = AddToPlaylistDialogDesignSpec.playlistCoverSize)
        assertEquals(expected = 61.dp, actual = AddToPlaylistDialogDesignSpec.footerHeight)
    }

    /**
     * 小窗口下添加到歌单弹窗必须按窗口高度收敛，完整窗口下不超过设计稿高度。
     */
    @Test
    fun addToPlaylistDialogHeightRespectsViewportLimit(): Unit {
        assertEquals(
            expected = 540.dp,
            actual = AddToPlaylistDialogDesignSpec.resolveHeight(maxHeight = 600.dp),
        )
        assertEquals(
            expected = 652.dp,
            actual = AddToPlaylistDialogDesignSpec.resolveHeight(maxHeight = 1200.dp),
        )
        assertTrue(actual = AddToPlaylistDialogDesignSpec.footerHeight > AddToPlaylistDialogDesignSpec.footerActionHeight)
    }

    /**
     * 新建歌单弹窗的关键视觉约束来自 Figma 节点 974:672。
     */
    @Test
    fun createPlaylistDialogDesignSpecMatchesFigmaNode(): Unit {
        assertEquals(expected = 358.dp, actual = CreatePlaylistDialogDesignSpec.maxWidth)
        assertEquals(expected = 32.dp, actual = CreatePlaylistDialogDesignSpec.cornerRadius)
        assertEquals(expected = 33.dp, actual = CreatePlaylistDialogDesignSpec.contentPadding)
        assertEquals(expected = 56.dp, actual = CreatePlaylistDialogDesignSpec.inputHeight)
        assertEquals(expected = 56.dp, actual = CreatePlaylistDialogDesignSpec.buttonHeight)
        assertEquals(expected = 0.85f, actual = CreatePlaylistDialogDesignSpec.backgroundAlpha)
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
     * 歌单详情页歌曲即使尚未进入全量曲库缓存，也要能打开原有更多操作面板。
     */
    @Test
    fun resolveMorePanelSongFindsLocalPlaylistDetailSong(): Unit {
        val playlistSong: Song = testSong(
            id = "playlist-song",
            title = "Playlist Song",
        )
        val state: MusicAppUiState = testState().copy(
            selectedLocalPlaylistDetail = LocalPlaylistDetailDisplayModel(
                id = "playlist",
                name = "歌单",
                availableSongCount = 1,
                coverArt = CoverArt.HeroLocalMusic,
                coverImageUri = null,
                songs = listOf(playlistSong),
            ),
        )

        val resolvedSong: Song? = resolveMorePanelSong(
            state = state,
            songId = playlistSong.id,
        )

        assertEquals(expected = playlistSong, actual = resolvedSong)
    }

    /**
     * 歌单详情页更多面板保留原有歌曲操作，但不显示“添加到歌单”入口。
     */
    @Test
    fun localPlaylistDetailMorePanelHidesAddToPlaylistAction(): Unit {
        val state: MusicAppUiState = testState().copy(
            moreSongSourceContext = SongMoreSourceContext.LocalPlaylistDetail,
        )

        assertFalse(actual = canShowAddToPlaylistAction(state = state))
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
