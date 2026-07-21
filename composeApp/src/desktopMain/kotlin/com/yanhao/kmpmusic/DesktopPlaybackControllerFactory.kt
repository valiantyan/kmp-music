package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.DesktopFolderMusicScanner
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import kotlinx.coroutines.CoroutineScope

/**
 * 基于持久化数据库和真实桌面引擎构建共享控制器，确保 Desktop 冷启动恢复能解析本地歌曲实体。
 */
internal fun createDesktopPlaybackController(
    playbackDatabase: PlaybackDatabase,
    audioPlayerEngine: AudioPlayerEngine,
    controllerScope: CoroutineScope,
    localMusicScanner: LocalMusicScanner = DesktopFolderMusicScanner(),
    nowMillis: () -> Long = { System.currentTimeMillis() },
): MusicAppController =
    createPersistentMusicAppController(
        playbackDatabase = playbackDatabase,
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = audioPlayerEngine,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
