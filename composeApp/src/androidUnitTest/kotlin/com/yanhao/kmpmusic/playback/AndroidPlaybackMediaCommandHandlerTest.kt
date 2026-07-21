package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class AndroidPlaybackMediaCommandHandlerTest {
    @BeforeTest
    fun clearDispatcher() {
        PlaybackMediaCommandDispatcher.clear()
    }

    @Test
    fun customCommandWithoutAttachedActionsReturnsInvalidState() {
        val resultCode =
            AndroidPlaybackMediaCommandHandler.handleCustomCommand(
                customAction = PlaybackMediaCommandCatalog.toggleFavoriteCommand().customAction,
            )

        assertEquals(expected = SessionResult.RESULT_ERROR_INVALID_STATE, actual = resultCode)
    }

    @Test
    fun favoriteCommandOnlyCallsFavoriteAction() {
        val actions = RecordingPlaybackMediaButtonActions()
        PlaybackMediaCommandDispatcher.attach(actions = actions)

        val resultCode =
            AndroidPlaybackMediaCommandHandler.handleCustomCommand(
                customAction = PlaybackMediaCommandCatalog.toggleFavoriteCommand().customAction,
            )

        assertEquals(expected = SessionResult.RESULT_SUCCESS, actual = resultCode)
        assertEquals(expected = 1, actual = actions.toggleFavoriteCalls)
        assertEquals(expected = 0, actual = actions.cycleModeCalls)
    }

    @Test
    fun cycleModeCommandOnlyCallsModeAction() {
        val actions = RecordingPlaybackMediaButtonActions()
        PlaybackMediaCommandDispatcher.attach(actions = actions)

        val resultCode =
            AndroidPlaybackMediaCommandHandler.handleCustomCommand(
                customAction = PlaybackMediaCommandCatalog.cycleModeCommand().customAction,
            )

        assertEquals(expected = SessionResult.RESULT_SUCCESS, actual = resultCode)
        assertEquals(expected = 0, actual = actions.toggleFavoriteCalls)
        assertEquals(expected = 1, actual = actions.cycleModeCalls)
    }

    @Test
    fun updateButtonsCommandIsRejectedByHandler() {
        val actions = RecordingPlaybackMediaButtonActions()
        PlaybackMediaCommandDispatcher.attach(actions = actions)

        val resultCode =
            AndroidPlaybackMediaCommandHandler.handleCustomCommand(
                customAction = PlaybackMediaCommandCatalog.updateButtonsCommand().customAction,
            )

        assertEquals(expected = SessionResult.RESULT_ERROR_BAD_VALUE, actual = resultCode)
        assertEquals(expected = 0, actual = actions.toggleFavoriteCalls)
        assertEquals(expected = 0, actual = actions.cycleModeCalls)
    }
}

private class RecordingPlaybackMediaButtonActions : PlaybackMediaButtonActions {
    var toggleFavoriteCalls: Int = 0
        private set

    var cycleModeCalls: Int = 0
        private set

    override fun toggleFavorite() {
        toggleFavoriteCalls += 1
    }

    override fun cycleMode() {
        cycleModeCalls += 1
    }
}
