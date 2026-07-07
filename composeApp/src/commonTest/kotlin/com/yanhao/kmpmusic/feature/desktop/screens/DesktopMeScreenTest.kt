package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面“我的”页测试，锁住个人中心静态入口、统计区、扫描入口和最近播放摘要展示语义。
 */
class DesktopMeScreenTest {
    /**
     * 桌面最近播放摘要为空时仍显示查看全部入口和轻量空态，避免内容区留白。
     */
    @Test
    fun desktopMeRecentPlayedSummaryShowsEmptyState(): Unit {
        val model: DesktopMeRecentPlayedSummaryDisplayModel = buildDesktopMeRecentPlayedSummaryDisplayModel(
            recentSongs = emptyList(),
        )

        assertEquals(expected = "最近播放", actual = model.title)
        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertTrue(actual = model.isActionEnabled)
        assertTrue(actual = model.rows.isEmpty())
        assertTrue(actual = model.emptyMessage.contains(other = "最近听过的音乐"))
    }

    /**
     * 桌面摘要只露出统一最近播放列表的前三首，完整列表由 workspace 最近播放页承载。
     */
    @Test
    fun desktopMeRecentPlayedSummaryKeepsOnlyTopThreeSongs(): Unit {
        val songs: List<Song> = (1..5).map { index: Int ->
            testSong(id = "song-$index", title = "Song $index")
        }

        val model: DesktopMeRecentPlayedSummaryDisplayModel = buildDesktopMeRecentPlayedSummaryDisplayModel(
            recentSongs = songs,
        )

        assertEquals(
            expected = listOf("song-1", "song-2", "song-3"),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.song.id },
        )
    }

    /**
     * 摘要展示模型只使用调用方传入的过滤后列表，不自行回退到 demo、全库或陈旧历史。
     */
    @Test
    fun desktopMeRecentPlayedSummaryUsesProvidedFilteredSongsOnly(): Unit {
        val model: DesktopMeRecentPlayedSummaryDisplayModel = buildDesktopMeRecentPlayedSummaryDisplayModel(
            recentSongs = listOf(testSong(id = "filtered-real-song", title = "Real Song")),
        )

        assertEquals(
            expected = listOf("filtered-real-song"),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.song.id },
        )
        assertEquals(expected = "Real Song", actual = model.rows.single().title)
    }

    /**
     * 当前切片只提供查看全部入口，不把摘要标题动作混成歌曲更多菜单。
     */
    @Test
    fun desktopMeRecentPlayedSummaryKeepsViewAllSeparateFromSongActions(): Unit {
        val model: DesktopMeRecentPlayedSummaryDisplayModel = buildDesktopMeRecentPlayedSummaryDisplayModel(
            recentSongs = listOf(testSong(id = "song-1", title = "Song 1")),
        )

        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertFalse(actual = model.actionLabel.contains(other = "更多"))
        assertFalse(actual = model.actionLabel.contains(other = "..."))
        assertTrue(actual = model.isActionEnabled)
    }

    /**
     * 桌面“我的”页设置菜单只显示三行静态入口，不能携带导航启用语义。
     */
    @Test
    fun desktopMeStaticSettingsMenuShowsThreeNonNavigatingRows(): Unit {
        val items: List<DesktopMeStaticSettingsMenuItemDisplayModel> = buildDesktopMeStaticSettingsMenuItemDisplayModels()
        assertEquals(
            expected = listOf("存储管理", "主题与外观", "关于"),
            actual = items.map { item: DesktopMeStaticSettingsMenuItemDisplayModel -> item.title },
        )
        assertEquals(
            expected = listOf(false, false, false),
            actual = items.map { item: DesktopMeStaticSettingsMenuItemDisplayModel -> item.isNavigationEnabled },
        )
    }

    /**
     * 桌面“我的”页必须显示扫描音乐入口，并把入口语义标记为桌面扫描动作。
     */
    @Test
    fun desktopMeQuickActionsExposeScanMusicEntry(): Unit {
        val actions: List<DesktopMeQuickActionDisplayModel> = buildDesktopMeQuickActionDisplayModels()
        val action: DesktopMeQuickActionDisplayModel = actions.single()
        assertEquals(expected = 1, actual = actions.size)
        assertEquals(
            expected = DesktopMeQuickAction.ScanMusic,
            actual = action.action,
        )
        assertEquals(
            expected = "扫描音乐",
            actual = action.title,
        )
        assertEquals(
            expected = "添加文件夹",
            actual = action.actionLabel,
        )
    }

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
            sourceKind = LocalMusicSourceKind.DesktopFolder,
            localUri = "file:///$id.mp3",
        )
    }
}
