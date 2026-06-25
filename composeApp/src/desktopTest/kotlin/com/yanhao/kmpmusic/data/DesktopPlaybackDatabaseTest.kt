package com.yanhao.kmpmusic.data

import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopPlaybackDatabaseTest {
    @Test
    fun databasePathUsesMacosApplicationSupport(): Unit {
        val path = defaultDesktopPlaybackDatabasePath(
            userHome = "/Users/tester",
        )
        assertTrue(path.endsWith("Library/Application Support/KMP Music/kmp_music_playback.db"))
    }
}
