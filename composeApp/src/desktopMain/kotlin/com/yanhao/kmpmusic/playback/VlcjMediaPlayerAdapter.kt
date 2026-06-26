package com.yanhao.kmpmusic.playback

import com.sun.jna.NativeLibrary
import java.io.File
import java.net.URI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent

/**
 * vlcj 音频适配器，负责把桌面端真实播放回调转换为统一事件流。
 */
class VlcjMediaPlayerAdapter(
    private val runtimePath: MacosLibVlcRuntimePath?,
) : DesktopMediaPlayerAdapter {
    private val eventChannel: Channel<DesktopMediaPlayerEvent> = Channel(capacity = Channel.UNLIMITED)
    private val component: CallbackMediaPlayerComponent? = runtimePath?.let { resolvedRuntime ->
        NativeLibrary.addSearchPath(
            RuntimeUtil.getLibVlcLibraryName(),
            resolvedRuntime.libraryDirectory,
        )
        setLibVlcPluginPath(pluginDirectory = resolvedRuntime.pluginDirectory)
        CallbackMediaPlayerComponent(
            "--no-video",
        )
    }
    private var currentListener: MediaPlayerEventAdapter? = null

    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    override suspend fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        val mediaPlayer: MediaPlayer = component?.mediaPlayer() ?: run {
            eventChannel.trySend(
                DesktopMediaPlayerEvent.Failed(
                    generation = generation,
                    error = buildEngineUnavailableError(songId = songId),
                ),
            )
            return
        }
        val media: String = mediaUri.toVlcjMediaLocation()
        val accepted: Boolean = mediaPlayer.media().prepare(media)
        if (!accepted) {
            eventChannel.trySend(
                DesktopMediaPlayerEvent.Failed(
                    generation = generation,
                    error = mediaUri.toPlaybackError(songId = songId),
                ),
            )
            return
        }
        val snapshot: VlcjMediaCallbackSnapshot = VlcjMediaCallbackSnapshot(
            generation = generation,
            songId = songId,
        )
        replaceMediaListener(
            mediaPlayer = mediaPlayer,
            snapshot = snapshot,
        )
        if (startPositionMs > 0L) {
            mediaPlayer.controls().setTime(startPositionMs)
        }
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Prepared(
                generation = generation,
                durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
            ),
        )
    }

    override suspend fun play(generation: Long) {
        component?.mediaPlayer()?.controls()?.play()
    }

    override suspend fun pause(generation: Long) {
        val mediaPlayer: MediaPlayer = component?.mediaPlayer() ?: return
        mediaPlayer.controls().pause()
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Paused(
                generation = generation,
                positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
            ),
        )
    }

    override suspend fun seekTo(
        generation: Long,
        positionMs: Long,
    ) {
        component?.mediaPlayer()?.controls()?.setTime(positionMs)
    }

    override suspend fun stop(generation: Long) {
        component?.mediaPlayer()?.controls()?.stop()
    }

    override suspend fun currentPositionMs(): Long {
        val mediaPlayer: MediaPlayer = component?.mediaPlayer() ?: return 0L
        return currentPositionMsValue(mediaPlayer = mediaPlayer)
    }

    override suspend fun currentDurationMs(): Long? {
        val mediaPlayer: MediaPlayer = component?.mediaPlayer() ?: return null
        return currentDurationMsValue(mediaPlayer = mediaPlayer)
    }

    override suspend fun release() {
        val mediaPlayer: MediaPlayer = component?.mediaPlayer() ?: return
        currentListener?.let { listener ->
            mediaPlayer.events().removeMediaPlayerEventListener(listener)
        }
        currentListener = null
        component.release()
    }

    private fun currentPositionMsValue(mediaPlayer: MediaPlayer): Long {
        return mediaPlayer.status().time().coerceAtLeast(minimumValue = 0L)
    }

    private fun currentDurationMsValue(mediaPlayer: MediaPlayer): Long? {
        return mediaPlayer.status().length().takeIf { duration: Long -> duration > 0L }
    }

    private fun String.toVlcjMediaLocation(): String {
        if (startsWith(prefix = "file:")) {
            return File(URI(this)).absolutePath
        }
        return this
    }

    private fun replaceMediaListener(
        mediaPlayer: MediaPlayer,
        snapshot: VlcjMediaCallbackSnapshot,
    ) {
        currentListener?.let { listener ->
            mediaPlayer.events().removeMediaPlayerEventListener(listener)
        }
        val listener: MediaPlayerEventAdapter = object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                eventChannel.trySend(
                    snapshot.playing(
                        positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                        durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
                    ),
                )
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                eventChannel.trySend(
                    snapshot.paused(
                        positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                        durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
                    ),
                )
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                eventChannel.trySend(snapshot.finished())
            }

            override fun error(mediaPlayer: MediaPlayer) {
                eventChannel.trySend(snapshot.failed())
            }
        }
        currentListener = listener
        mediaPlayer.events().addMediaPlayerEventListener(listener)
    }
}

// macOS 的 LibVLC 3 不再接受 `--plugin-path`，原生发现逻辑会读取 [VLC_PLUGIN_PATH]。
private fun setLibVlcPluginPath(pluginDirectory: String): Unit {
    val result: Int = NativeLibrary.getInstance("c")
        .getFunction("setenv")
        .invokeInt(arrayOf("VLC_PLUGIN_PATH", pluginDirectory, 1))
    if (result != 0) {
        throw IllegalStateException("无法设置 VLC_PLUGIN_PATH，LibVLC 插件目录不可用。")
    }
}

/**
 * LibVLC 运行时不可用时的占位适配器，负责把缺失运行时收敛为确定性的播放器不可用错误。
 */
class UnavailableDesktopMediaPlayerAdapter : DesktopMediaPlayerAdapter {
    private val eventChannel: Channel<DesktopMediaPlayerEvent> = Channel(capacity = Channel.UNLIMITED)

    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    override suspend fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Failed(
                generation = generation,
                error = buildEngineUnavailableError(songId = songId),
            ),
        )
    }

    override suspend fun play(generation: Long) = Unit

    override suspend fun pause(generation: Long) = Unit

    override suspend fun seekTo(
        generation: Long,
        positionMs: Long,
    ) = Unit

    override suspend fun stop(generation: Long) = Unit

    override suspend fun currentPositionMs(): Long = 0L

    override suspend fun currentDurationMs(): Long? = null

    override suspend fun release() = Unit
}
