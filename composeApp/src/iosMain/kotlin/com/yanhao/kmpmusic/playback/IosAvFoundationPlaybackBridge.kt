package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemPlaybackStalledNotification
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setVolume
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL

private const val IOS_INTERRUPTION_TYPE_ENDED: Long = 0L
private const val IOS_INTERRUPTION_TYPE_BEGAN: Long = 1L
private const val IOS_INTERRUPTION_OPTION_SHOULD_RESUME: Long = 1L
private const val IOS_ROUTE_CHANGE_REASON_OLD_DEVICE_UNAVAILABLE: Long = 2L

/**
 * iOS AVFoundation bridge，负责把 [AVPlayer] 原生事实转换成平台内部事件。
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosAvFoundationPlaybackBridge(
    // 单个 AVPlayer，避免 AVQueuePlayer 接管业务队列。
    private val player: AVPlayer = AVPlayer(),
    // Foundation 通知中心，测试和真实实现都从这里移除观察器。
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
) : IosPlaybackBridge {
    // native 事件通道，交给 engine 做 generation 过滤和 common 事件归一化。
    private val eventChannel: Channel<IosPlaybackBridgeEvent> = Channel(capacity = Channel.UNLIMITED)
    // 当前媒体代号，通知回调通过该值归因。
    private var generation: Long = 0L
    // 当前媒体标识，失败映射需要带回 songId。
    private var songId: String? = null
    // 当前 item 观察器 token，切歌或 release 时必须移除。
    private val itemObserverTokens: MutableList<Any> = mutableListOf()
    // audio session 观察器 token，release 时必须移除。
    private val audioSessionObserverTokens: MutableList<Any> = mutableListOf()
    // AVPlayer 周期进度观察器 token，切歌、停止或 release 时必须移除。
    private var timeObserverToken: Any? = null
    // 当前是否已经释放。
    private var isReleased: Boolean = false
    // 当前媒体的最后已知进度。
    private var lastPositionMs: Long = 0L

    init {
        installAudioSessionObservers()
    }

    /** 对外暴露 AVFoundation 事件流。 */
    override val events: Flow<IosPlaybackBridgeEvent> = eventChannel.receiveAsFlow()

    /** 准备单个 AVPlayerItem，并安装当前 item 的结束、失败和卡顿通知。 */
    override suspend fun prepare(request: IosPlaybackBridgePrepareRequest): IosPlaybackBridgeCommandAck {
        if (isReleased) {
            return IosPlaybackBridgeCommandAck.Failed(error = buildEngineUnavailableError(songId = request.songId))
        }
        val url: NSURL = NSURL.URLWithString(URLString = request.mediaUri)
            ?: return IosPlaybackBridgeCommandAck.Failed(error = buildUnsupportedUriError(songId = request.songId))
        removeObservers()
        generation = request.generation
        songId = request.songId
        lastPositionMs = request.startPositionMs.coerceAtLeast(minimumValue = 0L)
        val item: AVPlayerItem = AVPlayerItem.playerItemWithURL(URL = url)
        installItemObservers(
            item = item,
            activeGeneration = request.generation,
            activeSongId = request.songId,
        )
        player.replaceCurrentItemWithPlayerItem(item = item)
        installTimeObserver(
            item = item,
            activeGeneration = request.generation,
        )
        if (lastPositionMs > 0L) {
            player.seekToTime(time = timeFromMillis(positionMs = lastPositionMs))
        }
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Prepared(
                generation = generation,
                durationMs = durationOf(item = item),
            ),
        )
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 调用 [AVPlayer.play] 并回传播放中事实。 */
    override suspend fun play(generation: Long): IosPlaybackBridgeCommandAck {
        if (!isGenerationCurrent(generation = generation)) {
            return IosPlaybackBridgeCommandAck.Accepted
        }
        player.play()
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Playing(
                generation = generation,
                positionMs = currentPositionMs(),
                durationMs = durationOf(item = player.currentItem),
            ),
        )
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 调用 [AVPlayer.pause] 并回传暂停事实。 */
    override suspend fun pause(generation: Long): IosPlaybackBridgeCommandAck {
        if (!isGenerationCurrent(generation = generation)) {
            return IosPlaybackBridgeCommandAck.Accepted
        }
        player.pause()
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Paused(
                generation = generation,
                positionMs = currentPositionMs(),
                durationMs = durationOf(item = player.currentItem),
            ),
        )
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 执行 seek 并回传最新进度。 */
    override suspend fun seekTo(request: IosPlaybackBridgeSeekRequest): IosPlaybackBridgeCommandAck {
        if (!isGenerationCurrent(generation = request.generation)) {
            return IosPlaybackBridgeCommandAck.Accepted
        }
        lastPositionMs = request.positionMs.coerceAtLeast(minimumValue = 0L)
        player.seekToTime(time = timeFromMillis(positionMs = lastPositionMs))
        eventChannel.trySend(
            IosPlaybackBridgeEvent.Progress(
                generation = request.generation,
                positionMs = lastPositionMs,
                durationMs = durationOf(item = player.currentItem),
            ),
        )
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 停止当前 item 并移除 item 观察器。 */
    override suspend fun stop(generation: Long): IosPlaybackBridgeCommandAck {
        if (!isGenerationCurrent(generation = generation)) {
            return IosPlaybackBridgeCommandAck.Accepted
        }
        player.pause()
        removeObservers()
        player.replaceCurrentItemWithPlayerItem(item = null)
        lastPositionMs = 0L
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 设置 [AVPlayer.volume]，只表达 App 内相对音量。 */
    override suspend fun setVolume(volume: Float): IosPlaybackBridgeCommandAck {
        player.setVolume(volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f))
        return IosPlaybackBridgeCommandAck.Accepted
    }

    /** 释放观察器和当前 [AVPlayerItem]。 */
    override suspend fun release(): IosPlaybackBridgeCommandAck {
        if (isReleased) {
            return IosPlaybackBridgeCommandAck.Accepted
        }
        isReleased = true
        player.pause()
        removeObservers()
        removeAudioSessionObservers()
        player.replaceCurrentItemWithPlayerItem(item = null)
        return IosPlaybackBridgeCommandAck.Accepted
    }

    // 安装当前 item 的结束、失败和卡顿通知。
    private fun installItemObservers(
        item: AVPlayerItem,
        activeGeneration: Long,
        activeSongId: String,
    ) {
        itemObserverTokens += addNotificationObserver(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            item = item,
        ) {
            eventChannel.trySend(element = IosPlaybackBridgeEvent.Ended(generation = activeGeneration))
        }
        itemObserverTokens += addNotificationObserver(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            item = item,
        ) {
            eventChannel.trySend(
                element = IosPlaybackBridgeEvent.Failed(
                    generation = activeGeneration,
                    error = buildUnknownError(
                        songId = activeSongId,
                        message = "iOS AVPlayer 播放到结束前失败",
                    ),
                ),
            )
        }
        itemObserverTokens += addNotificationObserver(
            name = AVPlayerItemPlaybackStalledNotification,
            item = item,
        ) {
            eventChannel.trySend(
                element = IosPlaybackBridgeEvent.Buffering(
                    generation = activeGeneration,
                    positionMs = currentPositionMs(),
                    durationMs = durationOf(item = item),
                ),
            )
        }
    }

    // 安装 audio session 中断和输出路线变化观察器。
    private fun installAudioSessionObservers() {
        audioSessionObserverTokens += addNotificationObserver(
            name = AVAudioSessionInterruptionNotification,
            item = null,
        ) { notification: NSNotification? ->
            handleInterruptionNotification(notification = notification)
        }
        audioSessionObserverTokens += addNotificationObserver(
            name = AVAudioSessionRouteChangeNotification,
            item = null,
        ) { notification: NSNotification? ->
            handleRouteChangeNotification(notification = notification)
        }
    }

    // 安装 AVPlayer 周期进度观察器，保证真实播放期间持续回流进度。
    private fun installTimeObserver(
        item: AVPlayerItem,
        activeGeneration: Long,
    ) {
        removeTimeObserver()
        timeObserverToken = player.addPeriodicTimeObserverForInterval(
            interval = timeFromMillis(positionMs = 1_000L),
            queue = null,
        ) { time: CValue<CMTime> ->
            lastPositionMs = millisFromTime(time = time)
            eventChannel.trySend(
                element = IosPlaybackBridgeEvent.Progress(
                    generation = activeGeneration,
                    positionMs = lastPositionMs,
                    durationMs = durationOf(item = item),
                ),
            )
        }
    }

    // 通过 Foundation 通知中心注册观察器。
    private fun addNotificationObserver(
        name: String?,
        item: Any?,
        handler: (NSNotification?) -> Unit,
    ): Any {
        return notificationCenter.addObserverForName(
            name = name,
            `object` = item,
            queue = NSOperationQueue.mainQueue,
            usingBlock = handler,
        )
    }

    // 移除当前媒体相关观察器。
    private fun removeObservers() {
        itemObserverTokens.forEach { token: Any ->
            notificationCenter.removeObserver(observer = token)
        }
        itemObserverTokens.clear()
        removeTimeObserver()
    }

    // 移除 audio session 观察器。
    private fun removeAudioSessionObservers() {
        audioSessionObserverTokens.forEach { token: Any ->
            notificationCenter.removeObserver(observer = token)
        }
        audioSessionObserverTokens.clear()
    }

    // 移除 AVPlayer 周期进度观察器。
    private fun removeTimeObserver() {
        val token: Any = timeObserverToken ?: return
        player.removeTimeObserver(observer = token)
        timeObserverToken = null
    }

    // 将系统中断通知归一化为平台内部事件。
    private fun handleInterruptionNotification(notification: NSNotification?) {
        val userInfo: Map<Any?, *> = notification?.userInfo ?: return
        val type: Long = longValue(
            dictionary = userInfo,
            key = AVAudioSessionInterruptionTypeKey,
        ) ?: return
        when (type) {
            IOS_INTERRUPTION_TYPE_BEGAN -> eventChannel.trySend(
                element = IosPlaybackBridgeEvent.InterruptionBegan(
                    generation = generation,
                    positionMs = currentPositionMs(),
                    durationMs = durationOf(item = player.currentItem),
                ),
            )
            IOS_INTERRUPTION_TYPE_ENDED -> eventChannel.trySend(
                element = IosPlaybackBridgeEvent.InterruptionEnded(
                    generation = generation,
                    shouldResume = shouldResumeAfterInterruption(userInfo = userInfo),
                ),
            )
        }
    }

    // 将旧输出设备不可用的路线变化归一化为输出断开事件。
    private fun handleRouteChangeNotification(notification: NSNotification?) {
        val userInfo: Map<Any?, *> = notification?.userInfo ?: return
        val reason: Long = longValue(
            dictionary = userInfo,
            key = AVAudioSessionRouteChangeReasonKey,
        ) ?: return
        if (reason != IOS_ROUTE_CHANGE_REASON_OLD_DEVICE_UNAVAILABLE) {
            return
        }
        eventChannel.trySend(
            element = IosPlaybackBridgeEvent.OutputDisconnected(
                generation = generation,
                positionMs = currentPositionMs(),
                durationMs = durationOf(item = player.currentItem),
            ),
        )
    }

    // 判断系统是否提示中断结束后可恢复播放。
    private fun shouldResumeAfterInterruption(userInfo: Map<Any?, *>): Boolean {
        val option: Long = longValue(
            dictionary = userInfo,
            key = AVAudioSessionInterruptionOptionKey,
        ) ?: return false
        return option and IOS_INTERRUPTION_OPTION_SHOULD_RESUME != 0L
    }

    // 从 notification userInfo 里读取 NSNumber 的 Long 值。
    private fun longValue(dictionary: Map<Any?, *>, key: String?): Long? {
        val number: NSNumber = dictionary[key] as? NSNumber ?: return null
        return number.longLongValue
    }

    // 判断命令是否仍属于当前 generation。
    private fun isGenerationCurrent(generation: Long): Boolean {
        return !isReleased && generation == this.generation
    }

    // 构造毫秒精度 CMTime。
    private fun timeFromMillis(positionMs: Long): CValue<CMTime> {
        return CMTimeMake(
            value = positionMs.coerceAtLeast(minimumValue = 0L),
            timescale = 1_000,
        )
    }

    // 读取当前播放进度，无法读取时回退到上一帧进度。
    private fun currentPositionMs(): Long {
        return millisFromTime(time = player.currentTime())
    }

    // 将 CMTime 转成毫秒，无法读取时回退到上一帧进度。
    private fun millisFromTime(time: CValue<CMTime>): Long {
        val seconds: Double = CMTimeGetSeconds(time)
        if (!seconds.isFinite()) {
            return lastPositionMs
        }
        lastPositionMs = (seconds * 1_000.0).toLong().coerceAtLeast(minimumValue = 0L)
        return lastPositionMs
    }

    // 读取当前 item 时长，未知或无穷时保留 null。
    private fun durationOf(item: AVPlayerItem?): Long? {
        val duration: CValue<CMTime> = item?.duration ?: return null
        val seconds: Double = CMTimeGetSeconds(duration)
        if (!seconds.isFinite() || seconds <= 0.0) {
            return null
        }
        return (seconds * 1_000.0).toLong()
    }

    // 构造不可播放 URI 错误。
    private fun buildUnsupportedUriError(songId: String): PlaybackError {
        return PlaybackError(
            type = PlaybackErrorType.UnsupportedFormat,
            songId = songId,
            message = "iOS AVFoundation 无法解析当前音频来源",
        )
    }

    // 构造 bridge 已释放错误。
    private fun buildEngineUnavailableError(songId: String): PlaybackError {
        return PlaybackError(
            type = PlaybackErrorType.EngineUnavailable,
            songId = songId,
            message = "iOS AVFoundation 播放器已经释放",
        )
    }

    // 构造未知播放错误。
    private fun buildUnknownError(songId: String?, message: String): PlaybackError {
        return PlaybackError(
            type = PlaybackErrorType.Unknown,
            songId = songId,
            message = message,
        )
    }
}
