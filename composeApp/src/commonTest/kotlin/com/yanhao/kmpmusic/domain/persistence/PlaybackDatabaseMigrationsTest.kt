package com.yanhao.kmpmusic.domain.persistence

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证播放数据库迁移不会破坏既有本地数据。
 */
class PlaybackDatabaseMigrationsTest {
    /**
     * 从版本 7 升级到版本 8 时应只新增歌单表，旧播放、收藏、搜索、曲库和偏好数据保留。
     */
    @Test
    fun migrationSevenToEightCreatesPlaylistTablesAndKeepsExistingRows(): Unit =
        runTest {
            BundledSQLiteDriver().open(fileName = ":memory:").use { connection: SQLiteConnection ->
                createVersionSevenSchema(connection = connection)
                insertVersionSevenSampleRows(connection = connection)

                PlaybackDatabaseMigrations.MIGRATION_7_8.migrate(connection = connection)

                assertEquals(expected = 1, actual = connection.countRows(tableName = "playback_snapshot"))
                assertEquals(expected = 1, actual = connection.countRows(tableName = "playback_queue_item"))
                assertEquals(expected = 1, actual = connection.countRows(tableName = "playback_history_item"))
                assertEquals(expected = 1, actual = connection.countRows(tableName = "favorite_song"))
                assertEquals(expected = 1, actual = connection.countRows(tableName = "local_song"))
                assertEquals(expected = 1, actual = connection.countRows(tableName = "search_history"))
                assertEquals(expected = 1, actual = connection.countRows(tableName = "user_preference"))
                assertEquals(expected = 0, actual = connection.countRows(tableName = "local_playlist"))
                assertEquals(expected = 0, actual = connection.countRows(tableName = "local_playlist_song"))
            }
        }

    // 建立与版本 7 schema 等价的最小样本库，避免迁移测试依赖生成代码。
    private fun createVersionSevenSchema(connection: SQLiteConnection) {
        connection.execSql(
            """
            CREATE TABLE playback_snapshot (
                id INTEGER NOT NULL PRIMARY KEY,
                currentSongId TEXT,
                currentIndex INTEGER NOT NULL,
                playbackMode TEXT NOT NULL,
                positionMs INTEGER NOT NULL,
                durationMs INTEGER,
                updatedAt INTEGER NOT NULL
            )
            """,
        )
        connection.execSql("CREATE TABLE playback_queue_item (position INTEGER NOT NULL PRIMARY KEY, songId TEXT NOT NULL)")
        connection.execSql(
            """
            CREATE TABLE playback_history_item (
                position INTEGER NOT NULL PRIMARY KEY,
                songId TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """,
        )
        connection.execSql("CREATE TABLE favorite_song (songId TEXT NOT NULL PRIMARY KEY, updatedAt INTEGER NOT NULL)")
        connection.execSql(
            """
            CREATE TABLE local_song (
                id TEXT NOT NULL PRIMARY KEY,
                sourceId TEXT NOT NULL,
                sourceKind TEXT NOT NULL,
                concreteSourceId TEXT,
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
                coverImageUri TEXT,
                lastScannedAt INTEGER NOT NULL,
                isAvailable INTEGER NOT NULL
            )
            """,
        )
        connection.execSql("CREATE TABLE search_history (context TEXT NOT NULL, query TEXT NOT NULL, position INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(context, query))")
        connection.execSql("CREATE TABLE user_preference (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
    }

    // 写入每张旧表一行，迁移后逐表计数确认未丢数据。
    private fun insertVersionSevenSampleRows(connection: SQLiteConnection) {
        connection.execSql("INSERT INTO playback_snapshot VALUES (1, 'song-1', 0, 'LoopAll', 1200, 3000, 10)")
        connection.execSql("INSERT INTO playback_queue_item VALUES (0, 'song-1')")
        connection.execSql("INSERT INTO playback_history_item VALUES (0, 'song-1', 11)")
        connection.execSql("INSERT INTO favorite_song VALUES ('song-1', 12)")
        connection.execSql(
            """
            INSERT INTO local_song VALUES (
                'song-1',
                'source-1',
                'fakeScanner',
                NULL,
                'fake://song-1',
                'song-1.mp3',
                'Song 1',
                'Artist',
                'Album',
                3000,
                'audio/mpeg',
                1000,
                20,
                'HeroLocalMusic',
                NULL,
                30,
                1
            )
            """,
        )
        connection.execSql("INSERT INTO search_history VALUES ('LocalLibrary', 'Song', 0, 13)")
        connection.execSql("INSERT INTO user_preference VALUES ('themeMode', 'System', 14)")
    }

    // 执行无返回值 SQL。
    private fun SQLiteConnection.execSql(sql: String) {
        prepare(sql = sql.trimIndent()).use { statement ->
            statement.step()
        }
    }

    // 读取表行数，作为迁移数据保留的稳定证据。
    private fun SQLiteConnection.countRows(tableName: String): Int {
        prepare(sql = "SELECT COUNT(*) FROM $tableName").use { statement ->
            statement.step()
            return statement.getInt(index = 0)
        }
    }
}
