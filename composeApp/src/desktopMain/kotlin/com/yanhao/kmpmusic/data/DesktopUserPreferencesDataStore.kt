package com.yanhao.kmpmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import java.io.File

/**
 * 为 Desktop 平台创建用户偏好 DataStore，统一落到 macOS Application Support。
 *
 * @param userHome 当前用户 home 目录，默认使用进程运行环境的 `user.home`。
 * @return 用户偏好 [DataStore]。
 */
internal fun createDesktopUserPreferencesDataStore(
    userHome: String = System.getProperty("user.home"),
): DataStore<Preferences> =
    createDesktopUserPreferencesDataStoreAtPath(
        dataStorePath = defaultDesktopUserPreferencesDataStorePath(userHome = userHome),
    )

/**
 * 为 Desktop 平台创建指定路径的用户偏好 DataStore，供测试和 smoke 场景复用同一文件。
 *
 * @param dataStorePath DataStore 文件绝对路径。
 * @return 用户偏好 [DataStore]。
 */
internal fun createDesktopUserPreferencesDataStoreAtPath(dataStorePath: String): DataStore<Preferences> {
    File(dataStorePath).parentFile.mkdirs()
    return createUserPreferencesDataStore(
        storage =
            FileStorage(
                serializer = PreferencesFileSerializer,
                produceFile = { File(dataStorePath) },
            ),
    )
}

/**
 * 返回 Desktop 用户偏好 DataStore 的默认存储路径。
 *
 * @param userHome 当前用户 home 目录。
 * @return `~/Library/Application Support/KMP Music/user_preferences.preferences_pb` 的绝对路径。
 */
internal fun defaultDesktopUserPreferencesDataStorePath(userHome: String): String =
    File(
        File(userHome, "Library/Application Support/KMP Music"),
        USER_PREFERENCES_DATA_STORE_FILE_NAME,
    ).absolutePath
