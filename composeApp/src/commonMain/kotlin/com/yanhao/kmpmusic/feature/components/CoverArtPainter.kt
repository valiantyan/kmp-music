package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.yanhao.kmpmusic.domain.model.CoverArt
import kmpmusic.composeapp.generated.resources.Res
import kmpmusic.composeapp.generated.resources.album_best_of_me
import kmpmusic.composeapp.generated.resources.album_river_year
import kmpmusic.composeapp.generated.resources.album_time_forest
import kmpmusic.composeapp.generated.resources.cover_sea_dream
import kmpmusic.composeapp.generated.resources.cover_summer_waltz
import kmpmusic.composeapp.generated.resources.hero_local_folder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 将 domain 层封面标识映射到 Compose 图片资源。
 */
fun coverArtResource(coverArt: CoverArt): DrawableResource {
    return when (coverArt) {
        CoverArt.AlbumBestOfMe -> Res.drawable.album_best_of_me
        CoverArt.AlbumRiverYear -> Res.drawable.album_river_year
        CoverArt.AlbumTimeForest -> Res.drawable.album_time_forest
        CoverArt.CoverSeaDream -> Res.drawable.cover_sea_dream
        CoverArt.CoverSummerWaltz -> Res.drawable.cover_summer_waltz
        CoverArt.HeroLocalMusic -> Res.drawable.hero_local_folder
    }
}

/**
 * 将 domain 层封面标识映射到 Compose [Painter]。
 */
@Composable
fun coverArtPainter(coverArt: CoverArt): Painter {
    return coverArtPainter(
        coverArt = coverArt,
        coverImageUri = null,
    )
}

/**
 * 优先绘制扫描得到的音频封面，缺失或解码失败时使用应用内兜底封面。
 */
@Composable
fun coverArtPainter(
    coverArt: CoverArt,
    coverImageUri: String?,
): Painter {
    return rememberPlatformCoverArtPainter(
        coverImageUri = coverImageUri,
        fallbackCoverArt = coverArt,
    )
}

/**
 * 由各平台负责解码本地封面 URI，避免 common UI 依赖平台文件或媒体 API。
 */
@Composable
internal expect fun rememberPlatformCoverArtPainter(
    coverImageUri: String?,
    fallbackCoverArt: CoverArt,
): Painter

/**
 * 应用内兜底资源的 [Painter]，供平台实现复用同一套映射规则。
 */
@Composable
internal fun fallbackCoverArtPainter(coverArt: CoverArt): Painter {
    return painterResource(resource = coverArtResource(coverArt = coverArt))
}
