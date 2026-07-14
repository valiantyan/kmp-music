package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.playback.ApplePlaybackBridge
import com.yanhao.kmpmusic.playback.DesktopAppleAudioPlayerEngine
import com.yanhao.kmpmusic.playback.MacosAvFoundationPlaybackBridge
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal data class DesktopAudioRuntime(
    // [audioEngine] 承担 macOS Desktop 真实播放职责，并在会话关闭时统一释放。
    val audioEngine: DesktopAppleAudioPlayerEngine,
)

internal object DesktopAudioRuntimeFactory {
    /**
     * 创建 macOS 默认 AVFoundation 播放运行时，bridge 不可用时由 Apple engine 发出统一失败事件。
     */
    fun create(
        sessionScope: CoroutineScope,
        bridgeFactory: () -> ApplePlaybackBridge = { MacosAvFoundationPlaybackBridge.create() },
        dispatcher: CoroutineContext = Dispatchers.Default,
    ): DesktopAudioRuntime {
        val audioEngine: DesktopAppleAudioPlayerEngine = DesktopAppleAudioPlayerEngine(
            bridge = bridgeFactory(),
            scope = sessionScope,
            dispatcher = dispatcher,
        )
        return DesktopAudioRuntime(audioEngine = audioEngine)
    }
}
