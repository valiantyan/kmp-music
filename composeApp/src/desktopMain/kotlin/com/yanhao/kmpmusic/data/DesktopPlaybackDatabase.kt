package com.yanhao.kmpmusic.data

import androidx.room3.Room
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import java.io.File

/**
 * 为 Desktop 平台创建播放数据库实例，统一落到 macOS Application Support。
 *
 * @param userHome 当前用户 home 目录，默认使用进程运行环境的 `user.home`。
 * @return 已复用共享 Room 配置的播放数据库。
 */
fun createDesktopPlaybackDatabase(
    userHome: String = System.getProperty("user.home"),
): PlaybackDatabase {
    val databasePath: String = defaultDesktopPlaybackDatabasePath(userHome = userHome)
    File(databasePath).parentFile.mkdirs()
    return createPlaybackDatabase(
        builder = Room.databaseBuilder<PlaybackDatabase>(
            name = databasePath,
        ),
    )
}

/**
 * 返回 Desktop 播放数据库的默认存储路径。
 *
 * @param userHome 当前用户 home 目录。
 * @return `~/Library/Application Support/KMP Music/kmp_music_playback.db` 的绝对路径。
 */
fun defaultDesktopPlaybackDatabasePath(userHome: String): String {
    return File(
        File(userHome, "Library/Application Support/KMP Music"),
        "kmp_music_playback.db",
    ).absolutePath
}
