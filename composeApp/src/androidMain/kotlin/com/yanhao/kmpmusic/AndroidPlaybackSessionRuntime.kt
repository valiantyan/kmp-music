package com.yanhao.kmpmusic

import android.content.Context
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import com.yanhao.kmpmusic.playback.AndroidPlaybackRuntime
import com.yanhao.kmpmusic.playback.PlaybackServiceConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 管理 Android 进程级播放会话运行时，把真实 runtime、controller 和 UI 绑定时序集中在一起。
 */
internal class AndroidPlaybackSessionRuntime(
    // Activity 生命周期相关的 UI 绑定代理注册表。
    private val uiBindings: AndroidUiBindingRegistry = AndroidUiBindingRegistry(),
    // 脱离 Activity 生命周期的播放作用域，保证后台播放命令持续回流 shared 控制器。
    private val playbackScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    // Android 真实播放服务到 common 层的连接器。
    private val playbackServiceConnector: PlaybackServiceConnector =
        PlaybackServiceConnector(scope = playbackScope),
    // Android 通知与系统媒体命令桥运行时。
    private val playbackRuntime: AndroidPlaybackRuntime =
        AndroidPlaybackRuntime(
            serviceConnector = playbackServiceConnector,
        ),
) {
    // 当前进程级共享控制器，仅在拿到 applicationContext 后初始化。
    private var controllerHolder: MusicAppController? = null

    // 冷启动恢复只允许请求一次，避免 UI 重建时打断进行中的后台播放。
    private var hasRequestedPlaybackRestore: Boolean = false

    /**
     * 当前进程级共享控制器，后台播放和系统命令都复用同一份状态。
     */
    val controller: MusicAppController
        get() = controllerHolder ?: error("AndroidPlaybackSession 尚未 bootstrap")

    /**
     * 初始化或复用进程级播放会话，并确保服务 runtime 始终拿到 applicationContext。
     */
    fun bootstrap(context: Context) {
        val applicationContext: Context = context.applicationContext
        playbackRuntime.attachContext(context = applicationContext)
        if (controllerHolder != null) {
            return
        }
        synchronized(this) {
            if (controllerHolder != null) {
                return
            }
            val controller: MusicAppController =
                createAndroidPlaybackController(
                    context = applicationContext,
                    localMusicScanner = uiBindings.localMusicScanner,
                    audioPlayerEngine = playbackServiceConnector,
                    permissionSettingsOpener = uiBindings.permissionSettingsOpener,
                    controllerScope = playbackScope,
                )
            playbackRuntime.attachController(controller = controller)
            controllerHolder = controller
        }
    }

    /**
     * 兼容 Activity 重建时的显式接线入口，内部复用 [bootstrap] 保持单一路径。
     */
    fun attachPlaybackContext(context: Context) {
        bootstrap(context = context)
    }

    /**
     * 仅在首次 UI 接入时请求冷启动快照恢复，避免 ViewModel 重建打断现有播放。
     */
    fun ensurePlaybackSnapshotRestoreRequested() {
        if (hasRequestedPlaybackRestore) {
            return
        }
        synchronized(this) {
            if (hasRequestedPlaybackRestore) {
                return
            }
            hasRequestedPlaybackRestore = true
        }
        playbackScope.launch {
            controller.restorePlaybackSnapshot()
        }
    }

    /**
     * 绑定当前 Activity 可用的本地音乐扫描器，并在首次接入后触发恢复请求。
     */
    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        uiBindings.attachLocalMusicScanner(scanner = scanner)
        ensurePlaybackSnapshotRestoreRequested()
    }

    /**
     * 绑定当前 Activity 可用的系统权限设置入口。
     */
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        uiBindings.attachPermissionSettingsOpener(opener = opener)
    }

    /**
     * 清空当前 Activity 绑定，避免进程级对象持有过期宿主。
     */
    fun clearUiBindings() {
        uiBindings.clear()
    }
}
