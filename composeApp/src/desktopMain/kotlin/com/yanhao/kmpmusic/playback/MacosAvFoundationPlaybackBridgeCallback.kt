package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlinx.coroutines.channels.Channel

/**
 * native callback adapter，负责把 JNI 回调安全送入 [ApplePlaybackBridgeEvent] 通道。
 */
internal class MacosAvFoundationPlaybackBridgeCallback(
    // bridge 事件通道。
    private val eventChannel: Channel<ApplePlaybackBridgeEvent>,
    // release 后返回 false，用于丢弃延迟 native 回调。
    private val canEmit: () -> Boolean,
) : MacosAvFoundationNativeBridgeCallback {
    /** 回传 prepared 事件。 */
    override fun onPrepared(
        generation: Long,
        durationMs: Long,
        hasDuration: Boolean,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.Prepared(
                    generation = generation,
                    durationMs = durationMs.takeIf { hasDuration },
                ),
        )
    }

    /** 回传 buffering 事件。 */
    override fun onBuffering(
        generation: Long,
        positionMs: Long,
        durationMs: Long,
        hasDuration: Boolean,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.Buffering(
                    generation = generation,
                    positionMs = positionMs,
                    durationMs = durationMs.takeIf { hasDuration },
                ),
        )
    }

    /** 回传 playing 事件。 */
    override fun onPlaying(
        generation: Long,
        positionMs: Long,
        durationMs: Long,
        hasDuration: Boolean,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.Playing(
                    generation = generation,
                    positionMs = positionMs,
                    durationMs = durationMs.takeIf { hasDuration },
                ),
        )
    }

    /** 回传 paused 事件。 */
    override fun onPaused(
        generation: Long,
        positionMs: Long,
        durationMs: Long,
        hasDuration: Boolean,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.Paused(
                    generation = generation,
                    positionMs = positionMs,
                    durationMs = durationMs.takeIf { hasDuration },
                ),
        )
    }

    /** 回传 progress 事件。 */
    override fun onProgress(
        generation: Long,
        positionMs: Long,
        durationMs: Long,
        hasDuration: Boolean,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.Progress(
                    generation = generation,
                    positionMs = positionMs,
                    durationMs = durationMs.takeIf { hasDuration },
                ),
        )
    }

    /** 回传 ended 事件。 */
    override fun onEnded(generation: Long) {
        sendIfActive(event = ApplePlaybackBridgeEvent.Ended(generation = generation))
    }

    /** 回传 failed 事件。 */
    override fun onFailed(
        generation: Long,
        errorType: Int,
        songId: String?,
        message: String,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.Failed(
                    generation = generation,
                    error =
                        PlaybackError(
                            type = playbackErrorTypeOf(errorType = errorType),
                            songId = songId,
                            message = message,
                        ),
                ),
        )
    }

    /** 回传 initialization failed 事件。 */
    override fun onInitializationFailed(
        errorType: Int,
        message: String,
    ) {
        sendIfActive(
            event =
                ApplePlaybackBridgeEvent.InitializationFailed(
                    error =
                        PlaybackError(
                            type = playbackErrorTypeOf(errorType = errorType),
                            songId = null,
                            message = message,
                        ),
                ),
        )
    }

    /** release 后不再让 native 延迟事件进入 Kotlin 状态机。 */
    private fun sendIfActive(event: ApplePlaybackBridgeEvent) {
        if (!canEmit()) {
            return
        }
        eventChannel.trySend(element = event)
    }

    /** 将 native 显式错误码映射为共享错误类型，避免依赖 enum 顺序。 */
    private fun playbackErrorTypeOf(errorType: Int): PlaybackErrorType =
        when (errorType) {
            MACOS_AVFOUNDATION_NATIVE_ERROR_MISSING_FILE -> PlaybackErrorType.MissingFile
            MACOS_AVFOUNDATION_NATIVE_ERROR_UNSUPPORTED_FORMAT -> PlaybackErrorType.UnsupportedFormat
            MACOS_AVFOUNDATION_NATIVE_ERROR_PERMISSION_DENIED -> PlaybackErrorType.PermissionDenied
            MACOS_AVFOUNDATION_NATIVE_ERROR_ENGINE_UNAVAILABLE -> PlaybackErrorType.EngineUnavailable
            else -> PlaybackErrorType.Unknown
        }
}
