package com.yanhao.kmpmusic.playback

/**
 * JNI native bridge session，持有 native handle 并把空 handle 映射成明确失败。
 */
internal class JniMacosAvFoundationNativeBridgeSession(
    // native bridge handle，只能由 JNI bindings 使用。
    private var handle: Long,
) : MacosAvFoundationNativeBridgeSession {
    /** 准备当前媒体。 */
    override fun prepare(
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
    ): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.prepare(
            handle = activeHandle,
            songId = songId,
            mediaUri = mediaUri,
            generation = generation,
            startPositionMs = startPositionMs,
        )
    }

    /** 播放当前媒体。 */
    override fun play(generation: Long): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.play(handle = activeHandle, generation = generation)
    }

    /** 暂停当前媒体。 */
    override fun pause(generation: Long): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.pause(handle = activeHandle, generation = generation)
    }

    /** 跳转当前媒体进度。 */
    override fun seekTo(
        generation: Long,
        positionMs: Long,
    ): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.seekTo(
            handle = activeHandle,
            generation = generation,
            positionMs = positionMs,
        )
    }

    /** 停止当前媒体。 */
    override fun stop(generation: Long): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.stop(handle = activeHandle, generation = generation)
    }

    /** 设置 App 内归一化音量。 */
    override fun setVolume(volume: Float): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.setVolume(handle = activeHandle, volume = volume)
    }

    /** 设置 App 内全局播放倍速。 */
    override fun setPlaybackSpeed(speed: Float): Int {
        val activeHandle: Long = activeHandleOrUnavailable() ?: return unavailable()
        return MacosAvFoundationNativeBindings.setPlaybackSpeed(handle = activeHandle, speed = speed)
    }

    /** 释放 native 会话。 */
    override fun release(): Int {
        val activeHandle: Long =
            activeHandleOrUnavailable()
                ?: return MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED
        handle = 0L
        return MacosAvFoundationNativeBindings.release(handle = activeHandle)
    }

    /** 读取仍可用的 native handle。 */
    private fun activeHandleOrUnavailable(): Long? = handle.takeIf { value: Long -> value != 0L }

    /** 统一返回 bridge 不可用状态码。 */
    private fun unavailable(): Int = MACOS_AVFOUNDATION_NATIVE_STATUS_ENGINE_UNAVAILABLE
}
