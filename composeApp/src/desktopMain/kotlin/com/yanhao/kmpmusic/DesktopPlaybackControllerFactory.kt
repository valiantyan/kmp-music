package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.DesktopFolderMusicScanner
import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.PersistentMusicLibraryRepository
import com.yanhao.kmpmusic.data.PersistentPlaybackRepository
import com.yanhao.kmpmusic.data.PersistentSearchHistoryRepository
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import kotlinx.coroutines.CoroutineScope
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
        playbackRepository = PersistentPlaybackRepository.create(
            playbackDatabase = playbackDatabase,
            nowMillis = nowMillis,
        ),
        playbackSnapshotStore = RoomPlaybackSnapshotStore(
            database = playbackDatabase,
            nowMillis = nowMillis,
        ),
        musicLibraryRepository = PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = favoriteSongDao,
        ),
        injectedFavoritesRepository = favoritesRepository,
        searchHistoryRepository = PersistentSearchHistoryRepository.create(
            playbackDatabase = playbackDatabase,
            nowMillis = nowMillis,
        ),
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
}
