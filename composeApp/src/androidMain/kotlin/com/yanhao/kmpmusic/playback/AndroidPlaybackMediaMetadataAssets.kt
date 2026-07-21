package com.yanhao.kmpmusic.playback

import android.content.Context
import android.net.Uri
import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * Android Media3 媒体元数据资源辅助器，负责把共享封面枚举转换为系统通知可读取的压缩图片数据。
 */
internal object AndroidPlaybackMediaMetadataAssets {
    // 解码后的封面字节缓存，避免队列刷新时反复读取 Compose resources assets。
    private val artworkDataCache: MutableMap<CoverArt, ByteArray> = LinkedHashMap()

    /**
     * 优先读取扫描音频提取出的封面图片，缺失时回退到内置封面资源。
     */
    fun artworkData(
        context: Context,
        coverImageUri: String?,
        coverArt: CoverArt,
    ): ByteArray? {
        readCoverImageUri(
            context = context,
            coverImageUri = coverImageUri,
        )?.let { artworkData: ByteArray ->
            return artworkData
        }
        artworkDataCache[coverArt]?.let { cachedData: ByteArray ->
            return cachedData.copyOf()
        }
        return runCatching {
            context.assets.open(coverArt.assetPath()).use { input ->
                input.readBytes()
            }
        }.getOrNull()
            ?.also { artworkData: ByteArray ->
                artworkDataCache[coverArt] = artworkData
            }?.copyOf()
    }

    // 系统媒体通知不能读取 Compose 自绘状态，因此这里把真实封面 URI 转成 Media3 metadata 字节。
    private fun readCoverImageUri(
        context: Context,
        coverImageUri: String?,
    ): ByteArray? {
        val normalizedCoverImageUri: String =
            coverImageUri?.trim()?.takeIf { uri: String ->
                uri.isNotEmpty()
            } ?: return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(normalizedCoverImageUri))?.use { input ->
                input.readBytes()
            }
        }.getOrNull()?.takeIf { artworkData: ByteArray -> artworkData.isNotEmpty() }
    }

    // 将 domain 封面枚举映射到 Android assets 中的 Compose 资源路径。
    private fun CoverArt.assetPath(): String {
        val fileName: String =
            when (this) {
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
