package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证 common fake scanner 能提供收藏页压力测试所需的大数据集。
 */
class FakeLocalMusicScannerTest {
    /**
     * 默认 fake 数据必须达到 500 首，便于收藏页列表滑动和增删压力测试。
     */
    @Test
    fun scanBuildsFiveHundredDemoSongsForFavoritesStress(): Unit = runTest {
        val scanner = FakeLocalMusicScanner()

        val result = scanner.scan(request = LocalMusicScanRequest.Refresh)

        assertEquals(expected = 500, actual = result.discovered.size)
        assertEquals(expected = 500, actual = result.sourceSummaries.single().songCount)
        assertEquals(expected = 500, actual = scanner.demoFavoriteSongIds().size)
        assertTrue(
            actual = result.discovered
                .map { metadata -> metadata.sourceKey }
                .containsAll(elements = scanner.demoFavoriteSongIds()),
        )
    }
}
