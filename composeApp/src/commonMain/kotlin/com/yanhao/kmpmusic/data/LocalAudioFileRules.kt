package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences

/**
 * 本地音频文件识别规则，供各平台真实 scanner 复用同一套 P0 格式判断。
 */
object LocalAudioFileRules {
    // P0 扫描入口只接收 Apple 矩阵中已经验证的格式，避免播放阶段才暴露不支持。
    private val audioTypesByExtension: Map<String, LocalAudioType> =
        AppleAudioFormatSupportMatrix.scannableAudioTypesByExtension

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

    /** 按用户偏好判断音频是否应进入本地音频发现结果，未知时长默认保留。 */
    fun shouldIncludeByDuration(
        durationMs: Long?,
        preferences: LocalMusicDiscoveryPreferences,
    ): Boolean {
        if (!preferences.shouldIgnoreShortAudio) {
            return true
        }
        val knownDurationMs: Long = durationMs ?: return true
        return knownDurationMs >= MINIMUM_MUSIC_DURATION_MS
    }

    /** 短音频过滤阈值(30 秒)，用于过滤通知音和语音消息。 */
    const val MINIMUM_MUSIC_DURATION_MS: Long = 30_000L
}

/**
 * Apple 平台 P0 格式支持矩阵，作为扫描入口、文档和 smoke 输出的同一事实源。
 */
object AppleAudioFormatSupportMatrix {
    /**
     * 当前 Apple P0 格式矩阵；[AppleAudioFormatSupportStatus.PendingVerification] 条目不会进入扫描可播放集合。
     */
    val entries: List<AppleAudioFormatSupport> = listOf(
        AppleAudioFormatSupport(
            formatName = "MP3",
            status = AppleAudioFormatSupportStatus.Supported,
            audioTypes = listOf(LocalAudioType(extension = "mp3", mimeType = "audio/mpeg")),
            evidence = "macOS：格式 smoke 用本机编码器生成 MP3 样本，并通过 AVFoundation 可播放性检查；iOS：导入扫描按 Apple P0 放行，真实样本播放仍需真机或后续 gate 验证。",
            note = "作为 Apple P0 格式进入扫描结果；iOS 播放风险必须在交接中保留，不把 macOS smoke 伪装成 iOS 播放证据。",
        ),
        AppleAudioFormatSupport(
            formatName = "M4A/AAC",
            status = AppleAudioFormatSupportStatus.Supported,
            audioTypes = listOf(
                LocalAudioType(extension = "m4a", mimeType = "audio/mp4"),
                LocalAudioType(extension = "aac", mimeType = "audio/aac"),
            ),
            evidence = "macOS：AVFoundation bridge smoke 已真实播放 M4A/AAC 样本，并对 M4A 与 AAC ADTS 执行可播放性检查；iOS：导入扫描按 Apple P0 放行，真实样本播放仍需真机或后续 gate 验证。",
            note = "AAC ADTS 和 M4A 容器作为 Apple P0 格式进入扫描结果；iOS 真实样本播放仍需真机或后续 gate 验证。",
        ),
        AppleAudioFormatSupport(
            formatName = "WAV",
            status = AppleAudioFormatSupportStatus.Supported,
            audioTypes = listOf(LocalAudioType(extension = "wav", mimeType = "audio/wav")),
            evidence = "macOS：格式 smoke 生成 WAV 样本，并通过 AVFoundation 可播放性检查；iOS：导入扫描按 Apple P0 放行，真实样本播放仍需真机或后续 gate 验证。",
            note = "PCM WAV 作为 Apple P0 格式进入扫描结果；iOS 播放风险必须在交接中保留。",
        ),
        AppleAudioFormatSupport(
            formatName = "FLAC",
            status = AppleAudioFormatSupportStatus.Supported,
            audioTypes = listOf(LocalAudioType(extension = "flac", mimeType = "audio/flac")),
            evidence = "macOS：格式 smoke 生成 FLAC 样本，并通过 AVFoundation 可播放性检查；iOS：导入扫描按 Apple P0 放行，真实样本播放仍需真机或后续 gate 验证。",
            note = "FLAC 作为 Apple P0 格式进入扫描结果；iOS 播放风险必须在交接中保留。",
        ),
        AppleAudioFormatSupport(
            formatName = "AIFF/ALAC",
            status = AppleAudioFormatSupportStatus.Supported,
            audioTypes = listOf(
                LocalAudioType(extension = "aif", mimeType = "audio/aiff"),
                LocalAudioType(extension = "aiff", mimeType = "audio/aiff"),
            ),
            evidence = "macOS：格式 smoke 生成 AIFF 和 ALAC-in-M4A 样本，并通过 AVFoundation 可播放性检查；iOS：导入扫描按 Apple P0 放行，真实样本播放仍需真机或后续 gate 验证。",
            note = "AIFF 通过 .aif/.aiff 进入扫描；ALAC 以 .m4a 容器进入扫描，不单独放行未验证的 .alac 扩展名；iOS 播放风险必须在交接中保留。",
        ),
        AppleAudioFormatSupport(
            formatName = "OGG/OPUS",
            status = AppleAudioFormatSupportStatus.PendingVerification,
            audioTypes = listOf(
                LocalAudioType(extension = "ogg", mimeType = "audio/ogg"),
                LocalAudioType(extension = "oga", mimeType = "audio/ogg"),
                LocalAudioType(extension = "opus", mimeType = "audio/opus"),
            ),
            evidence = "本票未取得 iOS 与 macOS 双平台真实样本播放证据。",
            note = "扫描入口暂不放行，避免沿用旧第三方播放器格式假设。",
        ),
        AppleAudioFormatSupport(
            formatName = "AMR",
            status = AppleAudioFormatSupportStatus.PendingVerification,
            audioTypes = listOf(
                LocalAudioType(extension = "amr", mimeType = "audio/amr"),
                LocalAudioType(extension = "awb", mimeType = "audio/amr-wb"),
            ),
            evidence = "本票未取得 iOS 与 macOS 双平台真实样本播放证据。",
            note = "扫描入口暂不放行，避免把语音编码误收为普通可播放音乐。",
        ),
    )

    // 供扫描入口消费的已验证扩展名映射，待验证格式只保留在矩阵和文档中。
    internal val scannableAudioTypesByExtension: Map<String, LocalAudioType> = entries
        .filter { support: AppleAudioFormatSupport -> support.allowsScanning }
        .flatMap { support: AppleAudioFormatSupport -> support.audioTypes }
        .associateBy { audioType: LocalAudioType -> audioType.extension }
}

/**
 * Apple 格式矩阵的验证状态。
 */
enum class AppleAudioFormatSupportStatus {
    Supported,
    PendingVerification,
}

/**
 * 单个 Apple 平台音频格式结论。
 *
 * @property formatName 面向文档和 smoke 输出的格式名称。
 * @property status 当前验证状态。
 * @property audioTypes 该格式关联的文件扩展名和 MIME 类型。
 * @property evidence 支撑结论的证据来源。
 * @property note 面向实现和人工验收的边界说明。
 */
data class AppleAudioFormatSupport(
    val formatName: String,
    val status: AppleAudioFormatSupportStatus,
    val audioTypes: List<LocalAudioType>,
    val evidence: String,
    val note: String,
) {
    /**
     * 是否允许平台 scanner 把该格式发布为可播放曲目。
     */
    val allowsScanning: Boolean
        get() = status == AppleAudioFormatSupportStatus.Supported
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
