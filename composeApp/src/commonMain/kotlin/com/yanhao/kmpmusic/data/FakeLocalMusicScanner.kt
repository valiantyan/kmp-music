package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner

/**
 * 显式 fake scanner，只用于 common 阶段验证 UI 与数据链路。
 */
class FakeLocalMusicScanner : LocalMusicScanner {
    /** 返回真实形态的扫描元数据，不复用 seed repository 冒充平台扫描。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        val songs: List<MusicFileMetadata> = buildFakeMetadata()
        return LocalMusicScanResult(
            discovered = songs,
            sourceSummaries = listOf(
                LocalMusicSourceSummary(
                    sourceKind = LocalMusicSourceKind.FakeScanner,
                    displayName = LocalMusicSourceKind.FakeScanner.displayName,
                    songCount = songs.size,
                    problemCount = 0,
                    lastScannedAt = 1_782_043_200_000L,
                ),
            ),
            completedAt = 1_782_043_200_000L,
        )
    }

    // 构造带 sourceKind/sourceId/localUri/modifiedAt 的演示扫描数据。
    private fun buildFakeMetadata(): List<MusicFileMetadata> {
        return listOf(
            metadata(
                sourceId = "001",
                title = "海边的梦",
                artist = "旅行团乐队",
                album = "似水流年",
                durationMs = 225_000L,
                mimeType = "audio/flac",
                coverArt = CoverArt.CoverSeaDream,
                modifiedAt = 1_782_043_200_000L,
            ),
            metadata(
                sourceId = "002",
                title = "Summer Waltz",
                artist = "久石让",
                album = "Dream Stories",
                durationMs = 265_000L,
                mimeType = "audio/aac",
                coverArt = CoverArt.CoverSummerWaltz,
                modifiedAt = 1_782_043_100_000L,
            ),
            metadata(
                sourceId = "003",
                title = "像水流年",
                artist = "旅行团乐队",
                album = "似水流年",
                durationMs = 238_000L,
                mimeType = "audio/flac",
                coverArt = CoverArt.AlbumRiverYear,
                modifiedAt = 1_782_043_000_000L,
            ),
            metadata(
                sourceId = "004",
                title = "The Best of Me",
                artist = "A-Lin",
                album = "The Best of Me",
                durationMs = 247_000L,
                mimeType = "audio/alac",
                coverArt = CoverArt.AlbumBestOfMe,
                modifiedAt = 1_782_042_900_000L,
            ),
            metadata(
                sourceId = "005",
                title = "时光森林",
                artist = "苏打绿",
                album = "时光森林",
                durationMs = 311_000L,
                mimeType = "audio/mpeg",
                coverArt = CoverArt.AlbumTimeForest,
                modifiedAt = 1_782_042_800_000L,
            ),
            metadata(
                sourceId = "006",
                title = "沿岸公路",
                artist = "旅行团乐队",
                album = "似水流年",
                durationMs = 251_000L,
                mimeType = "audio/flac",
                coverArt = CoverArt.AlbumRiverYear,
                modifiedAt = 1_782_042_700_000L,
            ),
            metadata(
                sourceId = "007",
                title = "小情歌",
                artist = "苏打绿",
                album = "时光森林",
                durationMs = 273_000L,
                mimeType = "audio/mpeg",
                coverArt = CoverArt.AlbumTimeForest,
                modifiedAt = 1_782_042_600_000L,
            ),
            metadata(
                sourceId = "008",
                title = "One Summer's Day",
                artist = "久石让",
                album = "Dream Stories",
                durationMs = 248_000L,
                mimeType = "audio/aac",
                coverArt = CoverArt.CoverSummerWaltz,
                modifiedAt = 1_782_042_500_000L,
            ),
        )
    }

    // 创建单首 fake 音频元数据，sourceId 与 localUri 保持一一对应。
    private fun metadata(
        sourceId: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        mimeType: String,
        coverArt: CoverArt,
        modifiedAt: Long,
    ): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.FakeScanner,
            localUri = "fake://local-audio/$sourceId",
            fileName = "$sourceId-${title.lowercase()}.audio",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mimeType = mimeType,
            sizeBytes = 24_000_000L,
            modifiedAt = modifiedAt,
            coverArt = coverArt,
        )
    }
}
