package com.yanhao.kmpmusic.feature.desktop.player

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 桌面播放页队列行测试，覆盖播放页旋转展示与真实队列下标之间的映射。
 */
class DesktopPlayerQueueRowsTest {
    /** 点击旋转展示后的队列行时，必须仍然跳到共享队列中的原始下标。 */
    @Test
    fun rotatedQueueRowsKeepOriginalQueueIndex(): Unit {
        val songs: List<Song> = (1..4).map { index: Int -> testSong(index = index) }
        val rows: List<DesktopPlayerQueueRowState> = buildPlayerQueueRowStates(
            song = songs[2],
            queueSongs = songs,
        )
        assertEquals(
            expected = listOf("song-3", "song-4", "song-1", "song-2"),
            actual = rows.map { row: DesktopPlayerQueueRowState -> row.song.id },
        )
        assertEquals(
            expected = listOf(2, 3, 0, 1),
            actual = rows.map { row: DesktopPlayerQueueRowState -> row.queueIndex },
        )
    }

    /** 当前歌曲不在队列时，展示顺序回退为原队列顺序并保留原始下标。 */
    @Test
    fun missingCurrentSongKeepsOriginalQueueOrder(): Unit {
        val songs: List<Song> = (1..3).map { index: Int -> testSong(index = index) }
        val rows: List<DesktopPlayerQueueRowState> = buildPlayerQueueRowStates(
            song = testSong(index = 9),
            queueSongs = songs,
        )
        assertEquals(
            expected = listOf("song-1", "song-2", "song-3"),
            actual = rows.map { row: DesktopPlayerQueueRowState -> row.song.id },
        )
        assertEquals(
            expected = listOf(0, 1, 2),
            actual = rows.map { row: DesktopPlayerQueueRowState -> row.queueIndex },
        )
    }

    // 只填充队列映射所需字段，避免测试被无关歌曲元数据牵动。
    private fun testSong(index: Int): Song {
        return Song(
            id = "song-$index",
            title = "Song $index",
            artist = "Artist",
            album = "Album",
            duration = "03:00",
            coverArt = CoverArt.AlbumRiverYear,
            isLiked = false,
            lastPlayed = "",
            quality = "FLAC",
            lyric = "",
            trackNumber = index,
            durationMs = 180_000L,
            localUri = "file:///song-$index.mp3",
            modifiedAt = index.toLong(),
        )
    }
}
