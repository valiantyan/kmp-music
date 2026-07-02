package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngine
import com.yanhao.kmpmusic.playback.MacosLibVlcRuntime
import com.yanhao.kmpmusic.playback.UnavailableDesktopMediaPlayerAdapter
import com.yanhao.kmpmusic.playback.VlcjMediaPlayerAdapter
import kotlinx.coroutines.CoroutineScope

internal data class DesktopAudioRuntime(
    // [audioEngine] 承担 Desktop 真实播放职责，并在会话关闭时统一释放。
    val audioEngine: DesktopVlcjAudioPlayerEngine,
)

internal object DesktopAudioRuntimeFactory {
    /** 根据本机 libVLC 可用性选择真实适配器或不可用占位适配器，保持会话装配逻辑单点收口。 */
    fun create(sessionScope: CoroutineScope): DesktopAudioRuntime {
        val runtimePath = MacosLibVlcRuntime.resolve()
        val audioEngine = if (runtimePath == null) {
            DesktopVlcjAudioPlayerEngine(
                adapter = UnavailableDesktopMediaPlayerAdapter(),
                scope = sessionScope,
                libVlcPluginPath = null,
            )
        } else {
            DesktopVlcjAudioPlayerEngine(
                adapter = VlcjMediaPlayerAdapter(runtimePath = runtimePath),
                scope = sessionScope,
                libVlcPluginPath = runtimePath.pluginDirectory,
            )
        }
        return DesktopAudioRuntime(audioEngine = audioEngine)
    }
}
