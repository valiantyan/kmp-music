package com.yanhao.kmpmusic.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * Android 播放通知资源辅助器，隔离 RemoteViews 所需的封面与时间格式。
 */
internal object AndroidPlaybackNotificationAssets {
    // 通知进度条使用的固定最大值，避免 RemoteViews 直接处理毫秒级大数。
    const val PROGRESS_MAX: Int = 1_000

    // 解码后的封面缓存，避免进度刷新时反复读取 assets。
    private val coverBitmapCache: MutableMap<CoverArt, Bitmap> = LinkedHashMap()

    /**
     * 从 Compose Multiplatform 资源 assets 中读取通知封面。
     */
    fun coverBitmap(context: Context, coverArt: CoverArt): Bitmap? {
        coverBitmapCache[coverArt]?.let { bitmap: Bitmap ->
            return bitmap
        }
        return runCatching {
            context.assets.open(coverArt.assetPath()).use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()?.also { bitmap: Bitmap ->
            coverBitmapCache[coverArt] = bitmap
        }
    }

    /**
     * 把真实进度换算成 RemoteViews 进度条需要的 0..[PROGRESS_MAX]。
     */
    fun progressValue(positionMs: Long, durationMs: Long?): Int {
        val safeDurationMs: Long = durationMs?.takeIf { value -> value > 0L } ?: return 0
        val fraction: Float = positionMs
            .coerceIn(minimumValue = 0L, maximumValue = safeDurationMs)
            .toFloat() / safeDurationMs.toFloat()
        return (fraction * PROGRESS_MAX).toInt().coerceIn(minimumValue = 0, maximumValue = PROGRESS_MAX)
    }

    /**
     * 通知时间统一按 mm:ss 显示，未知或负值统一回退到 0:00。
     */
    fun formatPlaybackTime(positionMs: Long?): String {
        val totalSeconds: Long = ((positionMs ?: 0L) / 1_000L).coerceAtLeast(minimumValue = 0L)
        val minutes: Long = totalSeconds / 60L
        val seconds: Long = totalSeconds % 60L
        return "$minutes:${seconds.toString().padStart(length = 2, padChar = '0')}"
    }

    // 将 domain 封面枚举映射到 Android assets 中的 Compose 资源路径。
    private fun CoverArt.assetPath(): String {
        val fileName: String = when (this) {
            CoverArt.AlbumBestOfMe -> "album_best_of_me.png"
            CoverArt.AlbumRiverYear -> "album_river_year.png"
            CoverArt.AlbumTimeForest -> "album_time_forest.png"
            CoverArt.CoverSeaDream -> "cover_sea_dream.png"
            CoverArt.CoverSummerWaltz -> "cover_summer_waltz.png"
            CoverArt.HeroLocalMusic -> "hero_local_folder.png"
        }
        return "composeResources/kmpmusic.composeapp.generated.resources/drawable/$fileName"
    }
}
