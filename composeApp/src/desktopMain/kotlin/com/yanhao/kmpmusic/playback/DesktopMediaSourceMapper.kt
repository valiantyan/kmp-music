package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.AudioSource
import com.yanhao.kmpmusic.domain.model.PlayableMedia

/**
 * 统一收口桌面端可播放媒体到实际播放 URI 的映射，避免引擎内部重复理解来源模型。
 */
internal object DesktopMediaSourceMapper {
    /** 按桌面播放引擎当前支持的来源类型生成底层适配器需要的 URI。 */
    fun playbackUri(media: PlayableMedia): String {
        return when (val source: AudioSource = media.audioSource) {
            is AudioSource.Local -> source.uri
        }
    }
}
