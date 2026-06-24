package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.AndroidPlaybackSession
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob

/**
 * Android Media3 播放服务，承载真实播放器与系统 [MediaSession]。
 */
@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    // Service 生命周期内复用的协程作用域。
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 当前 service 暴露给系统控制器的 session。
    private var mediaSession: MediaSession? = null

    // 当前 service 持有的真实 ExoPlayer。
    private var player: ExoPlayer? = null

    // 供 app 内 shared 协调器桥接的 Android 真播放引擎。
    private var engine: Media3AudioPlayerEngine? = null

    /** 初始化 ExoPlayer、引擎和 MediaSession，并把实例登记到进程内注册表。 */
    override fun onCreate() {
        super.onCreate()
        AndroidPlaybackSession.bootstrap(context = applicationContext)
        val exoPlayer: ExoPlayer = ExoPlayer.Builder(this).build()
        val mediaEngine: Media3AudioPlayerEngine = Media3AudioPlayerEngine(
            player = exoPlayer,
            scope = serviceScope,
        )
        player = exoPlayer
        engine = mediaEngine
        mediaSession = MediaSession.Builder(
            /* context = */ this,
            /* player = */ CoordinatorForwardingPlayer(player = exoPlayer),
        ).build()
        PlaybackServiceRegistry.attach(
            service = this,
            engine = mediaEngine,
        )
    }

    /** 统一返回当前 session，允许系统控制器连接到此 service。 */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /** 释放 service 资源并清空注册表，避免 Activity 重建后拿到旧实例。 */
    override fun onDestroy() {
        PlaybackServiceRegistry.detach()
        mediaSession?.release()
        mediaSession = null
        engine = null
        player?.release()
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

/**
 * 进程内播放服务注册表，供 connector 在不引入 Android API 到 common 的前提下拿到 service 实例。
 */
object PlaybackServiceRegistry {
    // 当前活动的 playback service。
    private var service: MusicPlaybackService? = null

    // 当前 service 暴露出来的真播放引擎。
    private var engine: Media3AudioPlayerEngine? = null

    /** 在 service 初始化完成后登记当前实例。 */
    fun attach(service: MusicPlaybackService, engine: Media3AudioPlayerEngine) {
        this.service = service
        this.engine = engine
    }

    /** 在 service 销毁时清空引用，避免 connector 读取失效对象。 */
    fun detach() {
        service = null
        engine = null
    }

    /** 返回当前存活的播放服务，供后续 Android runtime 或通知层复用。 */
    fun currentService(): MusicPlaybackService? {
        return service
    }

    /** 返回当前 service 对应的真实播放引擎。 */
    fun currentEngine(): Media3AudioPlayerEngine? {
        return engine
    }
}

/**
 * 供 Android session 命令回流 shared 协调器的桥接接口。
 */
interface PlaybackCommandBridge {
    /** 显式开始或继续播放。 */
    fun play()

    /** 显式暂停播放。 */
    fun pause()

    /** 切到上一首。 */
    fun previous()

    /** 切到下一首。 */
    fun next()

    /** 跳转当前媒体进度。 */
    fun seekTo(positionMs: Long)

    /** 直接切到共享队列的精确下标，并带入系统命令指定的起始进度。 */
    fun skipToQueueIndex(index: Int, positionMs: Long = 0L)
}

/**
 * 进程内命令桥注册表，供后续 Task 9 把 [MusicAppController] 或 [com.yanhao.kmpmusic.domain.playback.PlaybackCoordinator] 接进来。
 */
object PlaybackCommandBridgeRegistry {
    // 当前可消费系统命令的 shared 命令桥。
    private var bridge: PlaybackCommandBridge? = null

    /** 注册当前可用的命令桥。 */
    fun attach(bridge: PlaybackCommandBridge) {
        this.bridge = bridge
    }

    /** 清空旧命令桥，避免 service 向已销毁控制器发命令。 */
    fun detach() {
        bridge = null
    }

    /** 返回当前命令桥；尚未接入 runtime 时返回 null。 */
    fun current(): PlaybackCommandBridge? {
        return bridge
    }
}

/**
 * 拦截系统 [MediaSession] 命令并优先回流 shared 协调器，避免直接改写 ExoPlayer 造成状态分叉。
 */
@UnstableApi
private class CoordinatorForwardingPlayer(
    player: Player,
) : ForwardingPlayer(player) {
    // 当前无命令桥时直接忽略系统命令，避免 service 在 shared 协调器之外修改 ExoPlayer。
    private fun currentBridge(): PlaybackCommandBridge? {
        return PlaybackCommandBridgeRegistry.current()
    }

    /** 系统播放命令优先改 shared 状态；未接桥时保持 no-op。 */
    override fun play() {
        val bridge: PlaybackCommandBridge = currentBridge() ?: return
        bridge.play()
    }

    /** 系统暂停命令优先改 shared 状态；未接桥时保持 no-op。 */
    override fun pause() {
        val bridge: PlaybackCommandBridge = currentBridge() ?: return
        bridge.pause()
    }

    /** 兼容系统通过 [setPlayWhenReady] 发出的播放/暂停命令。 */
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        val bridge: PlaybackCommandBridge = currentBridge() ?: return
        if (playWhenReady) {
            bridge.play()
            return
        }
        bridge.pause()
    }

    /** 系统上一首命令优先回流 shared 队列。 */
    override fun seekToPreviousMediaItem() {
        currentBridge()?.previous()
    }

    /** 兼容蓝牙或锁屏面板直接触发的“回到上一首”。 */
    override fun seekToPrevious() {
        currentBridge()?.previous()
    }

    /** 系统下一首命令优先回流 shared 队列。 */
    override fun seekToNextMediaItem() {
        currentBridge()?.next()
    }

    /** 兼容蓝牙或锁屏面板直接触发的“下一首”。 */
    override fun seekToNext() {
        currentBridge()?.next()
    }

    /** 系统 seek 命令优先通过 shared 协调器落库，再驱动真引擎。 */
    override fun seekTo(positionMs: Long) {
        currentBridge()?.seekTo(positionMs = positionMs)
    }

    /** 兼容系统通过指定媒体下标和进度发起的 seek 命令。 */
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        val bridge: PlaybackCommandBridge = currentBridge() ?: return
        bridge.skipToQueueIndex(
            index = mediaItemIndex,
            positionMs = positionMs,
        )
    }

    /** 兼容系统通过默认位置切换媒体项的命令。 */
    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        val bridge: PlaybackCommandBridge = currentBridge() ?: return
        bridge.skipToQueueIndex(
            index = mediaItemIndex,
            positionMs = 0L,
        )
    }
}
