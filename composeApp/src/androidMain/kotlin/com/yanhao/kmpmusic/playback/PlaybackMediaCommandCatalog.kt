package com.yanhao.kmpmusic.playback

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands

private const val CUSTOM_ACTION_TOGGLE_FAVORITE: String = "com.yanhao.kmpmusic.playback.TOGGLE_FAVORITE"
private const val CUSTOM_ACTION_CYCLE_MODE: String = "com.yanhao.kmpmusic.playback.CYCLE_MODE"
private const val CUSTOM_ACTION_UPDATE_BUTTONS: String = "com.yanhao.kmpmusic.playback.UPDATE_BUTTONS"

/**
 * Android Media3 custom command 定义的唯一来源。
 */
@UnstableApi
internal object PlaybackMediaCommandCatalog {
    // 收藏按钮命令，供 [CommandButton] 和 [MediaSession.Callback] 共享同一个 action。
    private val toggleFavoriteSessionCommand: SessionCommand =
        SessionCommand(
            CUSTOM_ACTION_TOGGLE_FAVORITE,
            Bundle.EMPTY,
        )

    // 播放模式按钮命令，供 [CommandButton] 和 [MediaSession.Callback] 共享同一个 action。
    private val cycleModeSessionCommand: SessionCommand =
        SessionCommand(
            CUSTOM_ACTION_CYCLE_MODE,
            Bundle.EMPTY,
        )

    // App 内 controller 用于刷新按钮偏好的 session command。
    private val updateButtonsSessionCommand: SessionCommand =
        SessionCommand(
            CUSTOM_ACTION_UPDATE_BUTTONS,
            Bundle.EMPTY,
        )

    /** 为媒体通知控制器暴露默认 session 命令和本应用的自定义按钮命令。 */
    fun availableSessionCommands(): SessionCommands =
        SessionCommands
            .Builder()
            .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
            .add(toggleFavoriteSessionCommand)
            .add(cycleModeSessionCommand)
            .add(updateButtonsSessionCommand)
            .build()

    /** 返回收藏按钮命令，供按钮工厂和测试共享。 */
    fun toggleFavoriteCommand(): SessionCommand = toggleFavoriteSessionCommand

    /** 返回播放模式按钮命令，供按钮工厂和测试共享。 */
    fun cycleModeCommand(): SessionCommand = cycleModeSessionCommand

    /** 返回按钮刷新命令，供 [androidx.media3.session.MediaController] 客户端发送 custom command。 */
    fun updateButtonsCommand(): SessionCommand = updateButtonsSessionCommand

    /** 判断是否为 App 内 controller 发来的按钮刷新命令。 */
    fun isUpdateButtonsCommand(customAction: String): Boolean = customAction == CUSTOM_ACTION_UPDATE_BUTTONS

    /** 判断 action 是否为收藏命令。 */
    fun isToggleFavoriteAction(customAction: String): Boolean = customAction == CUSTOM_ACTION_TOGGLE_FAVORITE

    /** 判断 action 是否为播放模式轮换命令。 */
    fun isCycleModeAction(customAction: String): Boolean = customAction == CUSTOM_ACTION_CYCLE_MODE

    /** 判断按钮是否为收藏命令，供通知 provider 跨 Android 版本保持稳定顺序。 */
    fun isToggleFavoriteButton(commandButton: CommandButton): Boolean = commandButton.sessionCommand?.customAction == CUSTOM_ACTION_TOGGLE_FAVORITE

    /** 判断按钮是否为播放模式命令，供通知 provider 跨 Android 版本保持稳定顺序。 */
    fun isPlaybackModeButton(commandButton: CommandButton): Boolean = commandButton.sessionCommand?.customAction == CUSTOM_ACTION_CYCLE_MODE
}
