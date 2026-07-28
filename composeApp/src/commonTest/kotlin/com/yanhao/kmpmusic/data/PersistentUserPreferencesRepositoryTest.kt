package com.yanhao.kmpmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.persistence.UserPreferenceDao
import com.yanhao.kmpmusic.domain.persistence.UserPreferenceEntity
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证 [PersistentUserPreferencesRepository] 能持久化主题、倍速和本地音频发现偏好。
 */
class PersistentUserPreferencesRepositoryTest {
    /**
     * 保存偏好后，新仓库实例必须恢复同一份设置。
     */
    @Test
    fun savePreferencesPersistsAcrossRepositoryInstances(): Unit =
        runTest {
            val dao: FakeUserPreferenceDao = FakeUserPreferenceDao()
            val dataStore: DataStore<Preferences> = createTestUserPreferencesDataStore()
            val repository: UserPreferencesRepository =
                PersistentUserPreferencesRepository(
                    userPreferenceDao = dao,
                    playbackSpeedPreferencesStore =
                        PlaybackSpeedPreferencesStore(
                            dataStore = dataStore,
                        ),
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
                    playbackSpeedPreferencesStore =
                        PlaybackSpeedPreferencesStore(
                            dataStore = dataStore,
                        ),
                )
            val restoredDataStorePreferences: Preferences = dataStore.data.first()
            assertEquals(expected = ThemeMode.Dark, actual = restoredRepository.getThemeMode())
            assertEquals(expected = PlaybackSpeed.OneQuarter, actual = restoredRepository.getPlaybackSpeed())
            assertEquals(expected = preferences, actual = restoredRepository.getLocalMusicDiscoveryPreferences())
            assertEquals(expected = "Dark", actual = dao.getSavedValue(key = "themeMode"))
            assertEquals(expected = 1.25f, actual = restoredDataStorePreferences[PLAYBACK_SPEED_TEST_KEY])
            assertNull(actual = dao.getSavedValue(key = "playback.speed"))
            assertEquals(expected = "false", actual = dao.getSavedValue(key = "localMusic.ignoreShortAudio"))
        }

    /**
     * 旧版本或异常写入的 DataStore 倍速值必须回退默认 1.0，避免冷启动带入非法播放器参数。
     */
    @Test
    fun invalidPlaybackSpeedFallsBackToDefault(): Unit =
        runTest {
            val dao: FakeUserPreferenceDao = FakeUserPreferenceDao()
            val dataStore: DataStore<Preferences> = createTestUserPreferencesDataStore()
            dataStore.edit { preferences: MutablePreferences ->
                preferences[PLAYBACK_SPEED_TEST_KEY] = 3.0f
            }
            val repository: UserPreferencesRepository =
                PersistentUserPreferencesRepository(
                    userPreferenceDao = dao,
                    playbackSpeedPreferencesStore =
                        PlaybackSpeedPreferencesStore(
                            dataStore = dataStore,
                        ),
                )
            assertEquals(expected = PlaybackSpeed.resolveDefault(), actual = repository.getPlaybackSpeed())
        }

    /**
     * 多项本地音频发现偏好覆盖保存应进入同一事务。
     */
    @Test
    fun saveLocalMusicDiscoveryPreferencesRunsInsideWriteTransaction() {
        val dao: FakeUserPreferenceDao = FakeUserPreferenceDao()
        val dataStore: DataStore<Preferences> = createTestUserPreferencesDataStore()
        var transactionCount: Int = 0
        val repository: UserPreferencesRepository =
            PersistentUserPreferencesRepository(
                userPreferenceDao = dao,
                playbackSpeedPreferencesStore =
                    PlaybackSpeedPreferencesStore(
                        dataStore = dataStore,
                    ),
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
    }

    companion object {
        /** 测试直接检查的 DataStore 倍速 key。 */
        private val PLAYBACK_SPEED_TEST_KEY: Preferences.Key<Float> = floatPreferencesKey(name = "playback_speed")

        /** 创建隔离路径的 DataStore，避免同进程测试复用同一文件实例。 */
        private fun createTestUserPreferencesDataStore(): DataStore<Preferences> =
            createUserPreferencesDataStore(
                storage =
                    OkioStorage(
                        fileSystem = FileSystem.SYSTEM,
                        serializer = PreferencesSerializer,
                        producePath = {
                            "/tmp/kmp-music-user-preferences-${Random.nextLong()}.preferences_pb".toPath()
                        },
                    ),
            )
    }
}
