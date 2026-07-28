package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.domain.model.ThemeMode

/**
 * 用户偏好接口，承载主题和本地音频发现偏好。
 */
interface UserPreferencesRepository {
    /**
     * 获取主题偏好。
     */
    fun getThemeMode(): ThemeMode

    /**
     * 保存主题偏好。
     */
    fun saveThemeMode(themeMode: ThemeMode)

    /**
     * 获取全局播放倍速。
     */
    fun getPlaybackSpeed(): PlaybackSpeed

    /**
     * 保存全局播放倍速。
     */
    fun savePlaybackSpeed(playbackSpeed: PlaybackSpeed)

    /**
     * 获取本地音频发现偏好。
     */
    fun getLocalMusicDiscoveryPreferences(): LocalMusicDiscoveryPreferences

    /**
     * 保存本地音频发现偏好。
     */
    fun saveLocalMusicDiscoveryPreferences(preferences: LocalMusicDiscoveryPreferences)
}
