package com.yanhao.kmpmusic.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import com.google.common.collect.ImmutableList

/**
 * Android 媒体通知展开态按钮排序规则，集中隔离 System UI 展示顺序。
 */
@UnstableApi
internal object AndroidPlaybackMediaButtonOrdering {
    /**
     * 按产品固定顺序组织展开态 action：收藏、上一首、播放/暂停、下一首、播放模式。
     */
    @Suppress("UNUSED_PARAMETER")
    fun orderMediaButtons(
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val orderedButtons: ImmutableList.Builder<CommandButton> = ImmutableList.builder()
        mediaButtonPreferences.firstOrNull(PlaybackMediaCommandCatalog::isToggleFavoriteButton)
            ?.let { favoriteButton: CommandButton -> orderedButtons.add(favoriteButton) }
        val previousButton: CommandButton = mediaButtonPreferences.firstOrNull { commandButton: CommandButton ->
            commandButton.hasPreviousCommand()
        } ?: AndroidPlaybackMediaButtonFactory.createPreviousButton()
        orderedButtons.add(previousButton)
        orderedButtons.add(
            AndroidPlaybackMediaButtonFactory.createPlayPauseButton(shouldShowPauseButton = showPauseButton),
        )
        val nextButton: CommandButton = mediaButtonPreferences.firstOrNull { commandButton: CommandButton ->
            commandButton.hasNextCommand()
        } ?: AndroidPlaybackMediaButtonFactory.createNextButton()
        orderedButtons.add(nextButton)
        mediaButtonPreferences.firstOrNull(PlaybackMediaCommandCatalog::isPlaybackModeButton)
            ?.let { playbackModeButton: CommandButton -> orderedButtons.add(playbackModeButton) }
        return orderedButtons.build()
    }

    // 判断按钮是否承载上一首命令，用于固定展开态按钮集合。
    private fun CommandButton.hasPreviousCommand(): Boolean {
        return playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
            playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
    }

    // 判断按钮是否承载下一首命令，用于固定展开态按钮集合。
    private fun CommandButton.hasNextCommand(): Boolean {
        return playerCommand == Player.COMMAND_SEEK_TO_NEXT ||
            playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
    }
}
