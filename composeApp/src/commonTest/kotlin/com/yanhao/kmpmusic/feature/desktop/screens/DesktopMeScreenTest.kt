package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.LibraryStats
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 桌面“我的”页测试，锁住个人中心静态入口、统计区和扫描入口展示语义。
 */
class DesktopMeScreenTest {
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
}
