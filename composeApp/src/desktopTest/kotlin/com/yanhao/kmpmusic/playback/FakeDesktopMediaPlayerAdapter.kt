package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class FakeDesktopMediaPlayerAdapter : DesktopMediaPlayerAdapter {
    private val eventChannel = Channel<DesktopMediaPlayerEvent>(capacity = Channel.UNLIMITED)

    private var positionMs: Long = 0L
    private var durationMs: Long? = null
    private var volumePercent: Int = 100

    val commands: MutableList<String> = mutableListOf()

    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    override suspend fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        positionMs = startPositionMs
        commands += "prepare:$songId:$mediaUri:$generation:$startPositionMs"
    }

    override suspend fun play(generation: Long) {
        commands += "play:$generation"
    }

    override suspend fun pause(generation: Long) {
        commands += "pause:$generation"
    }

    override suspend fun seekTo(
        generation: Long,
        positionMs: Long,
    ) {
        this.positionMs = positionMs
        commands += "seek:$generation:$positionMs"
    }

    override suspend fun stop(generation: Long) {
        positionMs = 0L
        commands += "stop:$generation"
    }

    override suspend fun currentPositionMs(): Long {
        return positionMs
    }

    override suspend fun currentDurationMs(): Long? {
        return durationMs
    }

    override suspend fun setVolume(volumePercent: Int) {
        this.volumePercent = volumePercent
        commands += "volume:$volumePercent"
    }

    override suspend fun release() {
        commands += "release"
    }

    fun emitPrepared(
        generation: Long,
        durationMs: Long?,
    ) {
        this.durationMs = durationMs
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Prepared(
                generation = generation,
                durationMs = durationMs,
            ),
        )
    }

    fun emitPlaying(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        this.positionMs = positionMs
        this.durationMs = durationMs
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Playing(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    fun emitPaused(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        this.positionMs = positionMs
        this.durationMs = durationMs
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Paused(
                generation = generation,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    fun emitFinished(generation: Long) {
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Finished(
                generation = generation,
            ),
        )
    }

    fun emitFailure(
        generation: Long,
        error: PlaybackError,
    ) {
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Failed(
                generation = generation,
                error = error,
            ),
        )
    }
}
