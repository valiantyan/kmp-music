package com.yanhao.kmpmusic.domain.persistence

import androidx.room3.RoomDatabaseConstructor

/**
 * Android 端通过 [com.yanhao.kmpmusic.data.createAndroidPlaybackDatabase] 创建数据库，
 * 因此这里只提供满足 expect/actual 编译边界的占位 actual。
 */
actual object PlaybackDatabaseConstructor : RoomDatabaseConstructor<PlaybackDatabase> {
    /**
     * 显式阻止 Android 误走无 Context 的数据库初始化路径。
     */
    actual override fun initialize(): PlaybackDatabase {
        error("Android must create PlaybackDatabase with createAndroidPlaybackDatabase().")
    }
}
