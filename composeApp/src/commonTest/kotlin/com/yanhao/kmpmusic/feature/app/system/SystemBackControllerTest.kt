package com.yanhao.kmpmusic.feature.app.system

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.navigation.NavigationStateController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemBackControllerTest {
    /**
     * 系统返回优先关闭权限弹窗，且不继续消费到二级页面。
     */
    @Test
    fun systemBackClosesPermissionDialogFirst(): Unit {
        val state: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState(),
            screen = SecondaryScreen.Settings,
        ).copy(
            isPermissionSettingsDialogOpen = true,
            isClearCacheDialogOpen = true,
            isQueueOpen = true,
            moreSongId = "song-1",
        )
        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = state)
        assertTrue(actual = result.wasHandled)
        assertFalse(actual = result.state.isPermissionSettingsDialogOpen)
        assertTrue(actual = result.state.isClearCacheDialogOpen)
        assertTrue(actual = result.state.isQueueOpen)
        assertTrue(actual = result.state.navigationState.secondaryScreen is SecondaryScreen.Settings)
    }

    /**
     * 没有弹窗和面板时，系统返回才回退二级页面。
     */
    @Test
    fun systemBackReturnsFromSecondaryWhenNoOverlayExists(): Unit {
        val state: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState(),
            screen = SecondaryScreen.Settings,
        )
        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = state)
        assertTrue(actual = result.wasHandled)
        assertNull(actual = result.state.navigationState.secondaryScreen)
    }

    /**
     * 清缓存弹窗、单曲更多和队列都存在时，返回键每次只关闭当前最高优先级对象。
     */
    @Test
    fun systemBackClosesCacheDialogMorePanelAndQueueInOrder(): Unit {
        val state: MusicAppUiState = NavigationStateController.navigateToSecondary(
            state = baseState(),
            screen = SecondaryScreen.Settings,
        ).copy(
            isClearCacheDialogOpen = true,
            moreSongId = "song-1",
            isQueueOpen = true,
        )
        val afterCacheDialog: SystemBackController.Result = SystemBackController.handleSystemBack(state = state)
        val afterMorePanel: SystemBackController.Result = SystemBackController.handleSystemBack(state = afterCacheDialog.state)
        val afterQueue: SystemBackController.Result = SystemBackController.handleSystemBack(state = afterMorePanel.state)
        assertTrue(actual = afterCacheDialog.wasHandled)
        assertFalse(actual = afterCacheDialog.state.isClearCacheDialogOpen)
        assertEquals(expected = "song-1", actual = afterCacheDialog.state.moreSongId)
        assertTrue(actual = afterMorePanel.wasHandled)
        assertNull(actual = afterMorePanel.state.moreSongId)
        assertTrue(actual = afterMorePanel.state.isQueueOpen)
        assertTrue(actual = afterQueue.wasHandled)
        assertFalse(actual = afterQueue.state.isQueueOpen)
        assertTrue(actual = afterQueue.state.navigationState.secondaryScreen is SecondaryScreen.Settings)
    }

    /**
     * 顶层页面没有可关闭对象时，系统返回不消费事件。
     */
    @Test
    fun systemBackDoesNotHandleTopLevelIdleState(): Unit {
        val result: SystemBackController.Result = SystemBackController.handleSystemBack(state = baseState())
        assertFalse(actual = result.wasHandled)
    }
}

/**
 * 构造系统返回测试所需的最小状态，避免引入与返回优先级无关的噪音。
 */
private fun baseState(): MusicAppUiState {
    return MusicAppUiState(
        likedSongIds = emptySet(),
        currentSongId = null,
        playbackStatus = PlaybackStatus.Idle,
        queueSongIds = emptyList(),
    )
}
