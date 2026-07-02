package com.yanhao.kmpmusic.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import com.yanhao.kmpmusic.domain.model.PlaybackMode

/**
 * 构建 Media3 官方媒体按钮偏好，避免保留应用自绘通知按钮。
 */
@UnstableApi
internal object AndroidPlaybackMediaButtonFactory {
    /** 按系统 slot 语义声明媒体通知按钮偏好，最终位置由 System UI 决定。 */
    fun mediaButtonPreferences(
        shouldShowPauseButton: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
    ): List<CommandButton> {
        return listOf(
            createFavoriteButton(isFavorite = isFavorite),
            createPreviousButton(),
            createPlayPauseButton(shouldShowPauseButton = shouldShowPauseButton),
            createNextButton(),
            createPlaybackModeButton(playbackMode = playbackMode),
        )
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
    fun createPlayPauseButton(shouldShowPauseButton: Boolean): CommandButton {
        return CommandButton.Builder(
            if (shouldShowPauseButton) {
                CommandButton.ICON_PAUSE
            } else {
                CommandButton.ICON_PLAY
            },
        )
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setDisplayName(if (shouldShowPauseButton) "暂停" else "播放")
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
            .setSessionCommand(PlaybackMediaCommandCatalog.toggleFavoriteCommand())
            .setDisplayName(if (isFavorite) "取消收藏" else "收藏")
            .setSlots(CommandButton.SLOT_BACK_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .build()
    }

    // 播放模式按钮使用自定义 [SessionCommand]，并优先占据下一首外侧的次级前进 slot。
    private fun createPlaybackModeButton(playbackMode: PlaybackMode): CommandButton {
        return CommandButton.Builder(playbackMode.resolveIcon())
            .setSessionCommand(PlaybackMediaCommandCatalog.cycleModeCommand())
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

    // 播放模式显示名直接沿用 shared 枚举语义，便于系统面板无障碍朗读。
    private fun PlaybackMode.resolveDisplayName(): String {
        return when (this) {
            PlaybackMode.LoopAll -> "列表循环"
            PlaybackMode.LoopOne -> "单曲循环"
            PlaybackMode.Shuffle -> "随机播放"
        }
    }
}
