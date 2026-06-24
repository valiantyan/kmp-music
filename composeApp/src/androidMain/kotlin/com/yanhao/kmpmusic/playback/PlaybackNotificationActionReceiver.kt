package com.yanhao.kmpmusic.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 收藏切换动作，供自定义播放通知发送到进程内命令派发器。
 */
const val ACTION_TOGGLE_FAVORITE: String = "com.yanhao.kmpmusic.playback.TOGGLE_FAVORITE"

/**
 * 上一首动作，供自定义播放通知发送到进程内命令派发器。
 */
const val ACTION_PREVIOUS: String = "com.yanhao.kmpmusic.playback.PREVIOUS"

/**
 * 播放或暂停动作，供自定义播放通知发送到进程内命令派发器。
 */
const val ACTION_TOGGLE_PLAYBACK: String = "com.yanhao.kmpmusic.playback.TOGGLE_PLAYBACK"

/**
 * 下一首动作，供自定义播放通知发送到进程内命令派发器。
 */
const val ACTION_NEXT: String = "com.yanhao.kmpmusic.playback.NEXT"

/**
 * 播放模式轮换动作，供自定义播放通知发送到进程内命令派发器。
 */
const val ACTION_CYCLE_MODE: String = "com.yanhao.kmpmusic.playback.CYCLE_MODE"

/**
 * 接收通知点击事件，并把命令统一回流到 controller-backed dispatcher。
 */
class PlaybackNotificationActionReceiver : BroadcastReceiver() {
    /** 仅负责把动作转发给当前 dispatcher，避免通知层直接修改播放器或共享状态。 */
    override fun onReceive(context: Context, intent: Intent) {
        val dispatcher: PlaybackNotificationActions? = PlaybackNotificationDispatcher.current()
        when (intent.action) {
            ACTION_TOGGLE_FAVORITE -> dispatcher?.toggleFavorite()
            ACTION_PREVIOUS -> dispatcher?.previous()
            ACTION_TOGGLE_PLAYBACK -> dispatcher?.togglePlayback()
            ACTION_NEXT -> dispatcher?.next()
            ACTION_CYCLE_MODE -> dispatcher?.cycleMode()
        }
    }
}

/**
 * 自定义通知可发送的动作集合，复用同一套播放命令桥。
 */
interface PlaybackNotificationActions : PlaybackCommandBridge {
    /** 切换当前播放歌曲的收藏状态。 */
    fun toggleFavorite()

    /** 切换到下一种播放模式。 */
    fun cycleMode()

    /** 切换播放与暂停，保持与 UI 按钮一致的交互语义。 */
    fun togglePlayback()
}

/**
 * 进程内通知命令派发器，确保通知和 MediaSession 共享同一条 controller 命令路径。
 */
object PlaybackNotificationDispatcher {
    // 当前可消费通知动作的 controller-backed commands。
    private var actions: PlaybackNotificationActions? = null

    /** 在 Android 播放会话就绪后挂入同一份命令实现。 */
    fun attach(actions: PlaybackNotificationActions) {
        this.actions = actions
    }

    /** 在宿主销毁或替换实现时清空旧引用。 */
    fun detach() {
        actions = null
    }

    /** 返回当前通知动作实现；尚未接线时返回 null。 */
    fun current(): PlaybackNotificationActions? {
        return actions
    }
}
