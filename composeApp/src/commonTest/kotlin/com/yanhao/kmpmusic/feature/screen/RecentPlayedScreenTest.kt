package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 最近播放页骨架测试，锁住当前切片的空态语义和范围边界。
 */
class RecentPlayedScreenTest {
    /**
     * 空列表时必须说明最近播放来自真实播放行为，避免用户把空态理解成加载失败。
     */
    @Test
    fun emptyRecentPlayedDisplayModelExplainsPlaybackCreatesHistory(): Unit {
        val model: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(songs = emptyList())

        assertEquals(expected = "暂无最近播放", actual = model.title)
        assertTrue(actual = model.detail.contains(other = "播放歌曲后才会产生最近播放记录"))
    }

    /**
     * 非空列表在本切片只给出占位提示，不能提前承诺完整列表、队列或更多菜单已经完成。
     */
    @Test
    fun nonEmptyRecentPlayedDisplayModelKeepsFullListOutOfScope(): Unit {
        val model: RecentPlayedPageDisplayModel = buildRecentPlayedPageDisplayModel(
            songs = listOf(testSong()),
        )

        assertEquals(expected = "最近播放记录已准备好", actual = model.title)
        assertTrue(actual = model.detail.contains(other = "后续切片"))
        assertFalse(actual = model.detail.contains(other = "播放队列"))
        assertFalse(actual = model.detail.contains(other = "更多菜单"))
    }

    // 构造最小歌曲实体，让展示模型测试只关注最近播放页文案。
    private fun testSong(): Song {
        return Song(
            id = "song-1",
            title = "Song",
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
