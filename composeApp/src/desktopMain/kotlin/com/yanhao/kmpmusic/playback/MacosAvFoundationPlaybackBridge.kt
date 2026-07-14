package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * macOS AVFoundation bridge 初始化状态。
 */
sealed interface MacosAvFoundationBridgeInitialization {
    /** JVM 已加载 dylib 且 native session 创建成功。 */
    data object Success : MacosAvFoundationBridgeInitialization

    /**
     * bridge 初始化失败。
     *
     * @property error 已归一化的播放错误。
     */
    data class Failed(
        val error: PlaybackError,
    ) : MacosAvFoundationBridgeInitialization
}

/**
 * macOS AVFoundation bridge，把 JNI/native 回调转成 [ApplePlaybackBridge] 契约事件。
 */
internal class MacosAvFoundationPlaybackBridge(
    // native 事件通道，用于把 callback 重新暴露为 Flow。
    private val eventChannel: Channel<ApplePlaybackBridgeEvent>,
    // native session，初始化失败时为 null。
    private val session: MacosAvFoundationNativeBridgeSession?,
    /** bridge 初始化结果，供 smoke 和上层诊断读取。 */
    val initialization: MacosAvFoundationBridgeInitialization,
) : ApplePlaybackBridge {
    // release 后 callback 会检查该标记并丢弃延迟 native 事件。
    @Volatile
    private var isReleased: Boolean = false

    /** 当前 bridge 是否已经拥有可用 native session。 */
    val isInitialized: Boolean
        get() = initialization is MacosAvFoundationBridgeInitialization.Success && session != null

    override val events: Flow<ApplePlaybackBridgeEvent> = eventChannel.receiveAsFlow()

    /** 判断 native callback 当前是否仍可进入事件流。 */
    internal fun canEmitCallbacks(): Boolean {
        return !isReleased
    }

    /** 准备当前媒体，初始化失败时直接返回失败 ack。 */
    override suspend fun prepare(request: ApplePlaybackBridgePrepareRequest): ApplePlaybackBridgeCommandAck {
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return failedAck(error = unavailableError(songId = request.songId))
        return ackFromStatus(
            status = activeSession.prepare(
                songId = request.songId,
                mediaUri = request.mediaUri,
                generation = request.generation,
                startPositionMs = request.startPositionMs,
            ),
            songId = request.songId,
        )
    }

    /** 下发播放命令。 */
    override suspend fun play(generation: Long): ApplePlaybackBridgeCommandAck {
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return failedAck(error = unavailableError(songId = null))
        return ackFromStatus(status = activeSession.play(generation = generation), songId = null)
    }

    /** 下发暂停命令。 */
    override suspend fun pause(generation: Long): ApplePlaybackBridgeCommandAck {
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return failedAck(error = unavailableError(songId = null))
        return ackFromStatus(status = activeSession.pause(generation = generation), songId = null)
    }

    /** 下发 seek 命令。 */
    override suspend fun seekTo(request: ApplePlaybackBridgeSeekRequest): ApplePlaybackBridgeCommandAck {
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return failedAck(error = unavailableError(songId = null))
        return ackFromStatus(
            status = activeSession.seekTo(generation = request.generation, positionMs = request.positionMs),
            songId = null,
        )
    }

    /** 下发 stop 命令。 */
    override suspend fun stop(generation: Long): ApplePlaybackBridgeCommandAck {
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return failedAck(error = unavailableError(songId = null))
        return ackFromStatus(status = activeSession.stop(generation = generation), songId = null)
    }

    /** 设置 native 播放器音量。 */
    override suspend fun setVolume(volume: Float): ApplePlaybackBridgeCommandAck {
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return failedAck(error = unavailableError(songId = null))
        return ackFromStatus(status = activeSession.setVolume(volume = volume), songId = null)
    }

    /** 释放 native session，并阻止 release 后的延迟 callback 外泄。 */
    override suspend fun release(): ApplePlaybackBridgeCommandAck {
        if (isReleased) {
            return ApplePlaybackBridgeCommandAck.Accepted
        }
        isReleased = true
        val activeSession: MacosAvFoundationNativeBridgeSession = session
            ?: return ApplePlaybackBridgeCommandAck.Accepted
        return ackFromStatus(status = activeSession.release(), songId = null)
    }

    /** 把 native 命令状态码映射成 bridge ack。 */
    private fun ackFromStatus(status: Int, songId: String?): ApplePlaybackBridgeCommandAck {
        if (status == MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED) {
            return ApplePlaybackBridgeCommandAck.Accepted
        }
        return failedAck(error = errorFromStatus(status = status, songId = songId))
    }

    /** 生成失败 ack。 */
    private fun failedAck(error: PlaybackError): ApplePlaybackBridgeCommandAck {
        return ApplePlaybackBridgeCommandAck.Failed(error = error)
    }

    /** 根据 native 状态码生成播放错误。 */
    private fun errorFromStatus(status: Int, songId: String?): PlaybackError {
        val type: PlaybackErrorType = when (status) {
            MACOS_AVFOUNDATION_NATIVE_STATUS_MISSING_FILE -> PlaybackErrorType.MissingFile
            MACOS_AVFOUNDATION_NATIVE_STATUS_UNSUPPORTED_FORMAT -> PlaybackErrorType.UnsupportedFormat
            MACOS_AVFOUNDATION_NATIVE_STATUS_PERMISSION_DENIED -> PlaybackErrorType.PermissionDenied
            MACOS_AVFOUNDATION_NATIVE_STATUS_ENGINE_UNAVAILABLE -> PlaybackErrorType.EngineUnavailable
            else -> PlaybackErrorType.Unknown
        }
        return PlaybackError(
            type = type,
            songId = songId,
            message = "macOS AVFoundation bridge 命令失败：$type",
        )
    }

    /** 构造 bridge 不可用错误。 */
    private fun unavailableError(songId: String?): PlaybackError {
        return PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = songId,
            message = "macOS AVFoundation bridge 不可用",
        )
    }

    companion object {
        /** 创建生产 bridge，默认加载 JNI dylib。 */
        fun create(
            libraryLoader: MacosAvFoundationNativeLibraryLoader = SystemMacosAvFoundationNativeLibraryLoader,
            sessionFactory: MacosAvFoundationNativeBridgeSessionFactory =
                JniMacosAvFoundationNativeBridgeSessionFactory,
        ): MacosAvFoundationPlaybackBridge {
            return MacosAvFoundationPlaybackBridgeFactory.create(
                libraryLoader = libraryLoader,
                sessionFactory = sessionFactory,
            )
        }
    }
}
