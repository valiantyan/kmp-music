package com.yanhao.kmpmusic.domain.playback

import com.yanhao.kmpmusic.data.FakeAudioPlayerEngine
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [FakeAudioPlayerEngine] 的状态事件测试，确保假实现和真实引擎契约对齐。
 */
class FakeAudioPlayerEngineTest {
    /**
     * [setQueue] 注入的起始进度必须被后续播放和暂停事件复用。
     */
    @Test
    fun playAndPauseReuseQueueStartPosition(): Unit = runBlocking {
        val engine = FakeAudioPlayerEngine()
        val eventsDeferred = async {
            engine.events.take(count = 4).toList()
        }
        yield()
        engine.setQueue(
            items = listOf(testMedia(songId = "song-1", durationMs = 180_000L)),
            startIndex = 0,
            startPositionMs = 12_345L,
        )
        engine.play()
        engine.pause()
        val events: List<PlaybackEngineEvent> = eventsDeferred.await()
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Loading,
                positionMs = 12_345L,
                durationMs = 180_000L,
            ),
            actual = events[1],
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Playing,
                positionMs = 12_345L,
                durationMs = 180_000L,
            ),
            actual = events[2],
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 12_345L,
                durationMs = 180_000L,
            ),
            actual = events[3],
        )
    }

    /**
     * [seekTo] 更新后的进度必须反映到后续状态事件里。
     */
    @Test
    fun pauseUsesLatestSeekPosition(): Unit = runBlocking {
        val engine = FakeAudioPlayerEngine()
        val eventsDeferred = async {
            engine.events.take(count = 4).toList()
        }
        yield()
        engine.setQueue(
            items = listOf(testMedia(songId = "song-1", durationMs = 180_000L)),
            startIndex = 0,
            startPositionMs = 0L,
        )
        engine.seekTo(positionMs = 54_321L)
        engine.pause()
        val events: List<PlaybackEngineEvent> = eventsDeferred.await()
        assertEquals(
            expected = PlaybackEngineEvent.ProgressChanged(
                positionMs = 54_321L,
                durationMs = 180_000L,
            ),
            actual = events[2],
        )
        assertEquals(
            expected = PlaybackEngineEvent.StatusChanged(
                status = PlaybackStatus.Paused,
                positionMs = 54_321L,
                durationMs = 180_000L,
            ),
            actual = events[3],
        )
    }

    /**
     * 构造可播放媒体，避免测试分散在无关字段上。
     */
    private fun testMedia(songId: String, durationMs: Long): PlayableMedia {
        return PlayableMedia(
            songId = songId,
            title = "Song $songId",
            artist = "Artist",
            album = "Album",
            durationMs = durationMs,
            localUri = "content://media/$songId",
            coverArt = CoverArt.HeroLocalMusic,
            mimeType = "audio/mpeg",
        )
    }
}
