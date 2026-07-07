package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 最近播放页测试，锁住空态语义和完整列表展示规则。
 */
class RecentPlayedScreenTest {
    /**
     * 空列表时必须说明最近播放来自真实播放行为，避免用户把空态理解成加载失败。
     */
    @Test
    fun emptyRecentPlayedDisplayModelExplainsPlaybackCreatesHistory(): Unit {
        val model: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(songs = emptyList())

        assertTrue(actual = model.songs.isEmpty())
        assertEquals(expected = "暂无最近播放", actual = model.emptyTitle)
        assertTrue(actual = model.emptyDetail.contains(other = "播放歌曲后才会产生最近播放记录"))
    }

    /**
     * 最近播放页展示调用方传入的完整过滤后列表，不复用摘要区 Top3 截断规则。
     */
    @Test
    fun nonEmptyRecentPlayedDisplayModelKeepsEveryProvidedSong(): Unit {
        val songs: List<Song> = (1..5).map { index: Int ->
            testSong(
                id = "song-$index",
                title = "Song $index",
            )
        }

        val model: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(
            songs = songs,
        )

        assertEquals(
            expected = listOf("song-1", "song-2", "song-3", "song-4", "song-5"),
            actual = model.songs.map { song: Song -> song.id },
        )
    }

    /**
     * 展示模型只保留统一过滤后的入参，不自行扫描全库、拼 demo 或解析历史项。
     */
    @Test
    fun recentPlayedDisplayModelUsesProvidedFilteredSongsOnly(): Unit {
        val model: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(
            songs = listOf(testSong(id = "filtered-song", title = "Filtered Song")),
        )

        assertEquals(
            expected = listOf("filtered-song"),
            actual = model.songs.map { song: Song -> song.id },
        )
        assertEquals(expected = "Filtered Song", actual = model.songs.single().title)
    }

    // 构造最小歌曲实体，让展示模型测试只关注最近播放页文案。
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
            coverArt = CoverArt.CoverSeaDream,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = 1,
            durationMs = 180_000L,
        )
    }
}
