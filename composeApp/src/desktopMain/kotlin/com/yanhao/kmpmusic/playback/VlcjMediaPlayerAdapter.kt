package com.yanhao.kmpmusic.playback

import com.sun.jna.NativeLibrary
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import java.io.File
import java.net.URI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
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
    private val component: CallbackMediaPlayerComponent
    private var activeGeneration: Long = 0L
    private var activeSongId: String? = null

    override val events: Flow<DesktopMediaPlayerEvent> = eventChannel.receiveAsFlow()

    init {
        val resolvedRuntime: MacosLibVlcRuntimePath? = runtimePath
        val libVlcArgs: Array<String> = buildList {
            add("--no-video")
            if (resolvedRuntime != null) {
                add("--plugin-path=${resolvedRuntime.pluginDirectory}")
            }
        }.toTypedArray()
        if (resolvedRuntime != null) {
            NativeLibrary.addSearchPath(
                RuntimeUtil.getLibVlcLibraryName(),
                resolvedRuntime.libraryDirectory,
            )
        } else {
            NativeDiscovery().discover()
        }
        component = CallbackMediaPlayerComponent(*libVlcArgs)
        component.mediaPlayer().events().addMediaPlayerEventListener(
            object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Playing(
                            generation = activeGeneration,
                            positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                            durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
                        ),
                    )
                }

                override fun paused(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Paused(
                            generation = activeGeneration,
                            positionMs = currentPositionMsValue(mediaPlayer = mediaPlayer),
                            durationMs = currentDurationMsValue(mediaPlayer = mediaPlayer),
                        ),
                    )
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Finished(
                            generation = activeGeneration,
                        ),
                    )
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    eventChannel.trySend(
                        DesktopMediaPlayerEvent.Failed(
                            generation = activeGeneration,
                            error = PlaybackError(
                                type = PlaybackErrorType.Unknown,
                                songId = activeSongId,
                                message = "播放失败，已尝试播放下一首。",
                            ),
                        ),
                    )
                }
            },
        )
    }

    override suspend fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
        pluginPath: String?,
    ) {
        activeGeneration = generation
        activeSongId = songId
        val media: String = mediaUri.toVlcjMediaLocation()
        val mediaPlayer: MediaPlayer = component.mediaPlayer()
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
        activeGeneration = generation
        component.mediaPlayer().controls().play()
    }

    override suspend fun pause(generation: Long) {
        activeGeneration = generation
        component.mediaPlayer().controls().pause()
        eventChannel.trySend(
            DesktopMediaPlayerEvent.Paused(
                generation = generation,
                positionMs = currentPositionMs(),
                durationMs = currentDurationMs(),
            ),
        )
    }

    override suspend fun seekTo(
        generation: Long,
        positionMs: Long,
    ) {
        activeGeneration = generation
        component.mediaPlayer().controls().setTime(positionMs)
    }

    override suspend fun stop(generation: Long) {
        activeGeneration = generation
        component.mediaPlayer().controls().stop()
    }

    override suspend fun currentPositionMs(): Long {
        return currentPositionMsValue(mediaPlayer = component.mediaPlayer())
    }

    override suspend fun currentDurationMs(): Long? {
        return currentDurationMsValue(mediaPlayer = component.mediaPlayer())
    }

    override suspend fun release() {
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

    private fun String.toPlaybackError(songId: String): PlaybackError {
        val file: File? = runCatching { File(URI(this)) }.getOrNull()
        val type: PlaybackErrorType = when {
            file != null && !file.exists() -> PlaybackErrorType.MissingFile
            file != null && !file.canRead() -> PlaybackErrorType.PermissionDenied
            else -> PlaybackErrorType.Unknown
        }
        return PlaybackError(
            type = type,
            songId = songId,
            message = when (type) {
                PlaybackErrorType.MissingFile -> "文件不存在或已移动，请重新扫描本地音乐。"
                PlaybackErrorType.PermissionDenied ->
                    "无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。"
                PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持，已尝试播放下一首。"
                PlaybackErrorType.EngineUnavailable -> "播放器组件不可用，请重新安装应用或联系开发者。"
                PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首。"
            },
        )
    }
}
