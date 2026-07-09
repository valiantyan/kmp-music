package com.yanhao.kmpmusic.feature.app.preferences

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 偏好设置工作流控制器，统一保存主题与本地音频发现偏好。
 *
 * @property userPreferencesRepository 承载偏好持久化的仓库接口。
 */
internal class PreferenceStateController(
    // 用户偏好仓库，保证门面外观不直接散落持久化细节。
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    /**
     * 保存主题模式，并返回已经同步新主题的状态副本。
     *
     * @param state 当前门面状态。
     * @param themeMode 用户选中的主题模式。
     * @return 已写回主题模式的新状态。
     */
    fun setThemeMode(
        state: MusicAppUiState,
        themeMode: ThemeMode,
    ): MusicAppUiState {
        userPreferencesRepository.saveThemeMode(themeMode = themeMode)
        return state.copy(themeMode = themeMode)
    }

    /**
     * 保存启动时自动扫描偏好，同时保留其他本地发现设置。
     *
     * @param state 当前门面状态。
     * @param isEnabled 是否启用启动时自动扫描。
     * @return 已同步新偏好的状态。
     */
    fun setLocalMusicAutoScanOnLaunchEnabled(
        state: MusicAppUiState,
        isEnabled: Boolean,
    ): MusicAppUiState {
        return updateLocalMusicDiscoveryPreferences(
            state = state,
        ) { preferences: LocalMusicDiscoveryPreferences ->
            preferences.copy(isAutoScanOnLaunchEnabled = isEnabled)
        }
    }

    /**
     * 保存短音频过滤偏好，同时保留其他本地发现设置。
     *
     * @param state 当前门面状态。
     * @param isIgnored 是否忽略短音频。
     * @return 已同步新偏好的状态。
     */
    fun setLocalMusicShortAudioIgnored(
        state: MusicAppUiState,
        isIgnored: Boolean,
    ): MusicAppUiState {
        return updateLocalMusicDiscoveryPreferences(
            state = state,
        ) { preferences: LocalMusicDiscoveryPreferences ->
            preferences.copy(shouldIgnoreShortAudio = isIgnored)
        }
    }

    /**
     * 保存系统文件夹排除偏好，同时保留其他本地发现设置。
     *
     * @param state 当前门面状态。
     * @param isExcluded 是否排除系统文件夹。
     * @return 已同步新偏好的状态。
     */
    fun setLocalMusicSystemFoldersExcluded(
        state: MusicAppUiState,
        isExcluded: Boolean,
    ): MusicAppUiState {
        return updateLocalMusicDiscoveryPreferences(
            state = state,
        ) { preferences: LocalMusicDiscoveryPreferences ->
            preferences.copy(shouldExcludeSystemFolders = isExcluded)
        }
    }

    // 统一保留未修改字段，避免多个偏好入口各自复制保存逻辑。
    private fun updateLocalMusicDiscoveryPreferences(
        state: MusicAppUiState,
        transform: (LocalMusicDiscoveryPreferences) -> LocalMusicDiscoveryPreferences,
    ): MusicAppUiState {
        val preferences: LocalMusicDiscoveryPreferences = transform(state.localMusicDiscoveryPreferences)
        userPreferencesRepository.saveLocalMusicDiscoveryPreferences(preferences = preferences)
        return state.copy(localMusicDiscoveryPreferences = preferences)
    }
}
