package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository

/**
 * 用户偏好内存实现，供 common fallback 和测试缓存当前进程设置。
 */
class InMemoryUserPreferencesRepository : UserPreferencesRepository {
    // 当前主题模式。
    private var themeMode: ThemeMode = ThemeMode.Light

    // 当前本地音频发现偏好。
    private var localMusicDiscoveryPreferences: LocalMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences()

    /** 获取主题模式。 */
    override fun getThemeMode(): ThemeMode = themeMode

    /** 保存主题模式。 */
    override fun saveThemeMode(themeMode: ThemeMode) {
        this.themeMode = themeMode
    }

    /** 获取本地音频发现偏好。 */
    override fun getLocalMusicDiscoveryPreferences(): LocalMusicDiscoveryPreferences = localMusicDiscoveryPreferences

    /** 保存本地音频发现偏好。 */
    override fun saveLocalMusicDiscoveryPreferences(preferences: LocalMusicDiscoveryPreferences) {
        localMusicDiscoveryPreferences = preferences
    }
}
