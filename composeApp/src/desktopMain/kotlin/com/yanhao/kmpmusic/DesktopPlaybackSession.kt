package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.DesktopFolderMusicScanner
import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.createDesktopPlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngine
import com.yanhao.kmpmusic.playback.MacosLibVlcRuntime
import com.yanhao.kmpmusic.playback.VlcjMediaPlayerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Desktop 进程级播放会话，负责把 Room、真实播放器与共享控制器固定在同一进程生命周期内。
 */
object DesktopPlaybackSession {
    // 脱离 Compose 重组生命周期的常驻作用域，统一承接真实播放器与快照恢复。
    private val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 进程级播放数据库，避免窗口重建导致收藏和快照状态丢失。
    private val playbackDatabase = createDesktopPlaybackDatabase()

    // Desktop 运行时解析出的 LibVLC 目录，允许打包版与开发回退共用一条创建链路。
    private val runtimePath = MacosLibVlcRuntime.resolve()

    // 真实桌面播放器引擎，统一复用到整条共享播放链路。
    private val audioEngine = DesktopVlcjAudioPlayerEngine(
        adapter = VlcjMediaPlayerAdapter(runtimePath = runtimePath),
        scope = sessionScope,
        libVlcPluginPath = runtimePath?.pluginDirectory,
    )

    // 冷启动恢复只允许请求一次，避免窗口重组时重复覆盖正在播放的运行态。
    private var hasRequestedPlaybackRestore: Boolean = false

    /**
     * 进程级共享控制器，复用 Desktop 真实播放、Room 快照和收藏持久化。
     */
    val controller: MusicAppController by lazy {
        val favoriteSongDao = playbackDatabase.favoriteSongDao()
        val favoritesRepository = runBlocking {
            PersistentFavoritesRepository(
                favoriteSongDao = favoriteSongDao,
                initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                    favoriteSongDao = favoriteSongDao,
                ),
                nowMillis = { System.currentTimeMillis() },
            )
        }
        MusicAppController(
            localMusicScanner = DesktopFolderMusicScanner(),
            audioPlayerEngine = audioEngine,
            playbackSnapshotStore = RoomPlaybackSnapshotStore(
                database = playbackDatabase,
                nowMillis = { System.currentTimeMillis() },
            ),
            injectedFavoritesRepository = favoritesRepository,
            controllerScope = sessionScope,
            nowMillis = { System.currentTimeMillis() },
        )
    }

    /**
     * 只在 Desktop 进程生命周期内第一次窗口接入时请求快照恢复。
     */
    fun ensurePlaybackSnapshotRestoreRequested() {
        if (hasRequestedPlaybackRestore) {
            return
        }
        hasRequestedPlaybackRestore = true
        sessionScope.launch {
            controller.restorePlaybackSnapshot()
        }
    }

    /**
     * 在应用退出前固化最后一次播放快照，并释放底层原生播放器资源。
     */
    fun close() {
        controller.persistPlaybackSnapshotForServiceTeardown(
            positionMs = controller.uiState.playbackPositionMs,
            durationMs = controller.uiState.playbackDurationMs,
        )
        audioEngine.release()
    }
}
