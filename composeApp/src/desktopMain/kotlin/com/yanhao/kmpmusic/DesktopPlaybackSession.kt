package com.yanhao.kmpmusic

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.yanhao.kmpmusic.data.createDesktopPlaybackDatabase
import com.yanhao.kmpmusic.data.createDesktopUserPreferencesDataStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.feature.app.MusicAppController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Desktop 进程级播放会话，负责把 Room、真实播放器与共享控制器固定在同一进程生命周期内。
 */
object DesktopPlaybackSession {
    private val runtime: DesktopPlaybackSessionRuntime by lazy {
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val playbackDatabase: PlaybackDatabase = createDesktopPlaybackDatabase()
        val userPreferencesDataStore: DataStore<Preferences> = createDesktopUserPreferencesDataStore()
        val audioRuntime: DesktopAudioRuntime =
            DesktopAudioRuntimeFactory.create(
                sessionScope = sessionScope,
            )
        DesktopPlaybackSessionRuntime(
            controller =
                createDesktopPlaybackController(
                    playbackDatabase = playbackDatabase,
                    userPreferencesDataStore = userPreferencesDataStore,
                    audioPlayerEngine = audioRuntime.audioEngine,
                    controllerScope = sessionScope,
                ),
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                audioRuntime.audioEngine.releaseAndAwait()
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
