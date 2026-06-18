package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.ThemeMode

/**
 * 用户偏好接口，先服务主题切换，后续可扩展播放和同步偏好。
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
}
