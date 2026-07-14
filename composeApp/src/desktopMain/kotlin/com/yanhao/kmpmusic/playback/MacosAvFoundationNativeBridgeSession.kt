package com.yanhao.kmpmusic.playback

/**
 * JNI 回调对象，方法名需要保持稳定供 native bridge 查找。
 */
interface MacosAvFoundationNativeBridgeCallback {
    /** 回传当前媒体已准备完成。 */
    fun onPrepared(generation: Long, durationMs: Long, hasDuration: Boolean)

    /** 回传当前媒体正在缓冲。 */
    fun onBuffering(generation: Long, positionMs: Long, durationMs: Long, hasDuration: Boolean)

    /** 回传当前媒体进入播放中。 */
    fun onPlaying(generation: Long, positionMs: Long, durationMs: Long, hasDuration: Boolean)

    /** 回传当前媒体进入暂停。 */
    fun onPaused(generation: Long, positionMs: Long, durationMs: Long, hasDuration: Boolean)

    /** 回传当前媒体进度。 */
    fun onProgress(generation: Long, positionMs: Long, durationMs: Long, hasDuration: Boolean)

    /** 回传当前媒体自然结束。 */
    fun onEnded(generation: Long)

    /** 回传当前媒体播放失败。 */
    fun onFailed(generation: Long, errorType: Int, songId: String?, message: String)

    /** 回传 bridge 初始化失败。 */
    fun onInitializationFailed(errorType: Int, message: String)
}

/**
 * native AVFoundation bridge 会话，隐藏 JNI handle 细节。
 */
interface MacosAvFoundationNativeBridgeSession {
    /** 准备当前 generation 的媒体。 */
    fun prepare(songId: String, mediaUri: String, generation: Long, startPositionMs: Long): Int

    /** 播放当前 generation。 */
    fun play(generation: Long): Int

    /** 暂停当前 generation。 */
    fun pause(generation: Long): Int

    /** 跳转当前 generation 的进度。 */
    fun seekTo(generation: Long, positionMs: Long): Int

    /** 停止当前 generation。 */
    fun stop(generation: Long): Int

    /** 设置 App 内归一化音量。 */
    fun setVolume(volume: Float): Int

    /** 释放 native 会话。 */
    fun release(): Int
}

/**
 * native session 创建结果。
 */
sealed interface MacosAvFoundationNativeBridgeSessionCreation {
    /**
     * session 创建成功。
     *
     * @property session 可执行命令的 native 会话。
     */
    data class Success(
        val session: MacosAvFoundationNativeBridgeSession,
    ) : MacosAvFoundationNativeBridgeSessionCreation

    /**
     * session 创建失败。
     *
     * @property reason 面向诊断的失败原因。
     */
    data class Failed(
        val reason: String,
    ) : MacosAvFoundationNativeBridgeSessionCreation
}

/**
 * native session 工厂，用于隔离真实 JNI 和测试 fake。
 */
interface MacosAvFoundationNativeBridgeSessionFactory {
    /** 创建 native bridge 会话并绑定回调对象。 */
    fun create(callback: MacosAvFoundationNativeBridgeCallback): MacosAvFoundationNativeBridgeSessionCreation
}

/**
 * 真实 JNI session 工厂。
 */
object JniMacosAvFoundationNativeBridgeSessionFactory : MacosAvFoundationNativeBridgeSessionFactory {
    /** 创建 JNI session，handle 为 0 时视为初始化失败。 */
    override fun create(
        callback: MacosAvFoundationNativeBridgeCallback,
    ): MacosAvFoundationNativeBridgeSessionCreation {
        val handle: Long = MacosAvFoundationNativeBindings.create(callback = callback)
        if (handle == 0L) {
            return MacosAvFoundationNativeBridgeSessionCreation.Failed(reason = "native create 返回空 handle")
        }
        return MacosAvFoundationNativeBridgeSessionCreation.Success(
            session = JniMacosAvFoundationNativeBridgeSession(handle = handle),
        )
    }
}

/**
 * JNI native 方法绑定，方法名需要和 Objective-C++ 实现保持一致。
 */
object MacosAvFoundationNativeBindings {
    /** 创建 native AVFoundation bridge。 */
    @JvmStatic
    external fun create(callback: MacosAvFoundationNativeBridgeCallback): Long

    /** 准备当前媒体。 */
    @JvmStatic
    external fun prepare(
        handle: Long,
        songId: String,
        mediaUri: String,
        generation: Long,
        startPositionMs: Long,
    ): Int

    /** 播放当前媒体。 */
    @JvmStatic
    external fun play(handle: Long, generation: Long): Int

    /** 暂停当前媒体。 */
    @JvmStatic
    external fun pause(handle: Long, generation: Long): Int

    /** 跳转当前媒体进度。 */
    @JvmStatic
    external fun seekTo(handle: Long, generation: Long, positionMs: Long): Int

    /** 停止当前媒体。 */
    @JvmStatic
    external fun stop(handle: Long, generation: Long): Int

    /** 设置音量。 */
    @JvmStatic
    external fun setVolume(handle: Long, volume: Float): Int

    /** 释放 native bridge。 */
    @JvmStatic
    external fun release(handle: Long): Int
}
