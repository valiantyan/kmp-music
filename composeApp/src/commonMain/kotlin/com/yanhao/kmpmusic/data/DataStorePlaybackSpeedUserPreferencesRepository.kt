package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.runBlocking

/**
 * 给现有偏好仓库补上 DataStore 倍速持久化，避免 iOS 等无 Room 入口丢失倍速设置。
 *
 * @param delegate 继续承接本地音频发现偏好的仓库。
 * @param playbackSpeedPreferencesStore DataStore 倍速存储。
 */
internal class DataStorePlaybackSpeedUserPreferencesRepository(
    private val delegate: UserPreferencesRepository,
    private val playbackSpeedPreferencesStore: PlaybackSpeedPreferencesStore,
) : UserPreferencesRepository {
    /** 获取主题偏好，保持既有委托仓库行为。 */
    override fun getThemeMode(): ThemeMode = delegate.getThemeMode()

    /** 保存主题偏好，保持既有委托仓库行为。 */
    override fun saveThemeMode(themeMode: ThemeMode) {
        delegate.saveThemeMode(themeMode = themeMode)
    }

    /** 从 DataStore 读取全局播放倍速。 */
    override fun getPlaybackSpeed(): PlaybackSpeed =
        runBlocking {
            playbackSpeedPreferencesStore.readPlaybackSpeed()
        }

    /** 把全局播放倍速保存到 DataStore。 */
    override fun savePlaybackSpeed(playbackSpeed: PlaybackSpeed) {
        runBlocking {
            playbackSpeedPreferencesStore.savePlaybackSpeed(playbackSpeed = playbackSpeed)
        }
    }

    /** 获取本地音频发现偏好，保持既有委托仓库行为。 */
    override fun getLocalMusicDiscoveryPreferences(): LocalMusicDiscoveryPreferences = delegate.getLocalMusicDiscoveryPreferences()

    /** 保存本地音频发现偏好，保持既有委托仓库行为。 */
    override fun saveLocalMusicDiscoveryPreferences(preferences: LocalMusicDiscoveryPreferences) {
        delegate.saveLocalMusicDiscoveryPreferences(preferences = preferences)
    }
}
