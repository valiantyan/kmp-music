package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面最近播放页测试，锁住完整列表、空态和当前切片的动作边界。
 */
class DesktopRecentPlayedScreenTest {
    /**
     * 空列表时必须显示清晰空态，避免 workspace 只剩标题。
     */
    @Test
    fun desktopRecentPlayedPageShowsClearEmptyState() {
        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs = emptyList(),
            )

        assertEquals(expected = "最近播放", actual = model.title)
        assertEquals(expected = "暂无最近播放", actual = model.emptyTitle)
        assertTrue(actual = model.emptyDetail.contains(other = "最近听过的音乐"))
        assertTrue(actual = model.rows.isEmpty())
    }

    /**
     * 桌面完整页展示调用方传入的全部歌曲，不复用“我的”页摘要 Top3 截断。
     */
    @Test
    fun desktopRecentPlayedPageKeepsEveryProvidedSong() {
        val songs: List<Song> =
            (1..5).map { index: Int ->
                testSong(
                    id = "song-$index",
                    title = "Song $index",
                )
            }

        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs = songs,
            )

        assertEquals(
            expected = listOf("song-1", "song-2", "song-3", "song-4", "song-5"),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.song.id },
        )
        assertEquals(
            expected = listOf("1", "2", "3", "4", "5"),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.indexLabel },
        )
    }

    /**
     * 展示模型只使用统一过滤后的入参，不自行读取历史、全库或 demo 数据。
     */
    @Test
    fun desktopRecentPlayedPageUsesProvidedFilteredSongsOnly() {
        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs = listOf(testSong(id = "filtered-song", title = "Filtered Song")),
            )

        assertEquals(
            expected = listOf("filtered-song"),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.song.id },
        )
        assertEquals(expected = "Filtered Song", actual = model.rows.single().title)
    }

    /**
     * 当前页面不是播放日志管理页，不能暴露清空、编辑、筛选、排序或审计语义。
     */
    @Test
    fun desktopRecentPlayedPageDoesNotExposeManagementActions() {
        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs = listOf(testSong(id = "song-1", title = "Song 1")),
            )

        assertFalse(actual = model.hasManagementActions)
        assertFalse(actual = model.eyebrow.contains(other = "清空"))
        assertFalse(actual = model.eyebrow.contains(other = "排序"))
        assertFalse(actual = model.eyebrow.contains(other = "筛选"))
        assertFalse(actual = model.eyebrow.contains(other = "审计"))
    }

    /**
     * 桌面完整页歌曲行本轮接入播放和单曲更多入口，且不新增管理动作。
     */
    @Test
    fun desktopRecentPlayedPageRowsExposePlaybackAndMoreActions() {
        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs =
                    listOf(
                        testSong(id = "song-1", title = "Song 1"),
                        testSong(id = "song-2", title = "Song 2"),
                    ),
            )

        assertEquals(
            expected = listOf(true, true),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.hasPlaybackAction },
        )
        assertEquals(
            expected = listOf(true, true),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.hasMoreAction },
        )
        assertFalse(actual = model.hasManagementActions)
    }

    /**
     * 桌面完整页只给全局当前歌曲行附加播放中标识，非当前行保持普通状态。
     */
    @Test
    fun desktopRecentPlayedPageMarksOnlyCurrentSong() {
        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs =
                    listOf(
                        testSong(id = "song-1", title = "Song 1"),
                        testSong(id = "song-2", title = "Song 2"),
                        testSong(id = "song-3", title = "Song 3"),
                    ),
                currentSongId = "song-3",
            )

        assertEquals(
            expected = listOf(false, false, true),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.isCurrentSong },
        )
        assertEquals(
            expected = listOf(null, null, "播放中"),
            actual = model.rows.map { row: DesktopRecentPlayedSongDisplayModel -> row.playingIndicatorLabel },
        )
    }

    /**
     * 桌面页明确使用 workspace 表格策略，避免退回手机稿窄列表。
     */
    @Test
    fun desktopRecentPlayedPageUsesWorkspaceTableLayout() {
        val model: DesktopRecentPlayedPageDisplayModel =
            buildDesktopRecentPlayedPageDisplayModel(
                songs = listOf(testSong(id = "song-1", title = "Song 1")),
            )

        assertEquals(
            expected = DesktopRecentPlayedLayoutPolicy.WorkspaceTable,
            actual = model.layoutPolicy,
        )
    }

    // 构造已过滤的桌面可播放歌曲，避免测试依赖真实扫描或播放历史解析。
    private fun testSong(
        id: String,
        title: String,
    ): Song =
        Song(
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
