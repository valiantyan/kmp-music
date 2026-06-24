package com.yanhao.kmpmusic.playback

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

/**
 * Android 系统媒体通知 provider，补齐 API 33 以下 [NotificationCompat.MediaStyle] 的按钮顺序。
 *
 * Media3 默认 provider 会先放上一首、播放、下一首，再追加自定义命令；这里保留官方
 * [DefaultMediaNotificationProvider]，只通过受支持的 override 调整 action 数组顺序。
 */
@UnstableApi
internal class AndroidPlaybackMediaNotificationProvider(
    context: Context,
) : DefaultMediaNotificationProvider(context) {
    // 创建标准媒体 action 图标时使用 application context，避免持有短生命周期对象。
    private val appContext: Context = context.applicationContext

    /**
     * 按 QQ 音乐同类系统卡片顺序组织展开态 action：收藏、上一首、播放/暂停、下一首、播放模式。
     */
    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val orderedButtons: ImmutableList.Builder<CommandButton> = ImmutableList.builder()
        mediaButtonPreferences.firstOrNull(AndroidPlaybackMediaButtons::isToggleFavoriteButton)
            ?.let { favoriteButton: CommandButton -> orderedButtons.add(favoriteButton) }
        if (playerCommands.hasPreviousCommand()) {
            orderedButtons.add(AndroidPlaybackMediaButtons.createPreviousButton())
        }
        if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
            orderedButtons.add(
                AndroidPlaybackMediaButtons.createPlayPauseButton(isPlaying = showPauseButton),
            )
        }
        if (playerCommands.hasNextCommand()) {
            orderedButtons.add(AndroidPlaybackMediaButtons.createNextButton())
        }
        mediaButtonPreferences.firstOrNull(AndroidPlaybackMediaButtons::isPlaybackModeButton)
            ?.let { playbackModeButton: CommandButton -> orderedButtons.add(playbackModeButton) }
        return orderedButtons.build()
    }

    /**
     * 把 [getMediaButtons] 的顺序原样写入通知，并声明紧凑态优先展示收藏、播放/暂停、下一首。
     */
    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory,
    ): IntArray {
        mediaButtons.forEach { commandButton: CommandButton ->
            builder.addAction(
                createNotificationAction(
                    mediaSession = mediaSession,
                    commandButton = commandButton,
                    actionFactory = actionFactory,
                ),
            )
        }
        return mediaButtons.resolveCompactViewIndices()
    }

    // 根据按钮类型创建通知 action，自定义命令继续由 Media3 转发到 [MediaSession.Callback]。
    private fun createNotificationAction(
        mediaSession: MediaSession,
        commandButton: CommandButton,
        actionFactory: MediaNotification.ActionFactory,
    ): NotificationCompat.Action {
        commandButton.sessionCommand?.let {
            return actionFactory.createCustomActionFromCustomCommandButton(
                mediaSession,
                commandButton,
            )
        }
        return actionFactory.createMediaAction(
            mediaSession,
            IconCompat.createWithResource(appContext, commandButton.iconResId),
            commandButton.displayName,
            commandButton.playerCommand,
        )
    }

    // 紧凑态最多 3 个位置；收藏缺失时回退到上一首，避免兼容控制器出现空位。
    private fun ImmutableList<CommandButton>.resolveCompactViewIndices(): IntArray {
        val firstIndex: Int = indexOfFirst(AndroidPlaybackMediaButtons::isToggleFavoriteButton)
            .takeIf { index: Int -> index >= 0 }
            ?: indexOfFirst { commandButton: CommandButton -> commandButton.hasPreviousCommand() }
        val playPauseIndex: Int = indexOfFirst { commandButton: CommandButton ->
            commandButton.playerCommand == Player.COMMAND_PLAY_PAUSE
        }
        val nextIndex: Int = indexOfFirst { commandButton: CommandButton ->
            commandButton.hasNextCommand()
        }
        return listOf(firstIndex, playPauseIndex, nextIndex)
            .filter { index: Int -> index >= 0 }
            .toIntArray()
    }

    // 判断控制器是否允许上一首相关命令，兼容 Media3 对上一首命令的新旧拆分。
    private fun Player.Commands.hasPreviousCommand(): Boolean {
        return contains(Player.COMMAND_SEEK_TO_PREVIOUS) ||
            contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
    }

    // 判断控制器是否允许下一首相关命令，兼容 Media3 对下一首命令的新旧拆分。
    private fun Player.Commands.hasNextCommand(): Boolean {
        return contains(Player.COMMAND_SEEK_TO_NEXT) ||
            contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
    }

    // 判断按钮是否承载上一首命令，用于紧凑态 fallback。
    private fun CommandButton.hasPreviousCommand(): Boolean {
        return playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
            playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
    }

    // 判断按钮是否承载下一首命令，用于紧凑态第三位。
    private fun CommandButton.hasNextCommand(): Boolean {
        return playerCommand == Player.COMMAND_SEEK_TO_NEXT ||
            playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
    }
}
