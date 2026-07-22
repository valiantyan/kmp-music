package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面搜索结果显示规则测试，避免 pending 防抖阶段误报“没有找到”。
 */
class DesktopSearchScreenDisplayModelTest {
    @Test
    fun pendingQueryKeepsDesktopResultsHidden() {
        assertFalse(actual = shouldShowDesktopSearchResults(query = "雨", activeQuery = ""))
        assertFalse(actual = shouldShowDesktopSearchResults(query = "雨声", activeQuery = "雨"))
    }

    @Test
    fun matchingActiveQueryShowsDesktopResults() {
        assertTrue(actual = shouldShowDesktopSearchResults(query = "雨", activeQuery = "雨"))
    }

    /** 新建搜索从歌曲开始，Desktop 歌单 Tab 使用全量范围保留本地歌单投影。 */
    @Test
    fun initialTabMapsSongsAndPlaylistsToTheirDedicatedScopes() {
        assertEquals(
            expected = DesktopSearchResultTab.Playlists,
            actual = initialDesktopSearchResultTab(scope = SearchScope.All),
        )
        assertEquals(
            expected = DesktopSearchResultTab.Songs,
            actual = initialDesktopSearchResultTab(scope = SearchScope.Songs),
        )
    }

    /** 歌单只按名称进行大小写无关匹配，空关键词不应泄漏完整歌单列表。 */
    @Test
    fun playlistSearchMatchesNamesWithoutReturningAllOnBlankQuery() {
        val playlists: List<LocalPlaylistCardDisplayModel> =
            listOf(
                testPlaylist(id = "playlist-1", name = "雨天通勤"),
                testPlaylist(id = "playlist-2", name = "深夜电台"),
            )
        assertEquals(
            expected = listOf("playlist-1"),
            actual =
                filterDesktopSearchPlaylists(
                    query = "雨天",
                    playlists = playlists,
                ).map { playlist: LocalPlaylistCardDisplayModel -> playlist.id },
        )
        assertTrue(
            actual =
                filterDesktopSearchPlaylists(
                    query = "   ",
                    playlists = playlists,
                ).isEmpty(),
        )
    }

    /** 无命中反馈应携带当前 Tab，避免用户误解为其它分类也没有结果。 */
    @Test
    fun noResultTitleNamesTheSelectedTab() {
        assertEquals(
            expected = "未找到与“夜航”相关的歌单",
            actual =
                desktopSearchNoResultTitle(
                    tab = DesktopSearchResultTab.Playlists,
                    query = "夜航",
                ),
        )
    }

    /** 构造稳定歌单投影，测试只关心名称匹配而不依赖仓库状态。 */
    private fun testPlaylist(
        id: String,
        name: String,
    ): LocalPlaylistCardDisplayModel =
        LocalPlaylistCardDisplayModel(
            id = id,
            name = name,
            availableSongCount = 0,
            coverArt = CoverArt.HeroLocalMusic,
            coverImageUri = null,
        )
}
