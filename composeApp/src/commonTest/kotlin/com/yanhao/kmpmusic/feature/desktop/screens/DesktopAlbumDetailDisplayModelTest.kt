package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Desktop 专辑详情显示模型测试，固定曲序、播放文案和不可用专辑降级语义。 */
class DesktopAlbumDetailDisplayModelTest {
    /** 专辑详情必须沿用稳定曲序作为页面顺序和两种播放入口的完整队列。 */
    @Test
    fun buildsSortedAlbumQueueAndPlayAllLabel() {
        val album: Album = createAlbum()
        val displayModel: DesktopAlbumDetailDisplayModel =
            buildDesktopAlbumDetailDisplayModel(
                album = album,
                songs =
                    listOf(
                        createSong(id = "second", trackNumber = 2),
                        createSong(id = "other", trackNumber = 1, album = "其他专辑"),
                        createSong(id = "first", trackNumber = 1),
                    ),
            )
        assertEquals(expected = listOf("first", "second"), actual = displayModel.songs.map { song: Song -> song.id })
        assertEquals(expected = "播放全部 | 2首", actual = displayModel.playAllLabel)
        assertTrue(actual = displayModel.isPlaybackEnabled)
    }

    /** 缺失选择专辑时不建立新播放队列，保留安全的头部降级文案。 */
    @Test
    fun unavailableAlbumDisablesPlayback() {
        val displayModel: DesktopAlbumDetailDisplayModel =
            buildDesktopAlbumDetailDisplayModel(
                album = null,
                songs = listOf(createSong(id = "song", trackNumber = 1)),
            )
        assertEquals(expected = "专辑不可用", actual = displayModel.title)
        assertEquals(expected = "播放全部 | 0首", actual = displayModel.playAllLabel)
        assertFalse(actual = displayModel.isPlaybackEnabled)
    }

    /** 构造只含专辑详情判断所需字段的稳定专辑。 */
    private fun createAlbum(): Album =
        Album(
            id = "album:detail",
            title = "详情专辑",
            artist = "详情歌手",
            songCount = 2,
            coverArt = CoverArt.AlbumRiverYear,
            mood = "本地音乐",
            year = "2026",
        )

    /** 构造可播放的最小歌曲，便于验证专辑过滤和曲序规则。 */
    private fun createSong(
        id: String,
        trackNumber: Int,
        album: String = "详情专辑",
    ): Song =
        Song(
            id = id,
            title = id,
            artist = "详情歌手",
            album = album,
            duration = "03:20",
            coverArt = CoverArt.AlbumRiverYear,
            isLiked = false,
            lastPlayed = "",
            quality = "Lossless",
            lyric = "",
            trackNumber = trackNumber,
            localUri = "fake://$id",
        )
}
