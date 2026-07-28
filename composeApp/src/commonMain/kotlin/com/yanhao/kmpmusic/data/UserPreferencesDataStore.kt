package com.yanhao.kmpmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences

/**
 * 用户偏好 DataStore 文件名，本轮仅由播放倍速持久化使用。
 */
internal const val USER_PREFERENCES_DATA_STORE_FILE_NAME: String = "user_preferences.preferences_pb"

/**
 * 创建用户偏好 [DataStore]，具体文件系统由平台源码集提供。
 *
 * @param storage 平台侧构造的 Preferences 存储。
 * @return 可在 common 层读写的用户偏好 [DataStore]。
 */
internal fun createUserPreferencesDataStore(storage: Storage<Preferences>): DataStore<Preferences> = DataStoreFactory.create(storage = storage)
