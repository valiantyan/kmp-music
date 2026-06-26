package com.yanhao.kmpmusic.data

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPlaybackDatabaseTest {
    @Test
    fun databasePathUsesMacosApplicationSupport(): Unit {
        val path = defaultDesktopPlaybackDatabasePath(
            userHome = "/Users/tester",
        )
        assertTrue(path.endsWith("Library/Application Support/KMP Music/kmp_music_playback.db"))
    }

    @Test
    fun databaseCanInstantiateGeneratedRoomImplementation(): Unit = runBlocking {
        val tempDir = Files.createTempDirectory("kmp-music-desktop-db")
        val database = createDesktopPlaybackDatabaseAtPath(
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
}
