package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.persistence.UserPreferenceDao
import com.yanhao.kmpmusic.domain.persistence.UserPreferenceEntity
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证 [PersistentUserPreferencesRepository] 能持久化主题、倍速和本地音频发现偏好。
 */
class PersistentUserPreferencesRepositoryTest {
    /**
     * 保存偏好后，新仓库实例必须恢复同一份设置。
     */
    @Test
    fun savePreferencesPersistsAcrossRepositoryInstances() {
        val dao: FakeUserPreferenceDao = FakeUserPreferenceDao()
        val repository: UserPreferencesRepository =
            PersistentUserPreferencesRepository(
                userPreferenceDao = dao,
                nowMillis = { 123L },
            )
        val preferences =
            LocalMusicDiscoveryPreferences(
                isAutoScanOnLaunchEnabled = true,
                shouldIgnoreShortAudio = false,
                shouldExcludeSystemFolders = false,
            )

        repository.saveThemeMode(themeMode = ThemeMode.Dark)
        repository.savePlaybackSpeed(playbackSpeed = PlaybackSpeed.OneQuarter)
        repository.saveLocalMusicDiscoveryPreferences(preferences = preferences)
        val restoredRepository: UserPreferencesRepository =
            PersistentUserPreferencesRepository(
                userPreferenceDao = dao,
            )

        assertEquals(expected = ThemeMode.Dark, actual = restoredRepository.getThemeMode())
        assertEquals(expected = PlaybackSpeed.OneQuarter, actual = restoredRepository.getPlaybackSpeed())
        assertEquals(expected = preferences, actual = restoredRepository.getLocalMusicDiscoveryPreferences())
        assertEquals(expected = "1.25", actual = dao.getSavedValue(key = "playback.speed"))
        assertEquals(expected = "false", actual = dao.getSavedValue(key = "localMusic.ignoreShortAudio"))
    }

    /**
     * 旧版本或异常写入的倍速值必须回退默认 1.0，避免冷启动带入非法播放器参数。
     */
    @Test
    fun invalidPlaybackSpeedFallsBackToDefault() {
        val dao: FakeUserPreferenceDao = FakeUserPreferenceDao()
        dao.saveRawValue(
            key = "playback.speed",
            value = "3.0",
        )
        val repository: UserPreferencesRepository =
            PersistentUserPreferencesRepository(
                userPreferenceDao = dao,
            )
        assertEquals(expected = PlaybackSpeed.resolveDefault(), actual = repository.getPlaybackSpeed())
    }

    /**
     * 多项本地音频发现偏好覆盖保存应进入同一事务。
     */
    @Test
    fun saveLocalMusicDiscoveryPreferencesRunsInsideWriteTransaction() {
        val dao: FakeUserPreferenceDao = FakeUserPreferenceDao()
        var transactionCount: Int = 0
        val repository: UserPreferencesRepository =
            PersistentUserPreferencesRepository(
                userPreferenceDao = dao,
                runInWriteTransaction = { block: suspend () -> Unit ->
                    transactionCount += 1
                    block()
                },
                nowMillis = { 456L },
            )

        repository.saveLocalMusicDiscoveryPreferences(
            preferences =
                LocalMusicDiscoveryPreferences(
                    isAutoScanOnLaunchEnabled = true,
                    shouldIgnoreShortAudio = true,
                    shouldExcludeSystemFolders = false,
                ),
        )

        assertEquals(expected = 1, actual = transactionCount)
        assertEquals(expected = "false", actual = dao.getSavedValue(key = "localMusic.excludeSystemFolders"))
    }

    private class FakeUserPreferenceDao : UserPreferenceDao {
        // 用 key 模拟数据库主键。
        private val rows: LinkedHashMap<String, UserPreferenceEntity> = linkedMapOf()

        /** 读取指定 key 的偏好值。 */
        override suspend fun getValue(key: String): String? = rows[key]?.value

        /** 覆盖保存指定偏好。 */
        override suspend fun savePreference(entity: UserPreferenceEntity) {
            rows[entity.key] = entity
        }

        /** 读取已保存值，供测试断言具体落库内容。 */
        fun getSavedValue(key: String): String? = rows[key]?.value

        /** 写入原始偏好值，供测试模拟旧版本或异常数据。 */
        fun saveRawValue(
            key: String,
            value: String,
        ) {
            rows[key] =
                UserPreferenceEntity(
                    key = key,
                    value = value,
                    updatedAt = 0L,
                )
        }
    }
}
