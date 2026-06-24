package com.yanhao.kmpmusic.playback

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.yanhao.kmpmusic.domain.model.PlaybackMode

/**
 * 收藏切换自定义命令，供 Media3 系统媒体通知按钮回流到共享控制器。
 */
private const val CUSTOM_ACTION_TOGGLE_FAVORITE: String = "com.yanhao.kmpmusic.playback.TOGGLE_FAVORITE"

/**
 * 播放模式轮换自定义命令，供 Media3 系统媒体通知按钮回流到共享控制器。
 */
private const val CUSTOM_ACTION_CYCLE_MODE: String = "com.yanhao.kmpmusic.playback.CYCLE_MODE"

/**
 * 系统媒体通知可触发的自定义播放动作集合，标准播放命令继续复用 [PlaybackCommandBridge]。
 */
interface PlaybackMediaButtonActions : PlaybackCommandBridge {
    /** 切换当前播放歌曲的收藏状态。 */
    fun toggleFavorite()

    /** 切换到下一种播放模式。 */
    fun cycleMode()
}

/**
 * 进程内 Media3 自定义命令派发器，确保通知按钮和系统媒体命令共享 controller 命令路径。
 */
object PlaybackMediaCommandDispatcher {
    // 当前可消费系统媒体自定义按钮的 controller-backed actions。
    private var actions: PlaybackMediaButtonActions? = null

    /** 在 Android 播放会话就绪后挂入同一份命令实现。 */
    fun attach(actions: PlaybackMediaButtonActions) {
        this.actions = actions
    }

    /** 在宿主销毁或替换实现时清空旧引用。 */
    fun detach() {
        actions = null
    }

    /** 返回当前按钮动作实现；尚未接线时返回 null。 */
    fun current(): PlaybackMediaButtonActions? {
        return actions
    }
}

/**
 * 构建 Media3 官方媒体按钮偏好与处理自定义命令，避免保留应用自绘通知按钮。
 */
@UnstableApi
internal object AndroidPlaybackMediaButtons {
    // Media3 自定义收藏命令，供 [CommandButton] 和 [MediaSession.Callback] 共享同一个 action。
    private val toggleFavoriteCommand: SessionCommand = SessionCommand(
        CUSTOM_ACTION_TOGGLE_FAVORITE,
        Bundle.EMPTY,
    )

    // Media3 自定义播放模式命令，供 [CommandButton] 和 [MediaSession.Callback] 共享同一个 action。
    private val cycleModeCommand: SessionCommand = SessionCommand(
        CUSTOM_ACTION_CYCLE_MODE,
        Bundle.EMPTY,
    )

    /** 为媒体通知控制器暴露默认 session 命令和本应用的两个自定义按钮命令。 */
    fun availableSessionCommands(): SessionCommands {
        return SessionCommands.Builder()
            .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
            .add(toggleFavoriteCommand)
            .add(cycleModeCommand)
            .build()
    }

    /** 按系统 slot 语义声明媒体通知按钮偏好，最终位置由 System UI 决定。 */
    fun mediaButtonPreferences(
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
    ): List<CommandButton> {
        return listOf(
            createFavoriteButton(isFavorite = isFavorite),
            createPreviousButton(),
            createPlayPauseButton(isPlaying = isPlaying),
            createNextButton(),
            createPlaybackModeButton(playbackMode = playbackMode),
        )
    }

    /** 执行 Media3 自定义命令，并把结果返回给系统控制器。 */
    fun handleCustomCommand(customAction: String): Int {
        val actions: PlaybackMediaButtonActions = PlaybackMediaCommandDispatcher.current()
            ?: return SessionResult.RESULT_ERROR_INVALID_STATE
        return when (customAction) {
            CUSTOM_ACTION_TOGGLE_FAVORITE -> {
                actions.toggleFavorite()
                SessionResult.RESULT_SUCCESS
            }
            CUSTOM_ACTION_CYCLE_MODE -> {
                actions.cycleMode()
                SessionResult.RESULT_SUCCESS
            }
            else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
        }
    }

    /** 判断按钮是否为收藏命令，供通知 provider 跨 Android 版本保持稳定顺序。 */
    fun isToggleFavoriteButton(commandButton: CommandButton): Boolean {
        return commandButton.sessionCommand?.customAction == CUSTOM_ACTION_TOGGLE_FAVORITE
    }

    /** 判断按钮是否为播放模式命令，供通知 provider 跨 Android 版本保持稳定顺序。 */
    fun isPlaybackModeButton(commandButton: CommandButton): Boolean {
        return commandButton.sessionCommand?.customAction == CUSTOM_ACTION_CYCLE_MODE
    }

    /** 创建上一首按钮，让系统命令继续进入 [CoordinatorForwardingPlayer]。 */
    fun createPreviousButton(): CommandButton {
        return CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .setDisplayName("上一首")
            .setSlots(CommandButton.SLOT_BACK)
            .build()
    }

    /** 创建播放/暂停按钮，图标随 shared 播放状态刷新。 */
    fun createPlayPauseButton(isPlaying: Boolean): CommandButton {
        return CommandButton.Builder(
            if (isPlaying) {
                CommandButton.ICON_PAUSE
            } else {
                CommandButton.ICON_PLAY
            },
        )
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setDisplayName(if (isPlaying) "暂停" else "播放")
            .setSlots(CommandButton.SLOT_CENTRAL)
            .build()
    }

    /** 创建下一首按钮，让系统命令继续进入 [CoordinatorForwardingPlayer]。 */
    fun createNextButton(): CommandButton {
        return CommandButton.Builder(CommandButton.ICON_NEXT)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .setDisplayName("下一首")
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()
    }

    // 收藏按钮使用系统心形图标，优先占据上一首外侧的次级后退 slot。
    private fun createFavoriteButton(isFavorite: Boolean): CommandButton {
        return CommandButton.Builder(
            if (isFavorite) {
                CommandButton.ICON_HEART_FILLED
            } else {
                CommandButton.ICON_HEART_UNFILLED
            },
        )
            .setSessionCommand(toggleFavoriteCommand)
            .setDisplayName(if (isFavorite) "取消收藏" else "收藏")
            .setSlots(CommandButton.SLOT_BACK_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .build()
    }

    // 播放模式按钮使用自定义 SessionCommand，并优先占据下一首外侧的次级前进 slot。
    private fun createPlaybackModeButton(playbackMode: PlaybackMode): CommandButton {
        return CommandButton.Builder(playbackMode.resolveIcon())
            .setSessionCommand(cycleModeCommand)
            .setDisplayName(playbackMode.resolveDisplayName())
            .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .build()
    }

    // 播放模式映射到 Media3 内置图标，避免维护应用自绘通知资源。
    private fun PlaybackMode.resolveIcon(): Int {
        return when (this) {
            PlaybackMode.LoopAll -> CommandButton.ICON_REPEAT_ALL
            PlaybackMode.LoopOne -> CommandButton.ICON_REPEAT_ONE
            PlaybackMode.Shuffle -> CommandButton.ICON_SHUFFLE_ON
        }
    }

    // 系统按钮 displayName 作为无障碍和兼容控制器文本，不承担 UI 排版。
    private fun PlaybackMode.resolveDisplayName(): String {
        return when (this) {
            PlaybackMode.LoopAll -> "列表循环"
            PlaybackMode.LoopOne -> "单曲循环"
            PlaybackMode.Shuffle -> "随机播放"
        }
    }
}
