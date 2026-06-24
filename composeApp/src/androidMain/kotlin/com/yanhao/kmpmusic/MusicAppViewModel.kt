package com.yanhao.kmpmusic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * Android 配置变化期间持有共享控制器，避免旋转、深浅色或字体变化重建页面状态。
 */
class MusicAppViewModel : ViewModel() {
    // Activity 重建时更新的 Android scanner 代理，避免控制器持有过期 ActivityResult launcher。
    private val localMusicScanner: MutableLocalMusicScanner = MutableLocalMusicScanner()

    // Activity 重建时更新的权限设置入口，避免控制器持有过期 Activity。
    private val permissionSettingsOpener: MutablePermissionSettingsOpener = MutablePermissionSettingsOpener()

    // Android 真实播放服务的 common 层代理，避免控制器继续使用 fake engine。
    private val playbackServiceConnector: PlaybackServiceConnector = PlaybackServiceConnector(
        scope = viewModelScope,
    )

    /**
     * 共享 App 控制器，生命周期跟随 [MusicAppViewModel]。
     */
    val controller: MusicAppController = MusicAppController(
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = playbackServiceConnector,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = viewModelScope,
    )

    init {
        PlaybackCommandBridgeRegistry.attach(
            bridge = ControllerPlaybackCommandBridge(controller = controller),
        )
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
     * 注入 applicationContext，让播放 connector 能按需拉起 Android 播放服务。
     */
    fun attachPlaybackContext(context: Context) {
        playbackServiceConnector.attachContext(context = context)
    }

    /**
     * ViewModel 销毁时同步移除命令桥，避免系统媒体命令误投递给旧控制器。
     */
    override fun onCleared() {
        PlaybackCommandBridgeRegistry.detach()
        super.onCleared()
    }
}

/**
 * 把系统媒体命令回流到共享控制器，保证队列与播放状态仍由 common 协调器托管。
 */
private class ControllerPlaybackCommandBridge(
    // 当前 ViewModel 生命周期内稳定存在的共享控制器。
    private val controller: MusicAppController,
) : PlaybackCommandBridge {
    /** 上一首命令始终走共享控制器，避免系统直接改 ExoPlayer。 */
    override fun previous() {
        controller.moveTrack(direction = -1)
    }

    /** 播放/暂停命令统一走共享控制器。 */
    override fun togglePlayback() {
        controller.togglePlayback()
    }

    /** 下一首命令始终走共享控制器，避免系统直接改 ExoPlayer。 */
    override fun next() {
        controller.moveTrack(direction = 1)
    }

    /** Seek 命令统一先改 shared 状态，再由协调器驱动真引擎。 */
    override fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs = positionMs)
    }
}

/**
 * ViewModel 内稳定存在的 scanner 代理，让 Activity 可以在重建后刷新平台实现。
 */
private class MutableLocalMusicScanner : LocalMusicScanner {
    // 当前委托 scanner，Activity attach 前只返回明确初始化错误。
    private var scanner: LocalMusicScanner = MissingAndroidLocalMusicScanner()

    /** 执行扫描时转发给当前 Android scanner。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scanner.scan(request = request)
    }

    // 替换 Activity 绑定的 scanner。
    fun replace(scanner: LocalMusicScanner) {
        this.scanner = scanner
    }
}

/**
 * ViewModel 内稳定存在的权限设置入口代理，Activity 重建后刷新平台实现。
 */
private class MutablePermissionSettingsOpener : PermissionSettingsOpener {
    // 当前委托入口，Activity attach 前保持空操作。
    private var opener: PermissionSettingsOpener = PermissionSettingsOpener {}

    /** 打开当前委托的系统权限设置页。 */
    override fun openPermissionSettings() {
        opener.openPermissionSettings()
    }

    // 替换 Activity 绑定的权限设置入口。
    fun replace(opener: PermissionSettingsOpener) {
        this.opener = opener
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
