package com.yanhao.kmpmusic.feature.desktop.player

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 桌面播放页队列行测试，覆盖播放页稳定展示顺序与真实队列下标之间的映射。
 */
class DesktopPlayerQueueRowsTest {
    /** 当前歌曲变化后，播放页队列仍按共享队列原始顺序展示。 */
    @Test
    fun currentSongChangeKeepsOriginalQueueOrder() {
        val songs: List<Song> = (1..4).map { index: Int -> testSong(index = index) }
        val rows: List<DesktopPlayerQueueRowState> =
            buildPlayerQueueRowStates(
                queueSongs = songs,
            )
        assertEquals(
            expected = listOf("song-1", "song-2", "song-3", "song-4"),
            actual = rows.map { row: DesktopPlayerQueueRowState -> row.song.id },
        )
        assertEquals(
            expected = listOf(0, 1, 2, 3),
            actual = rows.map { row: DesktopPlayerQueueRowState -> row.queueIndex },
        )
    }

    /** 队列行始终保留共享队列下标，点击时才能切到正确歌曲。 */
    @Test
    fun queueRowsKeepOriginalQueueIndex() {
        val songs: List<Song> = (1..3).map { index: Int -> testSong(index = index) }
        val rows: List<DesktopPlayerQueueRowState> =
            buildPlayerQueueRowStates(
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
    private fun testSong(index: Int): Song =
        Song(
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
