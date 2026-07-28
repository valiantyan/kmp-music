package com.yanhao.kmpmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * 为 iOS 平台创建用户偏好 DataStore，文件落在宿主 App Documents 目录。
 *
 * @return 用户偏好 [DataStore]。
 */
@OptIn(ExperimentalForeignApi::class)
internal fun createIosUserPreferencesDataStore(): DataStore<Preferences> =
    createUserPreferencesDataStore(
        storage =
            OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = {
                    val documentDirectory: NSURL? =
                        NSFileManager.defaultManager.URLForDirectory(
                            directory = NSDocumentDirectory,
                            inDomain = NSUserDomainMask,
                            appropriateForURL = null,
                            create = false,
                            error = null,
                        )
                    "${requireNotNull(documentDirectory).path}/$USER_PREFERENCES_DATA_STORE_FILE_NAME".toPath()
                },
            ),
    )
