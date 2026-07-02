package com.yanhao.kmpmusic.playback

import kotlinx.coroutines.channels.Channel

/**
 * 串行消费桌面播放命令，并把退出收口逻辑从引擎主体剥离出去。
 */
internal class DesktopPlaybackCommandLoop(
    // 引擎唯一命令通道，所有状态变更都从这里进入。
    private val commands: Channel<DesktopPlaybackCommand>,
    // 单条命令处理器，由引擎提供具体实现。
    private val handleCommand: suspend (DesktopPlaybackCommand) -> Unit,
    // 命令循环退出时统一执行的收尾逻辑。
    private val onFinally: () -> Unit,
) {
    /** 运行命令循环，并确保异常或关闭时都能执行统一收尾。 */
    suspend fun run() {
        try {
            for (command: DesktopPlaybackCommand in commands) {
                handleCommand(command)
            }
        } finally {
            onFinally()
        }
    }
}
