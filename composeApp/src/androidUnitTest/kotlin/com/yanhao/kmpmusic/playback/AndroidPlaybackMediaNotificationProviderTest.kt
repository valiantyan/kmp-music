package com.yanhao.kmpmusic.playback

import android.content.Context
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
import org.robolectric.RuntimeEnvironment

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class AndroidPlaybackMediaNotificationProviderTest {
    @Test
    fun mediaButtonsKeepFiveActionsWhenQueueCommandsDisappear(): Unit {
        val context: Context = RuntimeEnvironment.getApplication()
        val provider: AndroidPlaybackMediaNotificationProvider =
            AndroidPlaybackMediaNotificationProvider(context = context)
        val buttons: ImmutableList<CommandButton> = provider.resolveOrderedMediaButtons(
            playerCommands = Player.Commands.Builder()
                .add(Player.COMMAND_PLAY_PAUSE)
                .build(),
            mediaButtonPreferences = ImmutableList.copyOf(
                AndroidPlaybackMediaButtonFactory.mediaButtonPreferences(
                    shouldShowPauseButton = true,
                    isFavorite = true,
                    playbackMode = PlaybackMode.Shuffle,
                ),
            ),
            showPauseButton = true,
        )
        assertEquals(expected = 5, actual = buttons.size)
        assertTrue(
            actual = PlaybackMediaCommandCatalog.isToggleFavoriteButton(commandButton = buttons[0]),
        )
        assertPreviousButton(commandButton = buttons[1])
        assertEquals(expected = Player.COMMAND_PLAY_PAUSE, actual = buttons[2].playerCommand)
        assertNextButton(commandButton = buttons[3])
        assertTrue(
            actual = PlaybackMediaCommandCatalog.isPlaybackModeButton(commandButton = buttons[4]),
        )
    }

    // 断言上一首按钮保留 Media3 标准命令，避免为固定展示改成不可执行的假按钮。
    private fun assertPreviousButton(commandButton: CommandButton): Unit {
        assertTrue(
            actual = commandButton.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                commandButton.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS,
        )
    }

    // 断言下一首按钮保留 Media3 标准命令，确保通知动作仍走官方 player command。
    private fun assertNextButton(commandButton: CommandButton): Unit {
        assertTrue(
            actual = commandButton.playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                commandButton.playerCommand == Player.COMMAND_SEEK_TO_NEXT,
        )
    }
}
