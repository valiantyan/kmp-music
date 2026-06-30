package com.yanhao.kmpmusic.feature.app.session

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证轻量 session/dialog reducer 只修改自身负责的 UI 状态。
 */
class LoginAndDialogStateControllerTest {
    @Test
    fun clearCacheDialogOpensAndClosesWithoutChangingUserData(): Unit {
        val controller = LoginAndDialogStateController
        val state: MusicAppUiState = testState().copy(likedSongIds = setOf("liked"))
        val openState: MusicAppUiState = controller.openClearCacheDialog(state = state)
        val closedState: MusicAppUiState = controller.confirmClearCache(state = openState)
        assertTrue(actual = openState.isClearCacheDialogOpen)
        assertFalse(actual = closedState.isClearCacheDialogOpen)
        assertEquals(expected = setOf("liked"), actual = closedState.likedSongIds)
    }

    @Test
    fun sendLoginMailRequiresAtSymbol(): Unit {
        val controller = LoginAndDialogStateController
        val invalidState: MusicAppUiState = controller.sendLoginMail(
            state = testState().copy(email = "not-mail"),
        )
        val validState: MusicAppUiState = controller.sendLoginMail(
            state = testState().copy(email = "user@example.com"),
        )
        assertFalse(actual = invalidState.isMailSent)
        assertTrue(actual = validState.isMailSent)
    }

    @Test
    fun moreMenuCanOpenAndCloseBySongId(): Unit {
        val controller = LoginAndDialogStateController
        val openState: MusicAppUiState = controller.openMore(
            state = testState(),
            songId = "song-1",
        )
        val closedState: MusicAppUiState = controller.closeMore(state = openState)
        assertEquals(expected = "song-1", actual = openState.moreSongId)
        assertNull(actual = closedState.moreSongId)
    }

    /** 构造最小可用 [MusicAppUiState]，避免测试依赖无关初始化细节。 */
    private fun testState(): MusicAppUiState {
        return MusicAppUiState(
            likedSongIds = emptySet(),
            currentSongId = null,
            playbackStatus = PlaybackStatus.Idle,
            queueSongIds = emptyList(),
        )
    }
}
