package com.yanhao.kmpmusic

/**
 * iOS 宿主后台音频配置证据，当前仓库产出 framework，真正的 App Info.plist 需由宿主工程应用。
 */
object IosPlaybackHostConfiguration {
    /** iOS App 宿主必须配置的 Info.plist key。 */
    const val BACKGROUND_MODES_KEY: String = "UIBackgroundModes"

    /** iOS 后台音频需要声明的 background mode 值。 */
    const val AUDIO_BACKGROUND_MODE: String = "audio"

    /** 当前 P0 不包含的系统播放专项能力，避免交接时误宣称已完成。 */
    val excludedSystemPlaybackFeatures: List<String> = listOf(
        "MPNowPlayingInfoCenter",
        "MPRemoteCommandCenter",
        "ControlCenterButtons",
        "HeadsetRemoteCommands",
        "ColdStartPlaybackResume",
    )
}
