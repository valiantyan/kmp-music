package com.yanhao.kmpmusic.data

import androidx.room3.withWriteTransaction
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.UserPreferenceDao
import com.yanhao.kmpmusic.domain.persistence.UserPreferenceEntity
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.runBlocking

/**
 * 基于 Room 的用户偏好仓库，保存主题和本地音频发现偏好。
 */
class PersistentUserPreferencesRepository(
    private val userPreferenceDao: UserPreferenceDao,
    private val runInWriteTransaction: suspend (suspend () -> Unit) -> Unit = { block -> block() },
    private val nowMillis: () -> Long = { currentTimeMillis() },
) : UserPreferencesRepository {
    /** 读取主题偏好，遇到旧值或非法值时回退到浅色主题。 */
    override fun getThemeMode(): ThemeMode =
        runBlocking {
            userPreferenceDao
                .getValue(key = KEY_THEME_MODE)
                ?.let { value: String -> ThemeMode.entries.firstOrNull { mode: ThemeMode -> mode.name == value } }
                ?: ThemeMode.Light
        }

    /** 保存主题偏好。 */
    override fun saveThemeMode(themeMode: ThemeMode) {
        runBlocking {
            saveValue(
                key = KEY_THEME_MODE,
                value = themeMode.name,
            )
        }
    }

    /** 读取本地音频发现偏好，未保存的字段使用产品默认值。 */
    override fun getLocalMusicDiscoveryPreferences(): LocalMusicDiscoveryPreferences =
        runBlocking {
            val defaults: LocalMusicDiscoveryPreferences = LocalMusicDiscoveryPreferences()
            LocalMusicDiscoveryPreferences(
                isAutoScanOnLaunchEnabled =
                    readBoolean(
                        key = KEY_LOCAL_MUSIC_AUTO_SCAN_ON_LAUNCH,
                        defaultValue = defaults.isAutoScanOnLaunchEnabled,
                    ),
                shouldIgnoreShortAudio =
                    readBoolean(
                        key = KEY_LOCAL_MUSIC_IGNORE_SHORT_AUDIO,
                        defaultValue = defaults.shouldIgnoreShortAudio,
                    ),
                shouldExcludeSystemFolders =
                    readBoolean(
                        key = KEY_LOCAL_MUSIC_EXCLUDE_SYSTEM_FOLDERS,
                        defaultValue = defaults.shouldExcludeSystemFolders,
                    ),
            )
        }

    /** 保存本地音频发现偏好，三项设置使用同一事务保持一致。 */
    override fun saveLocalMusicDiscoveryPreferences(preferences: LocalMusicDiscoveryPreferences) {
        runBlocking {
            runInWriteTransaction {
                saveValue(
                    key = KEY_LOCAL_MUSIC_AUTO_SCAN_ON_LAUNCH,
                    value = preferences.isAutoScanOnLaunchEnabled.toString(),
                )
                saveValue(
                    key = KEY_LOCAL_MUSIC_IGNORE_SHORT_AUDIO,
                    value = preferences.shouldIgnoreShortAudio.toString(),
                )
                saveValue(
                    key = KEY_LOCAL_MUSIC_EXCLUDE_SYSTEM_FOLDERS,
                    value = preferences.shouldExcludeSystemFolders.toString(),
                )
            }
        }
    }

    // 从偏好表读取布尔值，旧值异常时回退默认，避免阻塞 App 启动。
    private suspend fun readBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean =
        when (userPreferenceDao.getValue(key = key)) {
            "true" -> true
            "false" -> false
            else -> defaultValue
        }

    // 覆盖保存单个 key/value 偏好记录。
    private suspend fun saveValue(
        key: String,
        value: String,
    ) {
        userPreferenceDao.savePreference(
            entity =
                UserPreferenceEntity(
                    key = key,
                    value = value,
                    updatedAt = nowMillis(),
                ),
        )
    }

    companion object {
        /** 主题模式偏好键。 */
        private const val KEY_THEME_MODE: String = "themeMode"

        /** 启动时自动扫描偏好键。 */
        private const val KEY_LOCAL_MUSIC_AUTO_SCAN_ON_LAUNCH: String = "localMusic.autoScanOnLaunch"

        /** 忽略短音频偏好键。 */
        private const val KEY_LOCAL_MUSIC_IGNORE_SHORT_AUDIO: String = "localMusic.ignoreShortAudio"

        /** 排除系统文件夹偏好键。 */
        private const val KEY_LOCAL_MUSIC_EXCLUDE_SYSTEM_FOLDERS: String = "localMusic.excludeSystemFolders"

        /**
         * 从 [PlaybackDatabase] 创建仓库，保证多项偏好覆盖写入时保持事务一致。
         */
        fun create(
            playbackDatabase: PlaybackDatabase,
            nowMillis: () -> Long = { currentTimeMillis() },
        ): PersistentUserPreferencesRepository =
            PersistentUserPreferencesRepository(
                userPreferenceDao = playbackDatabase.userPreferenceDao(),
                runInWriteTransaction = { block: suspend () -> Unit ->
                    playbackDatabase.withWriteTransaction {
                        block()
                    }
                },
                nowMillis = nowMillis,
            )
    }
}
