package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 扫描结果合并测试，保护首页和全量曲库的共同数据来源。
 */
class MergeLocalMusicScanResultUseCaseTest {
    private val useCase: MergeLocalMusicScanResultUseCase = MergeLocalMusicScanResultUseCaseImpl()

    /**
     * 可播放歌曲应按 modifiedAt 降序进入快照，并保留收藏状态。
     */
    @Test
    fun mergeCreatesPlayableSnapshotSortedByModifiedTime(): Unit {
        val result: LocalMusicScanResult = LocalMusicScanResult(
            discovered = listOf(
                metadata(sourceId = "1", title = "旧歌", modifiedAt = 10L),
                metadata(sourceId = "2", title = "新歌", modifiedAt = 20L),
            ),
            completedAt = 30L,
        )

        val snapshot: LibrarySnapshot = useCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = LibrarySnapshot.Empty,
                scanResult = result,
                likedSongIds = setOf("fakeScanner:2"),
            ),
        )

        assertEquals(expected = listOf("新歌", "旧歌"), actual = snapshot.songs.map { song -> song.title })
        assertTrue(actual = snapshot.songs.first().isLiked)
        assertEquals(expected = 2, actual = snapshot.stats.songCount)
        assertEquals(expected = 1, actual = snapshot.stats.albumCount)
        assertEquals(expected = 1, actual = snapshot.stats.artistCount)
    }

    /**
     * 失败条目不能进入首页本地歌曲列表，只能出现在问题列表。
     */
    @Test
    fun mergeExcludesFailedEntriesFromSongs(): Unit {
        val failed: LocalMusicProblem = LocalMusicProblem(
            sourceKind = LocalMusicSourceKind.FakeScanner,
            sourceId = "bad",
            fileName = "broken.wav",
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.UnsupportedFormat,
                message = "格式不支持",
                sourceKind = LocalMusicSourceKind.FakeScanner,
                sourceId = "bad",
            ),
        )
        val snapshot: LibrarySnapshot = useCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = LibrarySnapshot.Empty,
                scanResult = LocalMusicScanResult(
                    discovered = listOf(metadata(sourceId = "ok", title = "可播放", modifiedAt = 20L)),
                    failed = listOf(failed),
                    completedAt = 40L,
                ),
                likedSongIds = emptySet(),
            ),
        )

        assertEquals(expected = listOf("可播放"), actual = snapshot.songs.map { song -> song.title })
        assertEquals(expected = 1, actual = snapshot.problems.size)
        assertFalse(actual = snapshot.songs.any { song -> song.sourceId == "bad" })
    }

    /**
     * 缺失元数据必须有稳定兜底，避免 UI 出现空标题或空专辑。
     */
    @Test
    fun mergeUsesMetadataFallbacks(): Unit {
        val snapshot: LibrarySnapshot = useCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = LibrarySnapshot.Empty,
                scanResult = LocalMusicScanResult(
                    discovered = listOf(
                        metadata(
                            sourceId = "3",
                            fileName = "untitled-track.mp3",
                            title = null,
                            artist = null,
                            album = null,
                            durationMs = null,
                            modifiedAt = 50L,
                        ),
                    ),
                    completedAt = 50L,
                ),
                likedSongIds = emptySet(),
            ),
        )

        val song: Song = snapshot.songs.single()
        assertEquals(expected = "untitled-track", actual = song.title)
        assertEquals(expected = "未知歌手", actual = song.artist)
        assertEquals(expected = "未知专辑", actual = song.album)
        assertEquals(expected = "--:--", actual = song.duration)
    }

    // 构造平台无关扫描元数据，让测试只关注合并规则。
    private fun metadata(
        sourceId: String,
        title: String? = "海边的梦",
        artist: String? = "旅行团乐队",
        album: String? = "似水流年",
        durationMs: Long? = 225_000L,
        fileName: String = "$sourceId.flac",
        modifiedAt: Long?,
    ): MusicFileMetadata {
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.FakeScanner,
            localUri = "fake://local-audio/$sourceId",
            fileName = fileName,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mimeType = "audio/flac",
            sizeBytes = 24_000_000L,
            modifiedAt = modifiedAt,
            coverArt = CoverArt.CoverSeaDream,
        )
    }
}
