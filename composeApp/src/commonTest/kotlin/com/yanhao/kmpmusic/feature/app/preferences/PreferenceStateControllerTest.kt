package com.yanhao.kmpmusic.feature.app.preferences

import com.yanhao.kmpmusic.data.InMemoryUserPreferencesRepository
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证 [PreferenceStateController] 会同时维护仓库和门面状态。
 */
class PreferenceStateControllerTest {
    /**
     * 主题设置必须同时写入仓库和 UI 状态，避免门面和持久化偏离。
     */
    @Test
    fun setThemeModePersistsAndUpdatesState(): Unit {
        val repository: InMemoryUserPreferencesRepository = InMemoryUserPreferencesRepository()
        val controller: PreferenceStateController = PreferenceStateController(
            userPreferencesRepository = repository,
        )

        val updatedState: MusicAppUiState = controller.setThemeMode(
            state = baseState(),
            themeMode = ThemeMode.Dark,
        )

        assertEquals(expected = ThemeMode.Dark, actual = repository.getThemeMode())
        assertEquals(expected = ThemeMode.Dark, actual = updatedState.themeMode)
    }

    /**
     * 单个本地发现开关变更时，其他偏好字段必须完整保留。
     */
    @Test
    fun localMusicDiscoveryPreferenceUpdatesOnlyRequestedField(): Unit {
        val repository: InMemoryUserPreferencesRepository = InMemoryUserPreferencesRepository()
        val controller: PreferenceStateController = PreferenceStateController(
            userPreferencesRepository = repository,
        )
        val state: MusicAppUiState = baseState().copy(
            localMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences(
                isAutoScanOnLaunchEnabled = true,
                shouldIgnoreShortAudio = false,
                shouldExcludeSystemFolders = true,
            ),
        )

        val updatedState: MusicAppUiState = controller.setLocalMusicSystemFoldersExcluded(
            state = state,
            isExcluded = false,
        )

        val expectedPreferences: LocalMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences(
            isAutoScanOnLaunchEnabled = true,
            shouldIgnoreShortAudio = false,
            shouldExcludeSystemFolders = false,
        )
        assertEquals(expected = expectedPreferences, actual = repository.getLocalMusicDiscoveryPreferences())
        assertEquals(expected = expectedPreferences, actual = updatedState.localMusicDiscoveryPreferences)
    }

    /** 提供最小状态夹具，避免测试被无关字段干扰。 */
    private fun baseState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = com.yanhao.kmpmusic.domain.model.PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }
}
