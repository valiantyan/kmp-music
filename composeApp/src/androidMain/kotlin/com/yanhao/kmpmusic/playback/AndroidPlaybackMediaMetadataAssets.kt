package com.yanhao.kmpmusic.playback

import android.content.Context
import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * Android Media3 媒体元数据资源辅助器，负责把共享封面枚举转换为系统通知可读取的压缩图片数据。
 */
internal object AndroidPlaybackMediaMetadataAssets {
    // 解码后的封面字节缓存，避免队列刷新时反复读取 Compose resources assets。
    private val artworkDataCache: MutableMap<CoverArt, ByteArray> = LinkedHashMap()

    /**
     * 从 Compose Multiplatform 复制到 Android assets 的资源中读取系统媒体通知封面数据。
     */
    fun artworkData(context: Context, coverArt: CoverArt): ByteArray? {
        artworkDataCache[coverArt]?.let { cachedData: ByteArray ->
            return cachedData.copyOf()
        }
        return runCatching {
            context.assets.open(coverArt.assetPath()).use { input ->
                input.readBytes()
            }
        }.getOrNull()?.also { artworkData: ByteArray ->
            artworkDataCache[coverArt] = artworkData
        }?.copyOf()
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
