package com.yanhao.kmpmusic.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 本地音频扫描模型测试，保护平台来源标识和稳定 key 规则。
 */
class LocalMusicModelsTest {
    /**
     * sourceKey 必须由来源类型和来源 id 组成，重新扫描后才能保留收藏和队列引用。
     */
    @Test
    fun metadataBuildsStableSourceKey(): Unit {
        val metadata: MusicFileMetadata = MusicFileMetadata(
            sourceId = "42",
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = "content://media/external/audio/media/42",
            fileName = "river.flac",
            title = "海边的梦",
            artist = "旅行团乐队",
            album = "似水流年",
            durationMs = 225_000L,
            mimeType = "audio/flac",
            sizeBytes = 24_000_000L,
            modifiedAt = 1_719_360_000_000L,
            coverArt = CoverArt.CoverSeaDream,
        )

        assertEquals(
            expected = "androidMediaStore:42",
            actual = metadata.sourceKey,
        )
    }

    /**
     * Song 必须携带 scanner 生成的 localUri，UI 和播放链路不能自行拼接。
     */
    @Test
    fun songExposesScannerGeneratedLocalUri(): Unit {
        val song: Song = Song(
            id = "androidMediaStore:42",
            title = "海边的梦",
            artist = "旅行团乐队",
            album = "似水流年",
            duration = "3:45",
            coverArt = CoverArt.CoverSeaDream,
            isLiked = false,
            lastPlayed = "未播放",
            quality = "本地 FLAC",
            lyric = "海风吹过窗边，像一段刚醒来的旋律。",
            trackNumber = 1,
            durationMs = 225_000L,
            sourceId = "42",
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = "content://media/external/audio/media/42",
            mimeType = "audio/flac",
            sizeBytes = 24_000_000L,
            modifiedAt = 1_719_360_000_000L,
        )

        assertEquals(
            expected = true,
            actual = song.isPlayable,
        )
        assertEquals(
            expected = "content://media/external/audio/media/42",
            actual = song.localUri,
        )
    }
}
