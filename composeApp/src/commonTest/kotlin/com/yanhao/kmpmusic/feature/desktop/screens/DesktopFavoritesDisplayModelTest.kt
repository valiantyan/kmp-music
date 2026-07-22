package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.Song
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 收藏页显示模型测试，锁定动态数量、播放按钮和空态规则。
 */
class DesktopFavoritesDisplayModelTest {
    /** 有收藏歌曲时文案必须包含真实数量，两个播放入口均可用。 */
    @Test
    fun populatedFavoritesShowDynamicCountAndEnabledActions(): Unit =
        runTest {
            val songs: List<Song> = buildFavoriteSongs(count = 8)

            val model: DesktopFavoritesDisplayModel = buildDesktopFavoritesDisplayModel(songs = songs)

            assertEquals(expected = "8 首歌曲", actual = model.songCountLabel)
            assertEquals(expected = "播放全部 (8)", actual = model.playAllLabel)
            assertTrue(actual = model.isPlaybackEnabled)
            assertEquals(expected = null, actual = model.emptyMessage)
        }

    /** 空收藏页保留页面骨架，但播放入口不可触发。 */
    @Test
    fun emptyFavoritesDisableActionsAndShowMessage() {
        val model: DesktopFavoritesDisplayModel = buildDesktopFavoritesDisplayModel(songs = emptyList())

        assertEquals(expected = "0 首歌曲", actual = model.songCountLabel)
        assertEquals(expected = "播放全部 (0)", actual = model.playAllLabel)
        assertFalse(actual = model.isPlaybackEnabled)
        assertEquals(expected = "暂无收藏歌曲", actual = model.emptyMessage)
    }
}

/** 使用 fake scanner 的默认收藏集合生成真实收藏歌曲。 */
private suspend fun buildFavoriteSongs(count: Int): List<Song> {
    val scanner = FakeLocalMusicScanner(demoSongCount = count)
    val libraryRepository = InMemoryMusicLibraryRepository()
    libraryRepository.applyScanResult(
        request = LocalMusicScanRequest.Refresh,
        scanResult = scanner.scan(request = LocalMusicScanRequest.Refresh),
        likedSongIds = scanner.demoFavoriteSongIds(),
    )
    return libraryRepository.getHomePreview(limit = count)
}
