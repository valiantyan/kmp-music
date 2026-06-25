package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType

/**
 * vlcj 回调使用的媒体快照，只保存当前 prepare 对应的不可变归因信息。
 */
internal data class VlcjMediaCallbackSnapshot(
    val generation: Long,
    val songId: String,
) {
    /**
     * 构造播放中事件，保证归因只来自当前快照。
     */
    fun playing(
        positionMs: Long,
        durationMs: Long?,
    ): DesktopMediaPlayerEvent.Playing {
        return DesktopMediaPlayerEvent.Playing(
            generation = generation,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /**
     * 构造暂停事件，保证归因只来自当前快照。
     */
    fun paused(
        positionMs: Long,
        durationMs: Long?,
    ): DesktopMediaPlayerEvent.Paused {
        return DesktopMediaPlayerEvent.Paused(
            generation = generation,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    /**
     * 构造完成事件，保证归因只来自当前快照。
     */
    fun finished(): DesktopMediaPlayerEvent.Finished {
        return DesktopMediaPlayerEvent.Finished(generation = generation)
    }

    /**
     * 构造失败事件，保证 songId 不会被后续媒体覆盖。
     */
    fun failed(): DesktopMediaPlayerEvent.Failed {
        return DesktopMediaPlayerEvent.Failed(
            generation = generation,
            error = PlaybackError(
                type = PlaybackErrorType.Unknown,
                songId = songId,
                message = "播放失败，已尝试播放下一首。",
            ),
        )
    }
}

/**
 * 生成运行时缺失时的确定性错误，不再尝试系统级发现。
 */
internal fun buildEngineUnavailableError(songId: String): PlaybackError {
    return PlaybackError(
        type = PlaybackErrorType.EngineUnavailable,
        songId = songId,
        message = "播放器组件不可用，请重新安装应用或联系开发者。",
    )
}

/**
 * 把媒体 URI 转成可诊断的播放错误，供 prepare 失败时使用。
 */
internal fun String.toPlaybackError(
    songId: String,
): PlaybackError {
    val file: java.io.File? = runCatching {
        java.io.File(java.net.URI(this))
    }.getOrNull()
    val type: PlaybackErrorType = when {
        file != null && !file.exists() -> PlaybackErrorType.MissingFile
        file != null && !file.canRead() -> PlaybackErrorType.PermissionDenied
        else -> PlaybackErrorType.Unknown
    }
    return PlaybackError(
        type = type,
        songId = songId,
        message = when (type) {
            PlaybackErrorType.MissingFile -> "文件不存在或已移动，请重新扫描本地音乐。"
            PlaybackErrorType.PermissionDenied -> "无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。"
            PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持，已尝试播放下一首。"
            PlaybackErrorType.EngineUnavailable -> "播放器组件不可用，请重新安装应用或联系开发者。"
            PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首。"
        },
    )
}
