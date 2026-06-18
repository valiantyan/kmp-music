package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository

/**
 * 用户偏好内存实现，先支撑主题切换。
 */
class InMemoryUserPreferencesRepository : UserPreferencesRepository {
    // 当前主题模式。
    private var themeMode: ThemeMode = ThemeMode.Light

    /** 获取主题模式。 */
    override fun getThemeMode(): ThemeMode = themeMode

    /** 保存主题模式。 */
    override fun saveThemeMode(themeMode: ThemeMode) {
        this.themeMode = themeMode
    }
}
