package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType

/**
 * 将播放诊断错误转换为面向用户的提示文案。
 */
fun PlaybackError.userMessage(songTitle: String?): String {
    val subject: String = songTitle
        ?.takeIf { title -> title.isNotBlank() }
        ?.let { title -> "《$title》" }
        ?: "当前歌曲"
    val detail: String = when (type) {
        PlaybackErrorType.EngineUnavailable -> "播放器组件不可用，请重新安装应用或联系开发者。"
        PlaybackErrorType.MissingFile -> "文件不存在或已移动，请重新扫描本地音乐。"
        PlaybackErrorType.PermissionDenied -> "无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。"
        PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持，已尝试播放下一首。"
        PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首。"
    }
    return "$subject$detail"
}
