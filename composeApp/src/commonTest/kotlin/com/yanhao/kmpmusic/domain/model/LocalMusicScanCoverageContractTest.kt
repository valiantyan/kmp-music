package com.yanhao.kmpmusic.domain.model

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 扫描覆盖契约测试，要求 scanner 显式声明完成覆盖范围，而不是让仓库按来源类型猜测删除权。
 */
class LocalMusicScanCoverageContractTest {
    /**
     * 无完成覆盖声明的结果只能提供正向发现，不能因为发现了同类来源歌曲就获得删除权。
     */
    @Test
    fun positiveOnlyResultHasNoDeletionAuthority(): Unit {
        val result: LocalMusicScanResult = LocalMusicScanResult(
            discovered = listOf(metadata(sourceId = "new", sourceKind = LocalMusicSourceKind.DesktopFolder)),
            sourceSummaries = listOf(
                sourceSummary(sourceKind = LocalMusicSourceKind.DesktopFolder, displayName = "Folder B"),
            ),
            completedCoverage = emptyList(),
        )

        assertTrue(actual = result.completedCoverage.isEmpty())
        assertEquals(expected = LocalMusicScanDeletionAuthority.None, actual = result.deletionAuthority)
    }

    /**
     * 完成覆盖必须由独立契约表达，不能从 [LocalMusicScanResult.discovered] 或 [LocalMusicScanResult.sourceSummaries] 推导。
     */
    @Test
    fun completedCoverageIsExplicitAndNotInferredFromDiscoveredOrSourceSummaries(): Unit {
        val result: LocalMusicScanResult = LocalMusicScanResult(
            discovered = listOf(metadata(sourceId = "android-1", sourceKind = LocalMusicSourceKind.AndroidMediaStore)),
            sourceSummaries = listOf(
                sourceSummary(sourceKind = LocalMusicSourceKind.AndroidMediaStore, displayName = "Android 媒体库"),
            ),
            completedCoverage = emptyList(),
        )

        assertEquals(expected = LocalMusicSourceKind.AndroidMediaStore, actual = result.discovered.single().sourceKind)
        assertEquals(expected = LocalMusicSourceKind.AndroidMediaStore, actual = result.sourceSummaries.single().sourceKind)
        assertEquals(expected = LocalMusicScanDeletionAuthority.None, actual = result.deletionAuthority)
    }

    /**
     * 失败结果可以携带正向发现，但不能消费完成覆盖来下线未处理旧歌。
     */
    @Test
    fun failedResultHasNoDeletionAuthorityEvenWithCompletedCoverage(): Unit {
        val result: LocalMusicScanResult = LocalMusicScanResult(
            discovered = listOf(metadata(sourceId = "android-new", sourceKind = LocalMusicSourceKind.AndroidMediaStore)),
            failed = listOf(failedProblem(sourceId = "android-bad")),
            completedCoverage = listOf(
                LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.AndroidMediaStore),
            ),
        )

        assertEquals(expected = LocalMusicScanDeletionAuthority.None, actual = result.deletionAuthority)
    }

    /**
     * 覆盖契约要区分完整来源类型、具体来源和 positive-only 三类语义，供后续合并逻辑安全消费。
     */
    @Test
    fun coverageModelDistinguishesSourceKindConcreteSourceAndPositiveOnly(): Unit {
        val androidCoverage: LocalMusicScanCoverage = LocalMusicScanCoverage.SourceKind(
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
        )
        val desktopFolderCoverage: LocalMusicScanCoverage = LocalMusicScanCoverage.ConcreteSource(
            sourceKind = LocalMusicSourceKind.DesktopFolder,
            sourceId = "folder:/Music/B",
        )
        val positiveOnlyCoverage: LocalMusicScanCoverage = LocalMusicScanCoverage.PositiveOnly

        assertContentEquals(
            expected = listOf(
                LocalMusicScanCoverageLevel.SourceKind,
                LocalMusicScanCoverageLevel.ConcreteSource,
                LocalMusicScanCoverageLevel.PositiveOnly,
            ),
            actual = listOf(
                androidCoverage.level,
                desktopFolderCoverage.level,
                positiveOnlyCoverage.level,
            ),
        )
    }

    /**
     * fake scanner 与平台 scanner 都必须在结果边界声明覆盖语义，仓库不能替 scanner 猜测。
     */
    @Test
    fun scannersMustDeclareCoverageSemanticsAtResultBoundary(): Unit {
        val fakeResult: LocalMusicScanResult = LocalMusicScanResult(
            discovered = listOf(metadata(sourceId = "fake-1", sourceKind = LocalMusicSourceKind.FakeScanner)),
            completedCoverage = listOf(
                LocalMusicScanCoverage.SourceKind(sourceKind = LocalMusicSourceKind.FakeScanner),
            ),
        )
        val platformImportResult: LocalMusicScanResult = LocalMusicScanResult(
            discovered = listOf(metadata(sourceId = "ios-1", sourceKind = LocalMusicSourceKind.IosImportedFile)),
            completedCoverage = listOf(LocalMusicScanCoverage.PositiveOnly),
        )

        assertEquals(
            expected = LocalMusicScanDeletionAuthority.CoveredSourcesOnly,
            actual = fakeResult.deletionAuthority,
        )
        assertEquals(
            expected = LocalMusicScanDeletionAuthority.None,
            actual = platformImportResult.deletionAuthority,
        )
    }

    // 构造平台无关元数据，让测试只约束公共扫描结果契约。
    private fun metadata(
        sourceId: String,
        sourceKind: LocalMusicSourceKind,
    ): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = sourceKind,
            localUri = "test://local-audio/$sourceId",
            fileName = "$sourceId.flac",
            title = "契约测试歌曲",
            artist = "契约测试歌手",
            album = "契约测试专辑",
            durationMs = 180_000L,
            mimeType = "audio/flac",
            sizeBytes = 1_000L,
            modifiedAt = 1L,
            coverArt = CoverArt.HeroLocalMusic,
        )
    }

    // 构造来源摘要，证明摘要存在也不等于完成覆盖。
    private fun sourceSummary(
        sourceKind: LocalMusicSourceKind,
        displayName: String,
    ): LocalMusicSourceSummary {
        return LocalMusicSourceSummary(
            sourceKind = sourceKind,
            displayName = displayName,
            songCount = 1,
            problemCount = 0,
            lastScannedAt = 1L,
        )
    }

    // 构造失败项，证明 problem 存在时结果只能作为 positive-only 合并。
    private fun failedProblem(sourceId: String): LocalMusicProblem {
        return LocalMusicProblem(
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            sourceId = sourceId,
            fileName = "$sourceId.flac",
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.FileUnreadable,
                message = "扫描失败",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
                sourceId = sourceId,
            ),
        )
    }
}
