package com.yanhao.kmpmusic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.yanhao.kmpmusic.data.createAndroidPlaybackDatabase
import com.yanhao.kmpmusic.data.createAndroidUserPreferencesDataStore
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import com.yanhao.kmpmusic.playback.PlaybackServiceConnector
import kotlinx.coroutines.CoroutineScope

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
    val playbackDatabase: PlaybackDatabase = createAndroidPlaybackDatabase(context = context)
    val userPreferencesDataStore: DataStore<Preferences> = createAndroidUserPreferencesDataStore(context = context)
    return createPersistentMusicAppController(
        playbackDatabase = playbackDatabase,
        userPreferencesDataStore = userPreferencesDataStore,
        localMusicScanner = localMusicScanner,
        audioPlayerEngine = audioPlayerEngine,
        permissionSettingsOpener = permissionSettingsOpener,
        controllerScope = controllerScope,
        nowMillis = nowMillis,
    )
}
