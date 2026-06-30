package com.yanhao.kmpmusic

import android.content.Context
import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.PersistentMusicLibraryRepository
import com.yanhao.kmpmusic.data.PersistentSearchHistoryRepository
import com.yanhao.kmpmusic.data.createAndroidPlaybackDatabase
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import com.yanhao.kmpmusic.playback.AndroidPlaybackRuntime
import com.yanhao.kmpmusic.playback.PlaybackServiceConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Android 进程级播放会话，统一拥有真实控制器、Room 持久化和播放运行时。
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

    // Android 进程级通知与命令桥运行时。
    private val playbackRuntime: AndroidPlaybackRuntime = AndroidPlaybackRuntime(
        serviceConnector = playbackServiceConnector,
    )

    // 当前进程级共享控制器；仅在拿到 applicationContext 后初始化。
    private var controllerHolder: MusicAppController? = null

    // 冷启动恢复只允许在进程级会话里请求一次，避免 UI 重建时打断后台播放。
    private var hasRequestedPlaybackRestore: Boolean = false

    /**
     * 当前进程级共享控制器，后台播放和系统命令都复用同一份状态。
     */
    val controller: MusicAppController
        get() = controllerHolder ?: error("AndroidPlaybackSession 尚未 bootstrap")

    /**
     * 确保进程级播放会话已初始化并拿到 applicationContext，供 Activity 与 service 共用。
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
            val playbackDatabase = createAndroidPlaybackDatabase(context = applicationContext)
            val favoriteSongDao = playbackDatabase.favoriteSongDao()
            val localSongDao = playbackDatabase.localSongDao()
            val favoritesRepository = runBlocking {
                PersistentFavoritesRepository(
                    favoriteSongDao = favoriteSongDao,
                    initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                        favoriteSongDao = favoriteSongDao,
                    ),
                    nowMillis = { System.currentTimeMillis() },
                )
            }
            controllerHolder = MusicAppController(
                localMusicScanner = localMusicScanner,
                audioPlayerEngine = playbackServiceConnector,
                playbackSnapshotStore = RoomPlaybackSnapshotStore(
                    database = playbackDatabase,
                    nowMillis = { System.currentTimeMillis() },
                ),
                musicLibraryRepository = PersistentMusicLibraryRepository(
                    localSongDao = localSongDao,
                    favoriteSongDao = favoriteSongDao,
                ),
                injectedFavoritesRepository = favoritesRepository,
                searchHistoryRepository = PersistentSearchHistoryRepository(
                    searchHistoryDao = playbackDatabase.searchHistoryDao(),
                    nowMillis = { System.currentTimeMillis() },
                ),
                permissionSettingsOpener = permissionSettingsOpener,
                controllerScope = playbackScope,
                nowMillis = { System.currentTimeMillis() },
            ).also { controller: MusicAppController ->
                playbackRuntime.attachController(controller = controller)
            }
        }
    }

    /**
     * 兼容 Activity 重建时的显式接线入口，内部直接复用 [bootstrap]。
     */
    fun attachPlaybackContext(context: Context) {
        bootstrap(context = context)
    }

    /**
     * 仅在当前进程会话的首次 UI 接入时请求冷启动恢复，避免 ViewModel 重建时暂停活动播放。
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
     * 注入当前 Activity 可用的 Android scanner。
     */
    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        localMusicScanner.replace(scanner = scanner)
        ensurePlaybackSnapshotRestoreRequested()
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
