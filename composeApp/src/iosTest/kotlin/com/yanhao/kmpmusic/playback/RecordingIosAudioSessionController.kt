package com.yanhao.kmpmusic.playback

/**
 * 记录 iOS audio session 配置顺序的测试实现。
 */
internal class RecordingIosAudioSessionController(
    // 与 fake bridge 共享的顺序记录。
    private val order: MutableList<String>,
) : IosAudioSessionController {
    /** 是否已经配置并激活 playback category。 */
    var isConfiguredForPlayback: Boolean = false
        private set

    /** 是否已经释放 audio session。 */
    var isReleased: Boolean = false
        private set

    /** 记录 playback category 配置。 */
    override fun configureForPlayback(): Boolean {
        order += "audio-session:configure-playback"
        isConfiguredForPlayback = true
        return true
    }

    /** 记录释放路径。 */
    override fun release() {
        order += "audio-session:release"
        isReleased = true
    }
}
