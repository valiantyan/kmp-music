package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata

/**
 * fake 本地音乐目录，集中生成普通演示歌曲和收藏压力测试歌曲。
 */
internal object FakeLocalMusicDemoCatalog {
    /** 根据目标数量生成 fake 扫描元数据。 */
    fun buildMetadata(demoSongCount: Int): List<MusicFileMetadata> {
        val effectiveDemoSongCount: Int = demoSongCount.coerceAtLeast(
            minimumValue = BASE_DEMO_SONG_COUNT,
        )
        val baseSongs: List<MusicFileMetadata> = buildBaseMetadata()
        if (effectiveDemoSongCount == BASE_DEMO_SONG_COUNT) {
            return baseSongs
        }
        return baseSongs + buildStressMetadata(demoSongCount = effectiveDemoSongCount)
    }

    /** 生成与 fake 歌曲一一对应的收藏 id 集合。 */
    fun buildFavoriteSongIds(demoSongCount: Int): Set<String> {
        val effectiveDemoSongCount: Int = demoSongCount.coerceAtLeast(
            minimumValue = BASE_DEMO_SONG_COUNT,
        )
        return (1..effectiveDemoSongCount)
            .map { index: Int -> "fakeScanner:${formatSourceId(index = index)}" }
            .toSet()
    }

    // 保留最初 8 首有代表性的演示歌曲，避免既有搜索和播放用例丢失锚点。
    private fun buildBaseMetadata(): List<MusicFileMetadata> {
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

    // 为收藏页滑动和增删压力测试补齐到 500 首，时间戳低于基础歌曲以稳定首页预览。
    private fun buildStressMetadata(demoSongCount: Int): List<MusicFileMetadata> {
        return (BASE_DEMO_SONG_COUNT + 1..demoSongCount).map { index: Int ->
            metadata(
                sourceId = formatSourceId(index = index),
                title = "收藏压力测试 ${formatSourceId(index = index)}",
                artist = "Demo Artist ${((index - 1) % 20) + 1}",
                album = "收藏压力专辑 ${((index - 1) % 40) + 1}",
                durationMs = 180_000L + ((index % 120) * 1_000L),
                mimeType = stressMimeType(index = index),
                coverArt = stressCoverArt(index = index),
                modifiedAt = 1_782_042_400_000L - (index * 60_000L),
            )
        }
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

    // 按序号轮换 MIME，避免 500 条数据在音质文案上完全相同。
    private fun stressMimeType(index: Int): String {
        return when (index % 3) {
            0 -> "audio/flac"
            1 -> "audio/mpeg"
            else -> "audio/aac"
        }
    }

    // 按序号轮换封面兜底，真实无封面时合并层仍会统一成占位资源。
    private fun stressCoverArt(index: Int): CoverArt {
        val coverArts: List<CoverArt> = CoverArt.entries
        return coverArts[index % coverArts.size]
    }

    // fake scanner 的 sourceId 保持三位数，便于快照和日志人工核对。
    private fun formatSourceId(index: Int): String {
        return index.toString().padStart(length = 3, padChar = '0')
    }

    /** 最初固定演示歌曲数量。 */
    private const val BASE_DEMO_SONG_COUNT: Int = 8
}
