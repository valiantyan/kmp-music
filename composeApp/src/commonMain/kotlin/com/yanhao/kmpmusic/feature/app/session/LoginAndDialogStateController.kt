package com.yanhao.kmpmusic.feature.app.session

import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 统一承接轻量会话弹层与登录输入态 reducer，避免 [MusicAppController] 混入简单 UI 状态细节。
 */
object LoginAndDialogStateController {
    /** 打开播放队列弹层。 */
    fun openQueue(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isQueueOpen = true)
    }

    /** 关闭播放队列弹层。 */
    fun closeQueue(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isQueueOpen = false)
    }

    /** 打开歌曲更多操作弹层，并记录目标歌曲 id。 */
    fun openMore(state: MusicAppUiState, songId: String): MusicAppUiState {
        return state.copy(moreSongId = songId)
    }

    /** 关闭歌曲更多操作弹层。 */
    fun closeMore(state: MusicAppUiState): MusicAppUiState {
        return state.copy(moreSongId = null)
    }

    /** 打开清理缓存确认框。 */
    fun openClearCacheDialog(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isClearCacheDialogOpen = true)
    }

    /** 关闭清理缓存确认框。 */
    fun closeClearCacheDialog(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isClearCacheDialogOpen = false)
    }

    /** 确认清理缓存时仅关闭确认框，具体缓存策略仍由上层决定。 */
    fun confirmClearCache(state: MusicAppUiState): MusicAppUiState {
        return state.copy(isClearCacheDialogOpen = false)
    }

    /** 更新登录邮箱输入。 */
    fun setEmail(state: MusicAppUiState, email: String): MusicAppUiState {
        return state.copy(email = email)
    }

    /** 只有邮箱格式具备最小可用性时，才允许进入已发送状态。 */
    fun sendLoginMail(state: MusicAppUiState): MusicAppUiState {
        if (!state.email.contains(other = "@")) {
            return state
        }
        return state.copy(isMailSent = true)
    }
}
