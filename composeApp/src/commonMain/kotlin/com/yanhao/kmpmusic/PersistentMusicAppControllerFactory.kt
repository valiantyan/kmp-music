package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.PersistentLocalPlaylistRepository
import com.yanhao.kmpmusic.data.PersistentMusicLibraryRepository
import com.yanhao.kmpmusic.data.PersistentPlaybackRepository
import com.yanhao.kmpmusic.data.PersistentSearchHistoryRepository
import com.yanhao.kmpmusic.data.PersistentUserPreferencesRepository
import com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao
import com.yanhao.kmpmusic.domain.persistence.LocalSongDao
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.playback.AudioPlayerEngine
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * 组装共享持久化控制器依赖图，平台入口只负责提供数据库和平台能力。
 */
internal fun createPersistentMusicAppController(
    playbackDatabase: PlaybackDatabase,
    localMusicScanner: LocalMusicScanner,
    audioPlayerEngine: AudioPlayerEngine,
    controllerScope: CoroutineScope,
    permissionSettingsOpener: PermissionSettingsOpener = PermissionSettingsOpener {},
    nowMillis: () -> Long,
): MusicAppController {
    val favoriteSongDao: FavoriteSongDao = playbackDatabase.favoriteSongDao()
    val localSongDao: LocalSongDao = playbackDatabase.localSongDao()
    val musicLibraryRepository: PersistentMusicLibraryRepository =
        PersistentMusicLibraryRepository(
            localSongDao = localSongDao,
            favoriteSongDao = favoriteSongDao,
        )
    val favoritesRepository: PersistentFavoritesRepository =
        runBlocking {
            PersistentFavoritesRepository(
                favoriteSongDao = favoriteSongDao,
                initialLikedSongIds =
                    PersistentFavoritesRepository.loadInitialLikedSongIds(
                        favoriteSongDao = favoriteSongDao,
                    ),
                nowMillis = nowMillis,
            )
        }
    return MusicAppController(
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = audioPlayerEngine,
        playbackRepository =
            PersistentPlaybackRepository.create(
                playbackDatabase = playbackDatabase,
                nowMillis = nowMillis,
            ),
        playbackSnapshotStore =
            RoomPlaybackSnapshotStore(
                database = playbackDatabase,
                nowMillis = nowMillis,
            ),
        musicLibraryRepository = musicLibraryRepository,
        injectedFavoritesRepository = favoritesRepository,
        localPlaylistRepository =
            PersistentLocalPlaylistRepository.create(
                playbackDatabase = playbackDatabase,
                musicLibraryRepository = musicLibraryRepository,
                nowMillis = nowMillis,
            ),
        searchHistoryRepository =
            PersistentSearchHistoryRepository.create(
                playbackDatabase = playbackDatabase,
                nowMillis = nowMillis,
            ),
        userPreferencesRepository =
            PersistentUserPreferencesRepository.create(
                playbackDatabase = playbackDatabase,
                nowMillis = nowMillis,
            ),
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
}
