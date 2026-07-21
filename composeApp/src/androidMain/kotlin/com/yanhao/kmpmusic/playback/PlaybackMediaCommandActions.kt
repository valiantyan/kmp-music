package com.yanhao.kmpmusic.playback

/**
 * 系统媒体通知可触发的自定义播放动作集合，标准播放命令由 Media3 委托给 Player。
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
    fun current(): PlaybackMediaButtonActions? = actions

    /** 清空当前动作实现，供 Android unit tests 隔离单例状态。 */
    internal fun clear() {
        actions = null
    }
}
