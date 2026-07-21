package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackErrorType

internal class FakeMacosAvFoundationLibraryLoader(
    private val result: MacosAvFoundationNativeLibraryLoadResult,
) : MacosAvFoundationNativeLibraryLoader {
    /** 返回测试预设的加载结果，避免单元测试依赖真实 dylib。 */
    override fun load(): MacosAvFoundationNativeLibraryLoadResult = result
}

internal class FakeMacosAvFoundationSessionFactory : MacosAvFoundationNativeBridgeSessionFactory {
    // 最近一次创建出的 fake session，供测试主动触发 native 回调。
    lateinit var session: FakeMacosAvFoundationNativeBridgeSession

    /** 创建 fake native session，并保存 callback 作为测试事件入口。 */
    override fun create(
        callback: MacosAvFoundationNativeBridgeCallback,
    ): MacosAvFoundationNativeBridgeSessionCreation {
        session = FakeMacosAvFoundationNativeBridgeSession(callback = callback)
        return MacosAvFoundationNativeBridgeSessionCreation.Success(session = session)
    }
}

internal class FakeMacosAvFoundationNativeBridgeSession(
    private val callback: MacosAvFoundationNativeBridgeCallback,
) : MacosAvFoundationNativeBridgeSession {
    // fake session 是否已经释放。
    var isReleased: Boolean = false

    /** fake prepare 只返回 accepted，事件由测试手动驱动。 */
    override fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
    ): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED

    /** fake play 只返回 accepted。 */
    override fun play(generation: Long): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED

    /** fake pause 只返回 accepted。 */
    override fun pause(generation: Long): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED

    /** fake seek 只返回 accepted。 */
    override fun seekTo(
        generation: Long,
        positionMs: Long,
    ): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED

    /** fake stop 只返回 accepted。 */
    override fun stop(generation: Long): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED

    /** fake volume 只返回 accepted。 */
    override fun setVolume(volume: Float): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED

    /** fake release 标记已释放并返回 accepted。 */
    override fun release(): Int {
        isReleased = true
        return MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED
    }

    /** 手动回传 prepared 事件。 */
    fun emitPrepared(
        generation: Long,
        durationMs: Long?,
    ) {
        callback.onPrepared(
            generation = generation,
            durationMs = durationMs ?: 0L,
            hasDuration = durationMs != null,
        )
    }

    /** 手动回传 playing 事件。 */
    fun emitPlaying(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        callback.onPlaying(
            generation = generation,
            positionMs = positionMs,
            durationMs = durationMs ?: 0L,
            hasDuration = durationMs != null,
        )
    }

    /** 手动回传 progress 事件。 */
    fun emitProgress(
        generation: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        callback.onProgress(
            generation = generation,
            positionMs = positionMs,
            durationMs = durationMs ?: 0L,
            hasDuration = durationMs != null,
        )
    }

    /** 手动回传 ended 事件。 */
    fun emitEnded(generation: Long) {
        callback.onEnded(generation = generation)
    }

    /** 手动回传 failed 事件。 */
    fun emitFailed(
        generation: Long,
        errorType: PlaybackErrorType,
        songId: String?,
        message: String,
    ) {
        callback.onFailed(
            generation = generation,
            errorType = nativeErrorCodeOf(errorType = errorType),
            songId = songId,
            message = message,
        )
    }

    /** 将测试错误类型映射为 native 显式错误码。 */
    private fun nativeErrorCodeOf(errorType: PlaybackErrorType): Int =
        when (errorType) {
            PlaybackErrorType.MissingFile -> MACOS_AVFOUNDATION_NATIVE_ERROR_MISSING_FILE
            PlaybackErrorType.UnsupportedFormat -> MACOS_AVFOUNDATION_NATIVE_ERROR_UNSUPPORTED_FORMAT
            PlaybackErrorType.PermissionDenied -> MACOS_AVFOUNDATION_NATIVE_ERROR_PERMISSION_DENIED
            PlaybackErrorType.EngineUnavailable -> MACOS_AVFOUNDATION_NATIVE_ERROR_ENGINE_UNAVAILABLE
            PlaybackErrorType.Unknown -> MACOS_AVFOUNDATION_NATIVE_ERROR_UNKNOWN
        }
}
