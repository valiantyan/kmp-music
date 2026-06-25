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
 * 媒体按钮偏好刷新命令，供 App 内 [MediaController] 客户端同步 shared 状态到 session。
 */
private const val CUSTOM_ACTION_UPDATE_BUTTONS: String = "com.yanhao.kmpmusic.playback.UPDATE_BUTTONS"

/**
 * 按钮刷新命令参数：当前是否正在播放。
 */
private const val ARG_IS_PLAYING: String = "is_playing"

/**
 * 按钮刷新命令参数：当前歌曲是否已收藏。
 */
private const val ARG_IS_FAVORITE: String = "is_favorite"

/**
 * 按钮刷新命令参数：当前播放模式名称。
 */
private const val ARG_PLAYBACK_MODE: String = "playback_mode"

/**
 * 按钮刷新命令参数：当前播放状态名称。
 */
private const val ARG_PLAYBACK_STATUS: String = "playback_status"

/**
 * 按钮刷新命令参数：shared 层是否仍持有当前播放上下文。
 */
private const val ARG_HAS_ACTIVE_PLAYBACK_SESSION: String = "has_active_playback_session"

/**
 * 系统媒体通知可触发的自定义播放动作集合，标准播放命令由 [MediaSession] 委托给 [Player]。
 */
interface PlaybackMediaButtonActions {
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

    // Media3 自定义按钮刷新命令，供 App 内 controller 通过 session 更新系统媒体卡片。
    private val updateButtonsSessionCommand: SessionCommand = SessionCommand(
        CUSTOM_ACTION_UPDATE_BUTTONS,
        Bundle.EMPTY,
    )

    /** 为媒体通知控制器暴露默认 session 命令和本应用的两个自定义按钮命令。 */
    fun availableSessionCommands(): SessionCommands {
        return SessionCommands.Builder()
            .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
            .add(toggleFavoriteCommand)
            .add(cycleModeCommand)
            .add(updateButtonsSessionCommand)
            .build()
    }

    /** 返回按钮刷新命令，供 [MediaController] 客户端发送官方 custom command。 */
    fun updateButtonsCommand(): SessionCommand {
        return updateButtonsSessionCommand
    }

    /** 把 shared 按钮状态编码到 [Bundle]，避免平台层直接引用 UI state。 */
    fun createUpdateButtonsArgs(state: MediaButtonState): Bundle {
        return Bundle().apply {
            putBoolean(ARG_IS_PLAYING, state.isPlaying)
            putBoolean(ARG_IS_FAVORITE, state.isFavorite)
            putString(ARG_PLAYBACK_MODE, state.playbackMode.name)
            putString(ARG_PLAYBACK_STATUS, state.playbackStatus.name)
            putBoolean(ARG_HAS_ACTIVE_PLAYBACK_SESSION, state.hasActivePlaybackSession)
        }
    }

    /** 从 custom command 参数中恢复按钮状态，解析失败时拒绝更新 session。 */
    fun resolveUpdateButtonsState(args: Bundle): MediaButtonState? {
        val playbackMode: PlaybackMode = args.getString(ARG_PLAYBACK_MODE)
            ?.let { value: String -> runCatching { PlaybackMode.valueOf(value) }.getOrNull() }
            ?: return null
        val playbackStatus: com.yanhao.kmpmusic.domain.model.PlaybackStatus = args.getString(ARG_PLAYBACK_STATUS)
            ?.let { value: String ->
                runCatching {
                    com.yanhao.kmpmusic.domain.model.PlaybackStatus.valueOf(value)
                }.getOrNull()
            }
            ?: return null
        return MediaButtonState(
            isPlaying = args.getBoolean(ARG_IS_PLAYING),
            isFavorite = args.getBoolean(ARG_IS_FAVORITE),
            playbackMode = playbackMode,
            playbackStatus = playbackStatus,
            hasActivePlaybackSession = args.getBoolean(ARG_HAS_ACTIVE_PLAYBACK_SESSION),
        )
    }

    /** 判断是否为 App 内 controller 发来的按钮刷新命令。 */
    fun isUpdateButtonsCommand(customAction: String): Boolean {
        return customAction == CUSTOM_ACTION_UPDATE_BUTTONS
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
            CUSTOM_ACTION_UPDATE_BUTTONS -> SessionResult.RESULT_ERROR_BAD_VALUE
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

    /** 创建上一首按钮，让系统命令按 Media3 官方路径进入 session player。 */
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

    /** 创建下一首按钮，让系统命令按 Media3 官方路径进入 session player。 */
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
