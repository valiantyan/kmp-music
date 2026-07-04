package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryStackEntry
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

/**
 * 纯导航 reducer，集中维护一级页与二级页切换时的状态收敛规则。
 */
object NavigationStateController {
    /**
     * 进入二级页面时保留来源一级页，并清理不能跨页面残留的临时浮层状态。
     */
    fun navigateToSecondary(state: MusicAppUiState, screen: SecondaryScreen): MusicAppUiState {
        val currentNavigationState: NavigationState = state.navigationState
        val nextEntryId: Int = currentNavigationState.secondaryEntryId + 1
        val nextBackStack: List<SecondaryStackEntry> = buildNextBackStack(
            navigationState = currentNavigationState,
        )
        return state.copy(
            navigationState = currentNavigationState.copy(
                secondaryScreen = screen,
                previousRootTab = currentNavigationState.rootTab,
                secondaryEntryId = nextEntryId,
                secondaryBackStack = nextBackStack,
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
        val previousEntry: SecondaryStackEntry? = state.navigationState.secondaryBackStack.lastOrNull()
        if (previousEntry != null) {
            return state.copy(
                navigationState = state.navigationState.copy(
                    secondaryScreen = previousEntry.screen,
                    secondaryEntryId = previousEntry.entryId,
                    secondaryBackStack = state.navigationState.secondaryBackStack.dropLast(n = 1),
                ),
            )
        }
        return state.copy(
            navigationState = state.navigationState.copy(
                rootTab = state.navigationState.previousRootTab,
                secondaryScreen = null,
                secondaryBackStack = emptyList(),
            ),
        )
    }

    /**
     * 只有当前已经处于二级页时才压栈；一级页本身由 [NavigationState.previousRootTab] 表达。
     */
    private fun buildNextBackStack(navigationState: NavigationState): List<SecondaryStackEntry> {
        val currentScreen: SecondaryScreen = navigationState.secondaryScreen ?: return navigationState.secondaryBackStack
        return navigationState.secondaryBackStack + SecondaryStackEntry(
            screen = currentScreen,
            entryId = navigationState.secondaryEntryId,
        )
    }
}
