package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.DesktopFolderMusicScanner
import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.PersistentMusicLibraryRepository
import com.yanhao.kmpmusic.data.createDesktopPlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.playback.DesktopVlcjAudioPlayerEngine
import com.yanhao.kmpmusic.playback.MacosLibVlcRuntime
import com.yanhao.kmpmusic.playback.VlcjMediaPlayerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 基于持久化数据库和真实桌面引擎构建共享控制器，确保 Desktop 冷启动恢复能解析本地歌曲实体。
 */
internal fun createDesktopPlaybackController(
    playbackDatabase: PlaybackDatabase,
    audioPlayerEngine: AudioPlayerEngine,
    controllerScope: CoroutineScope,
    localMusicScanner: LocalMusicScanner = DesktopFolderMusicScanner(),
    nowMillis: () -> Long = { System.currentTimeMillis() },
): MusicAppController {
    val favoriteSongDao = playbackDatabase.favoriteSongDao()
    val localSongDao = playbackDatabase.localSongDao()
    val favoritesRepository = runBlocking {
        PersistentFavoritesRepository(
            favoriteSongDao = favoriteSongDao,
            initialLikedSongIds = PersistentFavoritesRepository.loadInitialLikedSongIds(
                favoriteSongDao = favoriteSongDao,
            ),
            nowMillis = nowMillis,
        )
    }
    return MusicAppController(
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = audioPlayerEngine,
        playbackSnapshotStore = RoomPlaybackSnapshotStore(
            database = playbackDatabase,
            nowMillis = nowMillis,
        ),
        musicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = favoriteSongDao,
        ),
        injectedFavoritesRepository = favoritesRepository,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
}

/**
 * Desktop 进程级播放会话运行时，统一管理恢复幂等、关闭时序和底层资源收口。
 */
internal class DesktopPlaybackSessionRuntime(
    val controller: MusicAppController,
    private val sessionScope: CoroutineScope,
    private val releaseAudioEngineAndAwait: suspend () -> Unit,
    private val closePlaybackDatabase: () -> Unit,
    private val persistPlaybackSnapshotForProcessTeardown: suspend (Long, Long?) -> Unit = { positionMs, durationMs ->
        controller.persistPlaybackSnapshotForProcessTeardown(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    },
) {
    private val sessionJob: Job = sessionScope.coroutineContext[Job]
        ?: error("DesktopPlaybackSessionRuntime 需要带 Job 的会话作用域")

    // 冷启动恢复只允许请求一次，避免窗口重组或多次 attach 覆盖活跃播放态。
    private var hasRequestedPlaybackRestore: Boolean = false

    // close 只允许执行一次，避免重复释放数据库或原生播放器。
    private var isClosed: Boolean = false

    /** 只在 Desktop 进程生命周期内第一次窗口接入时请求快照恢复。 */
    fun ensurePlaybackSnapshotRestoreRequested() {
        synchronized(this) {
            if (hasRequestedPlaybackRestore || isClosed) {
                return
            }
            hasRequestedPlaybackRestore = true
        }
        sessionScope.launch {
            controller.restorePlaybackSnapshot()
        }
    }

    /**
     * 进程关闭前按顺序释放桌面播放器、停止长生命周期协程并同步持久化最终快照。
     */
    fun close() {
        val shouldClose: Boolean = synchronized(this) {
            if (isClosed) {
                false
            } else {
                isClosed = true
                true
            }
        }
        if (!shouldClose) {
            return
        }
        runBlocking {
            var teardownFailure: Throwable? = null
            try {
                releaseAudioEngineAndAwait()
            } catch (throwable: Throwable) {
                teardownFailure = throwable
            } finally {
                try {
                    sessionJob.cancelAndJoin()
                } catch (throwable: Throwable) {
                    teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
                }
            }
            val finalPositionMs: Long = controller.uiState.playbackPositionMs
            val finalDurationMs: Long? = controller.uiState.playbackDurationMs
            try {
                persistPlaybackSnapshotForProcessTeardown(
                    finalPositionMs,
                    finalDurationMs,
                )
            } catch (throwable: Throwable) {
                teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
            } finally {
                try {
                    closePlaybackDatabase()
                } catch (throwable: Throwable) {
                    teardownFailure = teardownFailure?.also { it.addSuppressed(throwable) } ?: throwable
                }
            }
            teardownFailure?.let { throwable ->
                throw throwable
            }
        }
    }
}

/**
 * Desktop 进程级播放会话，负责把 Room、真实播放器与共享控制器固定在同一进程生命周期内。
 */
object DesktopPlaybackSession {
    private val runtime: DesktopPlaybackSessionRuntime by lazy {
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val playbackDatabase: PlaybackDatabase = createDesktopPlaybackDatabase()
        val runtimePath = MacosLibVlcRuntime.resolve()
        val audioEngine = DesktopVlcjAudioPlayerEngine(
            adapter = VlcjMediaPlayerAdapter(runtimePath = runtimePath),
            scope = sessionScope,
            libVlcPluginPath = runtimePath?.pluginDirectory,
        )
        DesktopPlaybackSessionRuntime(
            controller = createDesktopPlaybackController(
                playbackDatabase = playbackDatabase,
                audioPlayerEngine = audioEngine,
                controllerScope = sessionScope,
            ),
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                audioEngine.releaseAndAwait()
            },
            closePlaybackDatabase = {
                playbackDatabase.close()
            },
        )
    }

    /** 进程级共享控制器，复用 Desktop 真实播放、Room 快照、歌曲与收藏持久化。 */
    val controller: MusicAppController
        get() = runtime.controller

    /** 只在 Desktop 进程第一次接入窗口时触发冷启动恢复。 */
    fun ensurePlaybackSnapshotRestoreRequested() {
        runtime.ensurePlaybackSnapshotRestoreRequested()
    }

    /** 在应用退出前同步收口真实播放器、控制器协程和数据库。 */
    fun close() {
        runtime.close()
    }
}
