package com.yanhao.kmpmusic.domain.persistence

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * 播放数据库迁移集合，禁止使用破坏性迁移以保留播放快照和收藏数据。
 */
object PlaybackDatabaseMigrations {
    /** 从播放/收藏数据库升级到包含本地歌曲表。 */
    val MIGRATION_1_2: Migration = object : Migration(startVersion = 1, endVersion = 2) {
        /** 创建 [local_song] 表及查询所需索引，确保升级后可直接承载扫描结果。 */
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSql(
                """
                CREATE TABLE IF NOT EXISTS local_song (
                    id TEXT NOT NULL PRIMARY KEY,
                    sourceId TEXT NOT NULL,
                    sourceKind TEXT NOT NULL,
                    localUri TEXT NOT NULL,
                    fileName TEXT NOT NULL,
                    title TEXT,
                    artist TEXT,
                    album TEXT,
                    durationMs INTEGER,
                    mimeType TEXT,
                    sizeBytes INTEGER,
                    modifiedAt INTEGER,
                    coverArt TEXT NOT NULL,
                    lastScannedAt INTEGER NOT NULL,
                    isAvailable INTEGER NOT NULL
                )
                """,
            )
            connection.execSql(
                "CREATE INDEX IF NOT EXISTS index_local_song_sourceKind_isAvailable ON local_song(sourceKind, isAvailable)",
            )
            connection.execSql(
                "CREATE INDEX IF NOT EXISTS index_local_song_isAvailable_modifiedAt ON local_song(isAvailable, modifiedAt)",
            )
        }
    }

    /** 从本地歌曲表升级到支持扫描封面 URI。 */
    val MIGRATION_2_3: Migration = object : Migration(startVersion = 2, endVersion = 3) {
        /** 新列允许为空，旧扫描记录继续使用应用内封面兜底。 */
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSql("ALTER TABLE local_song ADD COLUMN coverImageUri TEXT")
        }
    }

    /** 从本地歌曲封面版本升级到支持搜索历史持久化。 */
    val MIGRATION_3_4: Migration = object : Migration(startVersion = 3, endVersion = 4) {
        /** 创建按上下文隔离的搜索历史表。 */
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSql(
                """
                CREATE TABLE IF NOT EXISTS search_history (
                    context TEXT NOT NULL,
                    query TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(context, query)
                )
                """,
            )
        }
    }
}

/** 执行裁剪后的 SQL 文本，避免多行字符串首尾空白影响 SQLite 解析。 */
private fun SQLiteConnection.execSql(sql: String) {
    prepare(sql.trimIndent()).use { statement ->
        statement.step()
    }
}
