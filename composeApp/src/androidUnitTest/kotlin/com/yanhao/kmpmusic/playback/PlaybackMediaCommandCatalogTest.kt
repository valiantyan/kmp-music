package com.yanhao.kmpmusic.playback

import androidx.media3.common.util.UnstableApi
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class PlaybackMediaCommandCatalogTest {
    @Test
    fun recognizesUpdateButtonsCommand() {
        val command = PlaybackMediaCommandCatalog.updateButtonsCommand()

        assertTrue(
            actual =
                PlaybackMediaCommandCatalog.isUpdateButtonsCommand(
                    customAction = command.customAction,
                ),
        )
        assertFalse(
            actual =
                PlaybackMediaCommandCatalog.isUpdateButtonsCommand(
                    customAction = "com.yanhao.kmpmusic.playback.UNKNOWN",
                ),
        )
    }

    @Test
    fun exposesFavoriteAndPlaybackModeCommands() {
        assertTrue(
            actual =
                PlaybackMediaCommandCatalog.isToggleFavoriteAction(
                    customAction = PlaybackMediaCommandCatalog.toggleFavoriteCommand().customAction,
                ),
        )
        assertTrue(
            actual =
                PlaybackMediaCommandCatalog.isCycleModeAction(
                    customAction = PlaybackMediaCommandCatalog.cycleModeCommand().customAction,
                ),
        )
    }
}
