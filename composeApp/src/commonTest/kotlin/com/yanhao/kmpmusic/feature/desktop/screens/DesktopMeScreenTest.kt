package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.LibraryStats
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 桌面“我的”页测试，锁住统计区的三项展示语义和真实歌曲数来源。
 */
class DesktopMeScreenTest {
    /**
     * 桌面统计区只显示歌曲、歌单和听歌时长三项，避免回退到旧的专辑/歌手/收藏/最近播放组合。
     */
    @Test
    fun desktopMeStatsUseSongPlaylistAndListeningHoursOnly(): Unit {
        val stats: LibraryStats = LibraryStats(
            songCount = 42,
            albumCount = 7,
            artistCount = 5,
        )

        val models: List<DesktopMeStatDisplayModel> = buildDesktopMeStatDisplayModels(
            libraryStats = stats,
        )

        assertEquals(
            expected = listOf("歌曲", "歌单", "听歌时长"),
            actual = models.map { model: DesktopMeStatDisplayModel -> model.title },
        )
        assertEquals(expected = 3, actual = models.size)
    }

    /**
     * 歌曲数必须跟随曲库统计变化，歌单和听歌时长保持当前 PRD 要求的静态展示。
     */
    @Test
    fun desktopMeStatsUseRealSongCountAndStaticPlaceholderValues(): Unit {
        val stats: LibraryStats = LibraryStats(
            songCount = 128,
            albumCount = 9,
            artistCount = 4,
        )

        val values: List<String> = buildDesktopMeStatDisplayModels(
            libraryStats = stats,
        ).map { model: DesktopMeStatDisplayModel -> model.value }

        assertEquals(
            expected = listOf("128", "12", "365"),
            actual = values,
        )
    }
}
