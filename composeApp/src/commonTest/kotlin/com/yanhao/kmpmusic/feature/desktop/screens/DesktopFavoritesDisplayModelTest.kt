package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.data.InMemoryMusicLibraryRepository
import com.yanhao.kmpmusic.domain.model.CoverArt
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

    /** 第一首收藏歌曲带真实封面时，顶部应直接使用该歌曲。 */
    @Test
    fun firstCoveredSongBecomesHeroArtwork(): Unit =
        runTest {
            val songs: List<Song> = buildSongsWithoutCovers(count = 3)
            val firstSong: Song = songs.first().copy(coverImageUri = "file:///covers/first.jpg")

            val model: DesktopFavoritesDisplayModel = buildDesktopFavoritesDisplayModel(songs = listOf(firstSong) + songs.drop(n = 1))

            assertEquals(expected = firstSong.id, actual = model.heroArtworkSong?.id)
        }

    /** 前序歌曲没有真实封面时，应继续查找后续第一首带封面的歌曲。 */
    @Test
    fun skipsSongsWithoutCoversWhenSelectingHeroArtwork(): Unit =
        runTest {
            val songs: List<Song> = buildSongsWithoutCovers(count = 4)
            val coveredSong: Song = songs[2].copy(coverImageUri = "file:///covers/third.jpg")
            val inputSongs: List<Song> = songs.toMutableList().apply { set(index = 2, element = coveredSong) }

            val model: DesktopFavoritesDisplayModel = buildDesktopFavoritesDisplayModel(songs = inputSongs)

            assertEquals(expected = coveredSong.id, actual = model.heroArtworkSong?.id)
        }

    /** 所有收藏歌曲都没有真实封面时，顶部应返回空选择以显示默认心形封面。 */
    @Test
    fun songsWithoutCoversKeepDefaultHeroArtwork(): Unit =
        runTest {
            val songs: List<Song> = buildSongsWithoutCovers(count = 3)

            val model: DesktopFavoritesDisplayModel = buildDesktopFavoritesDisplayModel(songs = songs)

            assertEquals(expected = null, actual = model.heroArtworkSong)
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

/** 把测试歌曲统一成无真实封面的状态，避免 fake 曲库细节影响选择规则。 */
private suspend fun buildSongsWithoutCovers(count: Int): List<Song> =
    buildFavoriteSongs(count = count).map { song: Song ->
        song.copy(
            coverArt = CoverArt.HeroLocalMusic,
            coverImageUri = null,
        )
    }
