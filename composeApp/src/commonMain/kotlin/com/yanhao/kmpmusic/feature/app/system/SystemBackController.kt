package com.yanhao.kmpmusic.feature.app.system

import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController

/**
 * 系统返回键 reducer，集中维护弹窗、面板和二级页面的关闭优先级。
 */
object SystemBackController {
    /**
     * 系统返回处理结果。
     *
     * @property state 处理后的 UI 状态。
     * @property wasHandled 本次返回事件是否已被 App 消费。
     */
    data class Result(
        /** 处理后的 UI 状态。 */
        val state: MusicAppUiState,
        /** 本次返回事件是否已被 App 消费。 */
        val wasHandled: Boolean,
    )

    /**
     * 按权限弹窗、清缓存弹窗、轻提示、歌单流程、单曲更多面板、队列、二级页面的顺序处理返回。
     */
    fun handleSystemBack(state: MusicAppUiState): Result {
        if (state.isPermissionSettingsDialogOpen) {
            return Result(
                state = state.copy(isPermissionSettingsDialogOpen = false),
                wasHandled = true,
            )
        }
        if (state.isClearCacheDialogOpen) {
            return Result(
                state = state.copy(isClearCacheDialogOpen = false),
                wasHandled = true,
            )
        }
        if (state.transientMessage != null) {
            return Result(
                state = state.copy(transientMessage = null),
                wasHandled = true,
            )
        }
        if (state.addToPlaylistFlow != null) {
            return Result(
                state = state.copy(addToPlaylistFlow = null),
                wasHandled = true,
            )
        }
        if (state.moreSongId != null) {
            return Result(
                state = state.copy(moreSongId = null),
                wasHandled = true,
            )
        }
        if (state.isQueueOpen) {
            return Result(
                state = state.copy(isQueueOpen = false),
                wasHandled = true,
            )
        }
        if (!state.navigationState.isTopLevel) {
            return Result(
                state = NavigationStateController.navigateBack(state = state),
                wasHandled = true,
            )
        }
        return Result(
            state = state,
            wasHandled = false,
        )
    }
}
