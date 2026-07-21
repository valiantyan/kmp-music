package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult

/**
 * Media3 custom command dispatcher。它只负责转发命令，不直接读写 [com.yanhao.kmpmusic.feature.app.MusicAppUiState]。
 */
@UnstableApi
internal object AndroidPlaybackMediaCommandHandler {
    /** 按 action 分类执行自定义命令，并把结果映射为 [SessionResult] code。 */
    fun handleCustomCommand(customAction: String): Int {
        val actions: PlaybackMediaButtonActions =
            PlaybackMediaCommandDispatcher.current()
                ?: return SessionResult.RESULT_ERROR_INVALID_STATE
        return when {
            PlaybackMediaCommandCatalog.isToggleFavoriteAction(customAction = customAction) -> {
                actions.toggleFavorite()
                SessionResult.RESULT_SUCCESS
            }

            PlaybackMediaCommandCatalog.isCycleModeAction(customAction = customAction) -> {
                actions.cycleMode()
                SessionResult.RESULT_SUCCESS
            }

            PlaybackMediaCommandCatalog.isUpdateButtonsCommand(customAction = customAction) -> {
                SessionResult.RESULT_ERROR_BAD_VALUE
            }

            else -> {
                SessionResult.RESULT_ERROR_NOT_SUPPORTED
            }
        }
    }
}
