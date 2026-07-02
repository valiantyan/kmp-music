package com.yanhao.kmpmusic.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 把桌面端进度采样节拍隔离成独立协作者，避免引擎同时承担轮询生命周期细节。
 */
internal class DesktopProgressTicker(
    // 轮询任务挂靠的作用域，跟随引擎生命周期统一回收。
    private val scope: CoroutineScope,
    // 相邻两次 tick 之间的等待时间；非正数表示禁用轮询。
    private val intervalMs: Long,
    // 每次 tick 时回调给命令通道，由引擎自己决定如何处理。
    private val sendTick: suspend () -> Unit,
) {
    // 当前激活的轮询任务，便于重复启动时先取消旧任务。
    private var job: Job? = null

    /** 启动新的轮询任务，并保证同一时刻只存在一个活跃 ticker。 */
    fun start() {
        stop()
        if (intervalMs <= 0L) {
            return
        }
        job = scope.launch {
            while (isActive) {
                delay(timeMillis = intervalMs)
                sendTick()
            }
        }
    }

    /** 停止轮询，防止切歌、暂停或释放后继续发送旧 tick。 */
    fun stop() {
        job?.cancel()
        job = null
    }
}
