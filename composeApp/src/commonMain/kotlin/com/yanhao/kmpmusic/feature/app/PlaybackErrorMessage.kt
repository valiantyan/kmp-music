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
        PlaybackErrorType.EngineUnavailable -> "Apple 播放组件不可用，请重启应用；若仍失败请重新安装或联系开发者。"
        PlaybackErrorType.MissingFile -> "文件不存在或已移动，请恢复原文件位置后重新扫描本地音乐。"
        PlaybackErrorType.PermissionDenied -> "无法访问该音乐文件，请重新授权文件夹或重新导入后重试。"
        PlaybackErrorType.UnsupportedFormat -> "当前音频格式暂不支持或文件受保护，请换用已验证格式的无保护本地文件。"
        PlaybackErrorType.Unknown -> "播放失败，已尝试播放下一首；请稍后重试，若持续失败请重新扫描或换用已验证格式的音频。"
    }
    return "$subject$detail"
}
