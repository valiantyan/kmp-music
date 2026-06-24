package com.yanhao.kmpmusic

import android.content.Context
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import com.yanhao.kmpmusic.playback.PlaybackCommandBridge
import com.yanhao.kmpmusic.playback.PlaybackCommandBridgeRegistry
import com.yanhao.kmpmusic.playback.PlaybackServiceConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Android 进程级播放会话，承载真实播放控制器、命令桥和 service connector。
 */
object AndroidPlaybackSession {
    // Activity 可替换的扫描代理，避免长生命周期 controller 持有失效 Activity。
    private val localMusicScanner: MutableLocalMusicScanner = MutableLocalMusicScanner()

    // Activity 可替换的权限设置入口代理。
    private val permissionSettingsOpener: MutablePermissionSettingsOpener = MutablePermissionSettingsOpener()

    // 脱离 Activity 生命周期的播放作用域，保证后台播放命令仍能回流 shared 控制器。
    private val playbackScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Android 真实播放服务的 common 层代理。
    private val playbackServiceConnector: PlaybackServiceConnector = PlaybackServiceConnector(
        scope = playbackScope,
    )

    /**
     * 进程级共享控制器，后台播放期间也保持同一份队列与播放状态。
     */
    val controller: MusicAppController = MusicAppController(
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = playbackServiceConnector,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = playbackScope,
    )

    init {
        PlaybackCommandBridgeRegistry.attach(
            bridge = ControllerPlaybackCommandBridge(controller = controller),
        )
    }

    /**
     * 注入 applicationContext，让 connector 能惰性拉起 Android 播放服务。
     */
    fun attachPlaybackContext(context: Context) {
        playbackServiceConnector.attachContext(context = context)
    }

    /**
     * 确保进程级播放会话已初始化并拿到 applicationContext，供无 UI 场景下的 service 自举调用。
     */
    fun bootstrap(context: Context) {
        attachPlaybackContext(context = context.applicationContext)
    }

    /**
     * 注入当前 Activity 可用的 Android scanner。
     */
    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        localMusicScanner.replace(scanner = scanner)
    }

    /**
     * 注入当前 Activity 可用的系统权限设置入口。
     */
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        permissionSettingsOpener.replace(opener = opener)
    }

    /**
     * 当前 Activity 销毁时清空 UI 依赖，避免持有过期 Activity 引用。
     */
    fun clearUiBindings() {
        localMusicScanner.clear()
        permissionSettingsOpener.clear()
    }
}

/**
 * 把系统媒体命令回流到共享控制器，保证队列与播放状态仍由 common 协调器托管。
 */
private class ControllerPlaybackCommandBridge(
    // 进程级稳定存在的共享控制器。
    private val controller: MusicAppController,
) : PlaybackCommandBridge {
    /** 系统播放命令显式走 shared 控制器，避免依赖 toggle 猜状态。 */
    override fun play() {
        controller.play()
    }

    /** 系统暂停命令显式走 shared 控制器，避免 buffering/loading 态被忽略。 */
    override fun pause() {
        controller.pause()
    }

    /** 上一首命令始终走共享控制器，避免系统直接改 ExoPlayer。 */
    override fun previous() {
        controller.moveTrack(direction = -1)
    }

    /** 下一首命令始终走共享控制器，避免系统直接改 ExoPlayer。 */
    override fun next() {
        controller.moveTrack(direction = 1)
    }

    /** Seek 命令统一先改 shared 状态，再由协调器驱动真引擎。 */
    override fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs = positionMs)
    }

    /** 精确下标切歌必须经共享控制器更新完整队列状态。 */
    override fun skipToQueueIndex(index: Int, positionMs: Long) {
        controller.skipToQueueIndex(
            index = index,
            positionMs = positionMs,
        )
    }
}

/**
 * 进程级 scanner 代理，让 Activity 可以在重建后刷新平台实现。
 */
private class MutableLocalMusicScanner : LocalMusicScanner {
    // 当前委托 scanner，Activity attach 前只返回明确初始化错误。
    private var scanner: LocalMusicScanner = MissingAndroidLocalMusicScanner()

    /** 执行扫描时转发给当前 Android scanner。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scanner.scan(request = request)
    }

    /** 替换 Activity 绑定的 scanner。 */
    fun replace(scanner: LocalMusicScanner) {
        this.scanner = scanner
    }

    /** 清空旧 Activity 绑定的 scanner，避免持有失效引用。 */
    fun clear() {
        scanner = MissingAndroidLocalMusicScanner()
    }
}

/**
 * 进程级权限设置入口代理，Activity 重建后刷新平台实现。
 */
private class MutablePermissionSettingsOpener : PermissionSettingsOpener {
    // 当前委托入口，Activity attach 前保持空操作。
    private var opener: PermissionSettingsOpener = PermissionSettingsOpener {}

    /** 打开当前委托的系统权限设置页。 */
    override fun openPermissionSettings() {
        opener.openPermissionSettings()
    }

    /** 替换 Activity 绑定的权限设置入口。 */
    fun replace(opener: PermissionSettingsOpener) {
        this.opener = opener
    }

    /** 清空旧 Activity 绑定的权限设置入口，避免泄漏。 */
    fun clear() {
        opener = PermissionSettingsOpener {}
    }
}

/**
 * 防御 Activity 尚未完成注入时触发扫描的极端情况。
 */
private class MissingAndroidLocalMusicScanner : LocalMusicScanner {
    /** 返回明确错误，而不是静默使用 common fake scanner。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        throw LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.Unknown,
                message = "Android 本地音乐扫描器尚未初始化",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            ),
        )
    }
}
