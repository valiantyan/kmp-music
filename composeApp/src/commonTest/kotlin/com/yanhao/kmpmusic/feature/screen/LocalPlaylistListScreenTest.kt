package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.feature.app.LocalPlaylistCardDisplayModel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 本地歌单列表页的展示规则回归测试。
 */
class LocalPlaylistListScreenTest {
    /**
     * 顶部摘要必须使用真实歌单数量，避免继续展示旧排序文案。
     */
    @Test
    fun buildLocalPlaylistCountSummaryShowsPlaylistCount() {
        val playlists: List<LocalPlaylistCardDisplayModel> =
            listOf(
                testPlaylistCard(id = "playlist-1"),
                testPlaylistCard(id = "playlist-2"),
                testPlaylistCard(id = "playlist-3"),
            )

        val summary: String = buildLocalPlaylistCountSummary(playlists = playlists)

        assertEquals(expected = "共 3 个歌单", actual = summary)
    }

    /**
     * 空态防御路径也应复用数量摘要，方便后续入口策略变化时保持头部一致。
     */
    @Test
    fun buildLocalPlaylistCountSummarySupportsEmptyList() {
        val summary: String = buildLocalPlaylistCountSummary(playlists = emptyList())

        assertEquals(expected = "共 0 个歌单", actual = summary)
    }
}

// 测试只关心歌单数量摘要，卡片其它字段保持最小有效值。
private fun testPlaylistCard(id: String): LocalPlaylistCardDisplayModel =
    LocalPlaylistCardDisplayModel(
        id = id,
        name = "测试歌单",
        availableSongCount = 0,
        coverArt = CoverArt.HeroLocalMusic,
        coverImageUri = null,
    )
