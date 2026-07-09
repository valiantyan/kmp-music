package com.yanhao.kmpmusic.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import com.google.common.collect.ImmutableList
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class AndroidPlaybackMediaNotificationProviderTest {
    @Test
    fun mediaButtonsKeepFiveSlotsWhenPlayerNavigationCommandsAreUnavailable(): Unit {
        val mediaButtonPreferences: ImmutableList<CommandButton> = ImmutableList.copyOf<CommandButton>(
            AndroidPlaybackMediaButtonFactory.mediaButtonPreferences(
                shouldShowPauseButton = true,
                isFavorite = true,
                playbackMode = PlaybackMode.Shuffle,
            ),
        )
        val playerCommands: Player.Commands = Player.Commands.Builder()
            .add(Player.COMMAND_PLAY_PAUSE)
            .build()
        val mediaButtons: ImmutableList<CommandButton> = AndroidPlaybackMediaButtonOrdering.orderMediaButtons(
            playerCommands = playerCommands,
            mediaButtonPreferences = mediaButtonPreferences,
            showPauseButton = true,
        )
        assertEquals(expected = 5, actual = mediaButtons.size)
        assertTrue(actual = PlaybackMediaCommandCatalog.isToggleFavoriteButton(mediaButtons[0]))
        assertEquals(expected = Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, actual = mediaButtons[1].playerCommand)
        assertEquals(expected = Player.COMMAND_PLAY_PAUSE, actual = mediaButtons[2].playerCommand)
        assertEquals(expected = Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, actual = mediaButtons[3].playerCommand)
        assertTrue(actual = PlaybackMediaCommandCatalog.isPlaybackModeButton(mediaButtons[4]))
    }
}
