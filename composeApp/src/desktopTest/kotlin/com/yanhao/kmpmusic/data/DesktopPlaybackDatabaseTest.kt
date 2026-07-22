package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.persistence.FavoriteSongEntity
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPlaybackDatabaseTest {
    @Test
    fun databasePathUsesMacosApplicationSupport() {
        val path =
            defaultDesktopPlaybackDatabasePath(
                userHome = "/Users/tester",
            )
        assertTrue(path.endsWith("Library/Application Support/KMP Music/kmp_music_playback.db"))
    }

    @Test
    fun databaseCanInstantiateGeneratedRoomImplementation(): Unit =
        runBlocking {
            val tempDir = Files.createTempDirectory("kmp-music-desktop-db")
            val database =
                createDesktopPlaybackDatabaseAtPath(
                    databasePath = tempDir.resolve("playback.db").toString(),
                )
            try {
                assertEquals(
                    expected = 0,
                    actual = database.localSongDao().countAvailableSongs(),
                )
            } finally {
                database.close()
            }
        }

    /** 收藏 DAO 必须按收藏更新时间从新到旧返回歌曲标识。 */
    @Test
    fun favoriteSongDaoReturnsNewestFavoriteFirst(): Unit =
        runBlocking {
            val tempDir = Files.createTempDirectory("kmp-music-favorites-order-db")
            val database =
                createDesktopPlaybackDatabaseAtPath(
                    databasePath = tempDir.resolve("playback.db").toString(),
                )
            try {
                database.favoriteSongDao().saveFavorite(
                    entity = FavoriteSongEntity(songId = "a-older", updatedAt = 100L),
                )
                database.favoriteSongDao().saveFavorite(
                    entity = FavoriteSongEntity(songId = "z-newer", updatedAt = 200L),
                )
                database.favoriteSongDao().saveFavorite(
                    entity = FavoriteSongEntity(songId = "a-same-time-older", updatedAt = 300L),
                )
                database.favoriteSongDao().saveFavorite(
                    entity = FavoriteSongEntity(songId = "z-same-time-newer", updatedAt = 300L),
                )
                assertEquals(
                    expected =
                        listOf(
                            "z-same-time-newer",
                            "a-same-time-older",
                            "z-newer",
                            "a-older",
                        ),
                    actual = database.favoriteSongDao().getFavoriteSongIds(),
                )
            } finally {
                database.close()
            }
        }
}
