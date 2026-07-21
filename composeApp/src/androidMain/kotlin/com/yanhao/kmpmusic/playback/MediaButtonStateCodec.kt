package com.yanhao.kmpmusic.playback

import android.os.Bundle
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.PlaybackStatus

private const val ARG_SHOULD_SHOW_PAUSE_BUTTON: String = "should_show_pause_button"
private const val ARG_IS_FAVORITE: String = "is_favorite"
private const val ARG_PLAYBACK_MODE: String = "playback_mode"
private const val ARG_PLAYBACK_STATUS: String = "playback_status"
private const val ARG_HAS_ACTIVE_PLAYBACK_SESSION: String = "has_active_playback_session"

/**
 * 把 shared 媒体按钮状态编码到 Media3 custom command 参数，解析失败时返回 null。
 */
internal object MediaButtonStateCodec {
    /** 把 shared 层按钮状态编码为 [Bundle]，避免平台层直接引用 UI state。 */
    fun createUpdateButtonsArgs(state: MediaButtonState): Bundle =
        Bundle().apply {
            putBoolean(ARG_SHOULD_SHOW_PAUSE_BUTTON, state.shouldShowPauseButton)
            putBoolean(ARG_IS_FAVORITE, state.isFavorite)
            putString(ARG_PLAYBACK_MODE, state.playbackMode.name)
            putString(ARG_PLAYBACK_STATUS, state.playbackStatus.name)
            putBoolean(ARG_HAS_ACTIVE_PLAYBACK_SESSION, state.hasActivePlaybackSession)
        }

    /** 从 custom command 参数中恢复按钮状态，解析失败时拒绝更新 session。 */
    fun resolveUpdateButtonsState(args: Bundle): MediaButtonState? {
        val playbackMode: PlaybackMode =
            args
                .getString(ARG_PLAYBACK_MODE)
                ?.let { value: String -> runCatching { PlaybackMode.valueOf(value) }.getOrNull() }
                ?: return null
        val playbackStatus: PlaybackStatus =
            args
                .getString(ARG_PLAYBACK_STATUS)
                ?.let { value: String -> runCatching { PlaybackStatus.valueOf(value) }.getOrNull() }
                ?: return null
        return MediaButtonState(
            shouldShowPauseButton = args.getBoolean(ARG_SHOULD_SHOW_PAUSE_BUTTON),
            isFavorite = args.getBoolean(ARG_IS_FAVORITE),
            playbackMode = playbackMode,
            playbackStatus = playbackStatus,
            hasActivePlaybackSession = args.getBoolean(ARG_HAS_ACTIVE_PLAYBACK_SESSION),
        )
    }
}
