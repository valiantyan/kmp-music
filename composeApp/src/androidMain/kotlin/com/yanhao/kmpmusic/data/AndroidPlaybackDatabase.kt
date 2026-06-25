package com.yanhao.kmpmusic.data

import android.content.Context
import androidx.room3.Room
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase

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
