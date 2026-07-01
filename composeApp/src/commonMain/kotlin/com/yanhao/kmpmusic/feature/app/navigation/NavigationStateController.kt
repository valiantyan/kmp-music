package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

/**
 * 纯导航 reducer，集中维护一级页与二级页切换时的状态收敛规则。
 */
object NavigationStateController {
    /**
     * 进入二级页面时保留来源一级页，并清理不能跨页面残留的临时浮层状态。
     */
    fun navigateToSecondary(state: MusicAppUiState, screen: SecondaryScreen): MusicAppUiState {
        return state.copy(
            navigationState = state.navigationState.copy(
                secondaryScreen = screen,
                previousRootTab = state.navigationState.rootTab,
                secondaryEntryId = state.navigationState.secondaryEntryId + 1,
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    /**
     * 切换一级 Tab 时重置二级路由，并把目标 Tab 设为新的返回基线。
     */
    fun navigateToRoot(state: MusicAppUiState, tab: RootTab): MusicAppUiState {
        return state.copy(
            navigationState = NavigationState(
                rootTab = tab,
                previousRootTab = tab,
            ),
            isQueueOpen = false,
            moreSongId = null,
        )
    }

    /**
     * 从二级页返回时恢复上次一级页，并保留 entry id 让滚动 key 继续稳定。
     */
    fun navigateBack(state: MusicAppUiState): MusicAppUiState {
        return state.copy(
            navigationState = state.navigationState.copy(
                rootTab = state.navigationState.previousRootTab,
                secondaryScreen = null,
            ),
        )
    }
}
