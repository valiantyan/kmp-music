package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner

/**
 * 显式 fake scanner，只用于 common 阶段验证 UI 与数据链路。
 */
class FakeLocalMusicScanner(
    private val demoSongCount: Int = DEFAULT_DEMO_SONG_COUNT,
) : LocalMusicScanner {
    /** 返回真实形态的扫描元数据，不复用 seed repository 冒充平台扫描。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scan(
            request = request,
            preferences = LocalMusicDiscoveryPreferences(),
        )
    }

    /** 按本地音频发现偏好返回 fake 元数据，便于 common 测试覆盖过滤链路。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        val songs: List<MusicFileMetadata> = FakeLocalMusicDemoCatalog.buildMetadata(
            demoSongCount = demoSongCount,
        ).filter { metadata: MusicFileMetadata ->
            LocalAudioFileRules.shouldIncludeByDuration(
                durationMs = metadata.durationMs,
                preferences = preferences,
            )
        }
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

    /** 返回 fake 数据默认收藏 id，专供 common 演示和压力测试链路使用。 */
    fun demoFavoriteSongIds(): Set<String> {
        return FakeLocalMusicDemoCatalog.buildFavoriteSongIds(demoSongCount = demoSongCount)
    }

    companion object {
        /** 收藏页压力测试默认歌曲数。 */
        const val DEFAULT_DEMO_SONG_COUNT: Int = 500
    }
}
