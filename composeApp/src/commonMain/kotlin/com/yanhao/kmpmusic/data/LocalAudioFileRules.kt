package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * 本地音频文件识别规则，供各平台真实 scanner 复用同一套 P0 格式判断。
 */
object LocalAudioFileRules {
    // P0 支持的本地音频扩展名与 MIME 类型映射。
    private val audioTypesByExtension: Map<String, LocalAudioType> = listOf(
        LocalAudioType(extension = "mp3", mimeType = "audio/mpeg"),
        LocalAudioType(extension = "m4a", mimeType = "audio/mp4"),
        LocalAudioType(extension = "m4b", mimeType = "audio/mp4"),
        LocalAudioType(extension = "aac", mimeType = "audio/aac"),
        LocalAudioType(extension = "wav", mimeType = "audio/wav"),
        LocalAudioType(extension = "flac", mimeType = "audio/flac"),
        LocalAudioType(extension = "ogg", mimeType = "audio/ogg"),
        LocalAudioType(extension = "oga", mimeType = "audio/ogg"),
        LocalAudioType(extension = "opus", mimeType = "audio/opus"),
        LocalAudioType(extension = "aif", mimeType = "audio/aiff"),
        LocalAudioType(extension = "aiff", mimeType = "audio/aiff"),
        LocalAudioType(extension = "alac", mimeType = "audio/alac"),
        LocalAudioType(extension = "amr", mimeType = "audio/amr"),
    ).associateBy { audioType -> audioType.extension }

    /** 根据文件名判断是否是 P0 支持的音频文件。 */
    fun matchAudioType(fileName: String): LocalAudioType? {
        val extension: String = fileName.substringAfterLast(
            delimiter = ".",
            missingDelimiterValue = "",
        ).lowercase()
        if (extension.isBlank()) {
            return null
        }
        return audioTypesByExtension[extension]
    }

    /** 从文件名生成标题兜底，避免平台 scanner 重复处理扩展名。 */
    fun titleFromFileName(fileName: String): String {
        return fileName.substringBeforeLast(
            delimiter = ".",
            missingDelimiterValue = fileName,
        )
    }

    /** 为没有内嵌封面的本地文件使用明确的本地音乐占位封面。 */
    fun coverForSourceId(sourceId: String): CoverArt {
        return CoverArt.HeroLocalMusic
    }
}

/**
 * 支持的音频文件类型。
 *
 * @property extension 小写文件扩展名。
 * @property mimeType 进入曲库快照的 MIME 类型。
 */
data class LocalAudioType(
    val extension: String,
    val mimeType: String,
)
