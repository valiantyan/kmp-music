package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.IosFolderMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.playback.IosAudioSessionController
import com.yanhao.kmpmusic.playback.IosAvAudioSessionController
import com.yanhao.kmpmusic.playback.IosAvFoundationAudioPlayerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * iOS 进程级播放会话，持有真实播放器、audio session 和共享控制器。
 */
object IosPlaybackSession {
    // iOS 播放会话运行时，lazy 确保宿主第一次进入 UI 时才初始化原生资源。
    private val runtime: IosPlaybackSessionRuntime by lazy {
        val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val audioSessionController: IosAudioSessionController = IosAvAudioSessionController()
        val audioEngine: IosAvFoundationAudioPlayerEngine = IosAvFoundationAudioPlayerEngine(
            audioSessionController = audioSessionController,
            scope = sessionScope,
        )
        IosPlaybackSessionRuntime(
            controller = MusicAppController(
                localMusicScanner = IosFolderMusicScanner(),
                audioPlayerEngine = audioEngine,
                controllerScope = sessionScope,
            ),
            sessionScope = sessionScope,
            releaseAudioEngine = {
                audioEngine.releaseAndAwait()
            },
            releaseAudioSession = audioSessionController::release,
        )
    }

    /** iOS 共享控制器，避免播放器跟随 Compose composition 重建。 */
    val controller: MusicAppController
        get() = runtime.controller

    /** 首次 UI 接入时请求恢复；重复调用由 runtime 幂等保护。 */
    fun ensurePlaybackSnapshotRestoreRequested() {
        runtime.ensurePlaybackSnapshotRestoreRequested()
    }

    /** 宿主退出或测试收尾时释放 native 播放资源。 */
    fun close() {
        runtime.close()
    }
}
