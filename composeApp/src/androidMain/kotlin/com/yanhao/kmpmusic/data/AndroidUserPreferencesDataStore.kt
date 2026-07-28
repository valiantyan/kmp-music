package com.yanhao.kmpmusic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer

/**
 * 为 Android 平台创建用户偏好 DataStore，文件落在 App 私有 files 目录。
 *
 * @param context Android 应用上下文。
 * @return 用户偏好 [DataStore]。
 */
internal fun createAndroidUserPreferencesDataStore(context: Context): DataStore<Preferences> =
    createUserPreferencesDataStore(
        storage =
            FileStorage(
                serializer = PreferencesFileSerializer,
                produceFile = {
                    context.applicationContext.filesDir.resolve(USER_PREFERENCES_DATA_STORE_FILE_NAME)
                },
            ),
    )
