package com.yanhao.kmpmusic.data

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabaseMigrations
import kotlinx.coroutines.Dispatchers

/**
 * 复用 Room builder 的基础配置，避免多平台入口各自复制 driver、迁移和查询线程设置。
 *
 * @param builder 指向 [PlaybackDatabase] 的 Room builder。
 * @return 完成底层 driver 与协程上下文配置的数据库实例。
 */
fun createPlaybackDatabase(builder: RoomDatabase.Builder<PlaybackDatabase>): PlaybackDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(PlaybackDatabaseMigrations.MIGRATION_1_2)
        .addMigrations(PlaybackDatabaseMigrations.MIGRATION_2_3)
        .addMigrations(PlaybackDatabaseMigrations.MIGRATION_3_4)
        .addMigrations(PlaybackDatabaseMigrations.MIGRATION_4_5)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
