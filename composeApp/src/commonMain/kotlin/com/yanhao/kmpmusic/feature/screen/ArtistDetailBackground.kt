package com.yanhao.kmpmusic.feature.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.ArtistDetailPalette
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 背景从当前歌手头像取色，再以低透明度渐变承载沉浸式二级页。
@Composable
internal fun ArtistDetailBackground(
    artist: Artist,
    palette: ArtistDetailPalette,
) {
    val backgroundColor: Color by animateColorAsState(
        targetValue = palette.backgroundColor,
        animationSpec = tween(durationMillis = 260),
        label = "ArtistDetailBackgroundColor",
    )
    val ambientColor: Color by animateColorAsState(
        targetValue = palette.ambientColor,
        animationSpec = tween(durationMillis = 260),
        label = "ArtistDetailAmbientColor",
    )
    val contentColor: Color by animateColorAsState(
        targetValue = palette.contentColor,
        animationSpec = tween(durationMillis = 260),
        label = "ArtistDetailContentColor",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor),
    ) {
        CoverArtImage(
            coverArt = artist.coverArt,
            coverImageUri = artist.coverImageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha = 0.10f)
                .blur(radius = 56.dp),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to ambientColor.copy(alpha = 0.08f),
                            0.08f to contentColor.copy(alpha = 0.92f),
                            0.48f to contentColor.copy(alpha = 0.96f),
                            1f to contentColor,
                        ),
                    ),
                ),
        )
    }
}
