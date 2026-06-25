package com.yanhao.kmpmusic.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabaseMigrations
import kotlinx.coroutines.Dispatchers

/**
 * 为 Android 平台创建播放数据库实例，统一落到应用私有目录。
 *
 * @param context Android 应用上下文。
 * @return 已配置 SQLite driver 与 IO 查询线程的播放数据库。
 */
fun createAndroidPlaybackDatabase(context: Context): PlaybackDatabase {
    return createPlaybackDatabase(
        builder = Room.databaseBuilder<PlaybackDatabase>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath("kmp_music_playback.db").absolutePath,
        ),
    )
}

/**
 * 复用 Room builder 的基础配置，避免未来多入口创建时出现配置漂移。
 *
 * @param builder 指向 [PlaybackDatabase] 的 Room builder。
 * @return 完成底层 driver 与协程上下文配置的数据库实例。
 */
fun createPlaybackDatabase(builder: RoomDatabase.Builder<PlaybackDatabase>): PlaybackDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(PlaybackDatabaseMigrations.MIGRATION_1_2)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
