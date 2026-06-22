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
        CoverArt.HeroLocalFolder -> Res.drawable.hero_local_folder
    }
}

/**
 * 将 domain 层封面标识映射到 Compose [Painter]。
 */
@Composable
fun coverArtPainter(coverArt: CoverArt): Painter {
    return painterResource(resource = coverArtResource(coverArt = coverArt))
}
