package com.yanhao.kmpmusic.feature.components

import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * Coil 封面加载请求的纯数据部分，便于测试来源优先级。
 */
internal data class CoverArtImageRequest(
    val primaryModel: String,
    val fallbackResourcePath: String,
    val usesExternalCover: Boolean,
)

/**
 * 构建封面来源：扫描封面优先，应用内资源兜底。
 */
internal fun buildCoverArtImageRequest(
    coverArt: CoverArt,
    coverImageUri: String?,
): CoverArtImageRequest {
    val fallbackResourcePath: String = coverArtResourcePath(coverArt = coverArt)
    val normalizedCoverImageUri: String? = coverImageUri?.trim()?.takeIf { uri: String -> uri.isNotEmpty() }
    return CoverArtImageRequest(
        primaryModel = normalizedCoverImageUri ?: fallbackResourcePath,
        fallbackResourcePath = fallbackResourcePath,
        usesExternalCover = normalizedCoverImageUri != null,
    )
}

/**
 * Compose Multiplatform resources 中的封面路径，供 Res.getUri 加载。
 */
internal fun coverArtResourcePath(coverArt: CoverArt): String {
    return when (coverArt) {
        CoverArt.AlbumBestOfMe -> "drawable/album_best_of_me.png"
        CoverArt.AlbumRiverYear -> "drawable/album_river_year.png"
        CoverArt.AlbumTimeForest -> "drawable/album_time_forest.png"
        CoverArt.CoverSeaDream -> "drawable/cover_sea_dream.png"
        CoverArt.CoverSummerWaltz -> "drawable/cover_summer_waltz.png"
        CoverArt.HeroLocalMusic -> "drawable/hero_local_folder.png"
    }
}
