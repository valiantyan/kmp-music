package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.ui.geometry.Size
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.math.abs
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
    fun desktopMeRecentPlayedSummaryShowsEmptyState() {
        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
                recentSongs = emptyList(),
            )

        assertEquals(expected = "最近播放", actual = model.title)
        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertTrue(actual = model.isActionEnabled)
        assertTrue(actual = model.rows.isEmpty())
        assertTrue(actual = model.emptyMessage.contains(other = "最近听过的音乐"))
    }

    /**
     * 桌面摘要以 Figma 四卡网格露出统一最近播放列表的前四首，完整列表由 workspace 最近播放页承载。
     */
    @Test
    fun desktopMeRecentPlayedSummaryKeepsOnlyTopFourSongs() {
        val songs: List<Song> =
            (1..5).map { index: Int ->
                testSong(id = "song-$index", title = "Song $index")
            }

        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
                recentSongs = songs,
            )

        assertEquals(
            expected = listOf("song-1", "song-2", "song-3", "song-4"),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.song.id },
        )
    }

    /**
     * 摘要展示模型只使用调用方传入的过滤后列表，不自行回退到 demo、全库或陈旧历史。
     */
    @Test
    fun desktopMeRecentPlayedSummaryUsesProvidedFilteredSongsOnly() {
        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
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
    fun desktopMeRecentPlayedSummaryKeepsViewAllSeparateFromSongActions() {
        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
                recentSongs = listOf(testSong(id = "song-1", title = "Song 1")),
            )

        assertEquals(expected = "查看全部", actual = model.actionLabel)
        assertFalse(actual = model.actionLabel.contains(other = "更多"))
        assertFalse(actual = model.actionLabel.contains(other = "..."))
        assertTrue(actual = model.isActionEnabled)
    }

    /**
     * 桌面最近播放摘要歌曲行本轮接入播放和更多入口，但查看全部仍保持独立标题动作。
     */
    @Test
    fun desktopMeRecentPlayedSummaryRowsExposePlaybackAndMoreActions() {
        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
                recentSongs =
                    listOf(
                        testSong(id = "song-1", title = "Song 1"),
                        testSong(id = "song-2", title = "Song 2"),
                    ),
            )

        assertEquals(
            expected = listOf(true, true),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.hasPlaybackAction },
        )
        assertEquals(
            expected = listOf(true, true),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.hasMoreAction },
        )
        assertEquals(expected = "查看全部", actual = model.actionLabel)
    }

    /**
     * 桌面摘要只给当前播放歌曲附加播放中标识，避免 Top4 之外或非当前行误高亮。
     */
    @Test
    fun desktopMeRecentPlayedSummaryMarksOnlyCurrentVisibleSong() {
        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
                recentSongs =
                    listOf(
                        testSong(id = "song-1", title = "Song 1"),
                        testSong(id = "song-2", title = "Song 2"),
                        testSong(id = "song-3", title = "Song 3"),
                        testSong(id = "song-4", title = "Song 4"),
                    ),
                currentSongId = "song-2",
            )

        assertEquals(
            expected = listOf(false, true, false, false),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.isCurrentSong },
        )
        assertEquals(
            expected = listOf(null, "播放中", null, null),
            actual = model.rows.map { row: DesktopMeRecentPlayedSongDisplayModel -> row.playingIndicatorLabel },
        )
    }

    /**
     * 暂停后当前曲目仍可保留在播放器，但最近播放摘要不能继续显示播放中反馈。
     */
    @Test
    fun desktopMeRecentPlayedSummaryDoesNotMarkPausedSongAsPlaying() {
        val model: DesktopMeRecentPlayedSummaryDisplayModel =
            buildDesktopMeRecentPlayedSummaryDisplayModel(
                recentSongs = listOf(testSong(id = "song-1", title = "Song 1")),
                currentSongId = "song-1",
                isPlaying = false,
            )

        assertFalse(actual = model.rows.single().isCurrentSong)
        assertEquals(expected = null, actual = model.rows.single().playingIndicatorLabel)
    }

    /**
     * 桌面“我的”页设置菜单只显示三行静态入口，不能携带导航启用语义。
     */
    @Test
    fun desktopMeStaticSettingsMenuShowsThreeNonNavigatingRows() {
        val items: List<DesktopMeStaticSettingsMenuItemDisplayModel> = buildDesktopMeStaticSettingsMenuItemDisplayModels()
        assertEquals(
            expected = listOf("存储管理", "主题外观", "关于软件"),
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
    fun desktopMeQuickActionsExposeScanMusicEntry() {
        val actions: List<DesktopMeQuickActionDisplayModel> = buildDesktopMeQuickActionDisplayModels()
        val action: DesktopMeQuickActionDisplayModel = actions.single()
        assertEquals(expected = 1, actual = actions.size)
        assertEquals(
            expected = DesktopMeQuickAction.ScanMusic,
            actual = action.action,
        )
        assertEquals(
            expected = "扫描本地音乐",
            actual = action.title,
        )
        assertEquals(
            expected = "更新媒体库资源",
            actual = action.subtitle,
        )
    }

    /**
     * 桌面统计区只显示音乐、歌单和听歌时长三项，避免回退到旧的专辑/歌手/收藏/最近播放组合。
     */
    @Test
    fun desktopMeStatsUseSongPlaylistAndListeningHoursOnly() {
        val stats: LibraryStats =
            LibraryStats(
                songCount = 42,
                albumCount = 7,
                artistCount = 5,
            )

        val models: List<DesktopMeStatDisplayModel> =
            buildDesktopMeStatDisplayModels(
                libraryStats = stats,
                localPlaylistCount = 0,
            )

        assertEquals(
            expected = listOf("音乐", "创建歌单", "累计收听"),
            actual = models.map { model: DesktopMeStatDisplayModel -> model.title },
        )
        assertEquals(
            expected = listOf("SONGS", "PLAYLISTS", "DURATION"),
            actual = models.map { model: DesktopMeStatDisplayModel -> model.heading },
        )
        assertEquals(expected = 3, actual = models.size)
    }

    /**
     * 歌曲数和歌单数必须跟随真实状态变化，只有听歌时长继续保持静态展示。
     */
    @Test
    fun desktopMeStatsUseRealSongAndPlaylistCounts() {
        val stats: LibraryStats =
            LibraryStats(
                songCount = 128,
                albumCount = 9,
                artistCount = 4,
            )

        val values: List<String> =
            buildDesktopMeStatDisplayModels(
                libraryStats = stats,
                localPlaylistCount = 3,
            ).map { model: DesktopMeStatDisplayModel -> model.value }

        assertEquals(
            expected = listOf("128", "3", "365"),
            actual = values,
        )
    }

    /**
     * 音乐和歌单统计卡必须复用既有入口，听歌时长仍保持静态展示。
     */
    @Test
    fun desktopMeStatsExposeMusicAndPlaylistNavigationActions() {
        val stats: LibraryStats = LibraryStats(songCount = 7)

        val actions: List<DesktopMeStatAction?> =
            buildDesktopMeStatDisplayModels(
                libraryStats = stats,
                localPlaylistCount = 2,
            ).map { model: DesktopMeStatDisplayModel -> model.action }

        assertEquals(
            expected = listOf(DesktopMeStatAction.OpenHomeSongs, DesktopMeStatAction.OpenLocalPlaylists, null),
            actual = actions,
        )
    }

    /**
     * 资料横幅背景沿 Figma 的近垂直方向推进，避免 Compose 默认横向渐变形成色块。
     */
    @Test
    fun desktopMeProfileGradientUsesFigmaVerticalAngle() {
        val headerSize: Size = Size(width = 1_000f, height = 194f)
        val startpoint = DesktopMeFigmaTokens.profileBackgroundGradientStart(size = headerSize)
        val endpoint = DesktopMeFigmaTokens.profileBackgroundGradientEnd(size = headerSize)

        assertTrue(actual = abs(x = (startpoint.x + endpoint.x) / 2f - headerSize.width / 2f) < 0.001f)
        assertTrue(actual = abs(x = (startpoint.y + endpoint.y) / 2f - headerSize.height / 2f) < 0.001f)
        assertTrue(actual = endpoint.x - startpoint.x < headerSize.width * 0.1f)
        assertTrue(actual = endpoint.y - startpoint.y > headerSize.height)
    }

    // 构造已过滤的可播放歌曲，避免测试依赖 demo catalog 或全库扫描。
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
