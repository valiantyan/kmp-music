package com.yanhao.kmpmusic.feature.app.session

import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.SongMoreSourceContext
import com.yanhao.kmpmusic.domain.model.LocalPlaylist

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
    fun openMore(
        state: MusicAppUiState,
        songId: String,
        sourceContext: SongMoreSourceContext = SongMoreSourceContext.General,
    ): MusicAppUiState {
        return state.copy(
            moreSongId = songId,
            moreSongSourceContext = sourceContext,
        )
    }

    /** 关闭歌曲更多操作弹层。 */
    fun closeMore(state: MusicAppUiState): MusicAppUiState {
        return state.copy(
            moreSongId = null,
            moreSongSourceContext = SongMoreSourceContext.General,
        )
    }

    /** 从更多面板进入添加到歌单流程，同时关闭来源更多面板。 */
    fun openAddToPlaylistFlow(
        state: MusicAppUiState,
        songId: String,
        playlists: List<LocalPlaylist>,
    ): MusicAppUiState {
        return state.copy(
            moreSongId = null,
            moreSongSourceContext = SongMoreSourceContext.General,
            addToPlaylistFlow = AddToPlaylistFlowState(
                songId = songId,
                availablePlaylists = playlists,
            ),
        )
    }

    /** 关闭添加到歌单流程产生的所有临时弹窗。 */
    fun closeAddToPlaylistFlow(state: MusicAppUiState): MusicAppUiState {
        return state.copy(addToPlaylistFlow = null)
    }

    /** 打开新建歌单弹窗时填入仓库给出的可用默认名。 */
    fun openCreatePlaylistDialog(
        state: MusicAppUiState,
        defaultName: String,
    ): MusicAppUiState {
        val flow: AddToPlaylistFlowState = state.addToPlaylistFlow ?: return state
        return state.copy(
            addToPlaylistFlow = flow.copy(
                isCreateDialogOpen = true,
                newPlaylistName = defaultName,
                newPlaylistNameError = null,
            ),
        )
    }

    /** 更新新建歌单名称，并清理上一轮校验错误。 */
    fun setNewPlaylistName(
        state: MusicAppUiState,
        name: String,
    ): MusicAppUiState {
        val flow: AddToPlaylistFlowState = state.addToPlaylistFlow ?: return state
        return state.copy(
            addToPlaylistFlow = flow.copy(
                newPlaylistName = name,
                newPlaylistNameError = null,
            ),
        )
    }

    /** 选择已有歌单时只保留一个当前可见目标，避免完成按钮保存到隐藏或不存在的歌单。 */
    fun selectAddToPlaylistTarget(
        state: MusicAppUiState,
        playlistId: String,
    ): MusicAppUiState {
        val flow: AddToPlaylistFlowState = state.addToPlaylistFlow ?: return state
        val selectedPlaylistId: String? = playlistId.takeIf {
            flow.availablePlaylists.any { playlist: LocalPlaylist -> playlist.id == playlistId }
        }
        return state.copy(
            addToPlaylistFlow = flow.copy(selectedPlaylistId = selectedPlaylistId),
        )
    }

    /** 新建歌单校验失败时保持弹窗打开，方便用户直接修正输入。 */
    fun showCreatePlaylistError(
        state: MusicAppUiState,
        message: String,
    ): MusicAppUiState {
        val flow: AddToPlaylistFlowState = state.addToPlaylistFlow ?: return state
        return state.copy(
            addToPlaylistFlow = flow.copy(
                isCreateDialogOpen = true,
                newPlaylistNameError = message,
            ),
        )
    }

    /** 新建并加入成功后关闭整条流程，并留下全局轻提示文案。 */
    fun finishAddToPlaylistFlow(
        state: MusicAppUiState,
        playlistName: String,
    ): MusicAppUiState {
        return state.copy(
            addToPlaylistFlow = null,
            transientMessage = "添加到 $playlistName 歌单成功",
        )
    }

    /** 清除一次性轻提示，避免返回当前页面时重复展示旧结果。 */
    fun clearTransientMessage(state: MusicAppUiState): MusicAppUiState {
        return state.copy(transientMessage = null)
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
