package com.yanhao.kmpmusic

import android.content.Context
import com.yanhao.kmpmusic.data.PersistentFavoritesRepository
import com.yanhao.kmpmusic.data.PersistentMusicLibraryRepository
import com.yanhao.kmpmusic.data.PersistentPlaybackRepository
import com.yanhao.kmpmusic.data.PersistentSearchHistoryRepository
import com.yanhao.kmpmusic.data.PersistentUserPreferencesRepository
import com.yanhao.kmpmusic.data.createAndroidPlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.RoomPlaybackSnapshotStore
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import com.yanhao.kmpmusic.playback.PlaybackServiceConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * 组装 Android 真实播放控制器依赖图，保持进程级会话 facade 不再承担构图职责。
 */
internal fun createAndroidPlaybackController(
    context: Context,
    localMusicScanner: LocalMusicScanner,
    audioPlayerEngine: PlaybackServiceConnector,
    permissionSettingsOpener: PermissionSettingsOpener,
    controllerScope: CoroutineScope,
    nowMillis: () -> Long = { System.currentTimeMillis() },
): MusicAppController {
    val playbackDatabase: com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase =
        createAndroidPlaybackDatabase(context = context)
    val favoriteSongDao: com.yanhao.kmpmusic.domain.persistence.FavoriteSongDao =
        playbackDatabase.favoriteSongDao()
    val localSongDao: com.yanhao.kmpmusic.domain.persistence.LocalSongDao =
        playbackDatabase.localSongDao()
    val favoritesRepository: PersistentFavoritesRepository = runBlocking {
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
        userPreferencesRepository = PersistentUserPreferencesRepository.create(
            playbackDatabase = playbackDatabase,
            nowMillis = nowMillis,
        ),
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
}
