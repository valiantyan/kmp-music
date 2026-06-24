package com.yanhao.kmpmusic.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.yanhao.kmpmusic.R
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlaybackMode
import com.yanhao.kmpmusic.domain.model.Song

/**
 * Android 播放前台通知 channel id。
 */
private const val CHANNEL_ID: String = "kmp_music_playback"

/**
 * Android 播放前台通知固定 id。
 */
const val PLAYBACK_NOTIFICATION_ID: Int = 42

/**
 * 负责构建 Android 自定义播放通知，仅组装 RemoteViews 与 PendingIntent。
 */
class AndroidPlaybackNotificationController(
    // 当前 Android 运行时上下文，仅用于创建通知对象与 PendingIntent。
    private val context: Context,
) {
    /**
     * 构建播放通知；命令点击只发广播，不在通知层直接改业务状态。
     */
    fun createNotification(
        song: Song,
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
        playbackPositionMs: Long,
        playbackDurationMs: Long?,
    ): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_loop_all)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed(song = song, isPlaying = isPlaying))
            .setCustomBigContentView(
                expanded(
                    song = song,
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
                    playbackMode = playbackMode,
                    playbackPositionMs = playbackPositionMs,
                    playbackDurationMs = playbackDurationMs,
                ),
            )
            .build()
    }

    /** 组装折叠态通知，严格只暴露上一首、播放/暂停、下一首三个控件。 */
    private fun collapsed(song: Song, isPlaying: Boolean): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_playback_collapsed).apply {
            setCoverImage(coverArt = song.coverArt)
            setTextViewText(R.id.notification_title, song.title)
            setTextViewText(R.id.notification_artist, song.artist)
            setImageViewResource(R.id.action_previous, R.drawable.ic_notification_previous)
            setImageViewResource(
                R.id.action_play_pause,
                if (isPlaying) {
                    R.drawable.ic_notification_pause
                } else {
                    R.drawable.ic_notification_play
                },
            )
            setImageViewResource(R.id.action_next, R.drawable.ic_notification_next)
            setOnClickPendingIntent(R.id.action_previous, pending(action = ACTION_PREVIOUS, requestCode = 1))
            setOnClickPendingIntent(
                R.id.action_play_pause,
                pending(action = ACTION_TOGGLE_PLAYBACK, requestCode = 2),
            )
            setOnClickPendingIntent(R.id.action_next, pending(action = ACTION_NEXT, requestCode = 3))
        }
    }

    /** 组装展开态通知，严格暴露收藏、上一首、播放/暂停、下一首、播放模式五个控件。 */
    private fun expanded(
        song: Song,
        isPlaying: Boolean,
        isFavorite: Boolean,
        playbackMode: PlaybackMode,
        playbackPositionMs: Long,
        playbackDurationMs: Long?,
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_playback_expanded).apply {
            setCoverImage(coverArt = song.coverArt)
            setTextViewText(R.id.notification_title, song.title)
            setTextViewText(R.id.notification_artist, song.artist)
            setProgressBar(
                R.id.notification_progress,
                AndroidPlaybackNotificationAssets.PROGRESS_MAX,
                AndroidPlaybackNotificationAssets.progressValue(
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs,
                ),
                false,
            )
            setTextViewText(
                R.id.notification_elapsed,
                AndroidPlaybackNotificationAssets.formatPlaybackTime(positionMs = playbackPositionMs),
            )
            setTextViewText(
                R.id.notification_duration,
                AndroidPlaybackNotificationAssets.formatPlaybackTime(positionMs = playbackDurationMs),
            )
            setImageViewResource(
                R.id.action_favorite,
                if (isFavorite) {
                    R.drawable.ic_notification_favorite
                } else {
                    R.drawable.ic_notification_favorite_border
                },
            )
            setImageViewResource(R.id.action_previous, R.drawable.ic_notification_previous)
            setImageViewResource(
                R.id.action_play_pause,
                if (isPlaying) {
                    R.drawable.ic_notification_pause
                } else {
                    R.drawable.ic_notification_play
                },
            )
            setImageViewResource(R.id.action_next, R.drawable.ic_notification_next)
            setImageViewResource(
                R.id.action_mode,
                when (playbackMode) {
                    PlaybackMode.LoopAll -> R.drawable.ic_notification_loop_all
                    PlaybackMode.LoopOne -> R.drawable.ic_notification_loop_one
                    PlaybackMode.Shuffle -> R.drawable.ic_notification_shuffle
                },
            )
            setOnClickPendingIntent(
                R.id.action_favorite,
                pending(action = ACTION_TOGGLE_FAVORITE, requestCode = 4),
            )
            setOnClickPendingIntent(R.id.action_previous, pending(action = ACTION_PREVIOUS, requestCode = 5))
            setOnClickPendingIntent(
                R.id.action_play_pause,
                pending(action = ACTION_TOGGLE_PLAYBACK, requestCode = 6),
            )
            setOnClickPendingIntent(R.id.action_next, pending(action = ACTION_NEXT, requestCode = 7))
            setOnClickPendingIntent(R.id.action_mode, pending(action = ACTION_CYCLE_MODE, requestCode = 8))
        }
    }

    /** 设置通知封面；真实封面读取失败时使用稳定占位图，避免布局空洞。 */
    private fun RemoteViews.setCoverImage(coverArt: CoverArt) {
        setImageViewResource(R.id.notification_cover, R.drawable.ic_notification_cover_placeholder)
        AndroidPlaybackNotificationAssets.coverBitmap(
            context = context,
            coverArt = coverArt,
        )?.let { bitmap ->
            setImageViewBitmap(R.id.notification_cover, bitmap)
        }
    }

    /** 为每个通知动作创建不可变广播 PendingIntent，保证命令来源稳定可复用。 */
    private fun pending(action: String, requestCode: Int): PendingIntent {
        val intent: Intent = Intent(context, PlaybackNotificationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 仅在 Android O 及以上确保播放通知 channel 存在，避免前台服务通知创建失败。 */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }
}
