package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 我的页测试，锁住最近播放摘要真实输入、Top3 截断和范围边界。
 */
class MeScreenTest {
    /**
     * 最近播放摘要为空时继续展示固定标题、查看全部占位和轻量空态。
     */
    @Test
    fun recentPlayedSummaryDisplayModelShowsEmptySkeleton(): Unit {
        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
            recentSongs = emptyList(),
        )

        assertEquals(expected = "最近播放", actual = model.title)
        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertTrue(actual = model.songs.isEmpty())
        assertTrue(actual = model.emptyMessage.contains(other = "最近听过的音乐"))
    }

    /**
     * 摘要只显示统一最近播放列表的前 3 条，避免把完整历史塞进“我的”页。
     */
    @Test
    fun recentPlayedSummaryDisplayModelKeepsOnlyTopThreeSongs(): Unit {
        val songs: List<Song> = (1..5).map { index: Int ->
            testSong(id = "song-$index", title = "Song $index")
        }

        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
            recentSongs = songs,
        )

        assertEquals(
            expected = listOf("song-1", "song-2", "song-3"),
            actual = model.songs.map { song: Song -> song.id },
        )
    }

    /**
     * 展示模型只使用调用方传入的过滤后列表，不自行回退到 demo、全库或陈旧历史。
     */
    @Test
    fun recentPlayedSummaryDisplayModelUsesProvidedFilteredSongsOnly(): Unit {
        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
            recentSongs = listOf(testSong(id = "filtered-real-song", title = "Real Song")),
        )

        assertEquals(
            expected = listOf("filtered-real-song"),
            actual = model.songs.map { song: Song -> song.id },
        )
        assertEquals(expected = "Real Song", actual = model.songs.single().title)
    }

    /**
     * 查看全部只启用最近播放页导航，歌曲行的三点更多入口与标题区保持分离。
     */
    @Test
    fun recentPlayedSummaryDisplayModelKeepsViewAllSeparateFromMoreAction(): Unit {
        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
            recentSongs = listOf(testSong(id = "song-1", title = "Song 1")),
        )

        assertTrue(actual = model.isActionEnabled)
        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertFalse(actual = model.actionLabel.contains(other = "更多"))
        assertFalse(actual = model.actionLabel.contains(other = "..."))
        assertFalse(actual = model.emptyMessage.contains(other = "播放队列"))
        assertTrue(actual = model.songRows.single().hasMoreAction)
    }

    /**
     * 摘要展示模型只把当前播放标识给命中的可见歌曲，避免普通行误变红或误显示播放中。
     */
    @Test
    fun recentPlayedSummaryDisplayModelMarksOnlyCurrentVisibleSong(): Unit {
        val songs: List<Song> = (1..4).map { index: Int ->
            testSong(id = "song-$index", title = "Song $index")
        }

        val model: RecentPlayedSummaryDisplayModel = buildRecentPlayedSummaryDisplayModel(
            recentSongs = songs,
            currentSongId = "song-2",
        )

        assertEquals(
            expected = listOf(false, true, false),
            actual = model.songRows.map { row: RecentPlayedSongRowDisplayModel -> row.isCurrentSong },
        )
        assertEquals(
            expected = listOf("song-1", "song-2", "song-3"),
            actual = model.songRows.map { row: RecentPlayedSongRowDisplayModel -> row.song.id },
        )
    }

    // 构造已过滤的可播放歌曲，避免测试依赖 demo catalog 或全库扫描。
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
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = "content://$id",
        )
    }
}
