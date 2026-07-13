package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.ArtistDetailPalette
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 折叠 Toolbar 使用浅色背景贴合当前 App 视觉体系。
internal val artistDetailToolbarColor: Color = MusicColors.MobileToolbarBackground

// 头图顶部使用浅色遮罩承接深色返回按钮。
private val artistDetailHeroTopScrimColor: Color = MusicColors.Paper

// 头图最后 1/4 从完全可见渐隐到透明，露出正文背景。
private const val ARTIST_DETAIL_HERO_FADE_START_FRACTION = 0.75f

/**
 * 歌手详情页沉浸头图层，只负责图片和遮罩，Toolbar 在页面根部单独置顶。
 */
@Composable
internal fun ArtistDetailHeroChrome(
    artist: Artist,
    palette: ArtistDetailPalette,
    scrollState: State<ArtistDetailScrollState>,
    layoutState: ArtistDetailLayoutState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ArtistDetailHeroImage(
            artist = artist,
            palette = palette,
            scrollState = scrollState,
            layoutState = layoutState,
        )
    }
}

// 头图随滚动折叠，回到顶部继续下拉时按上限轻微放大。
@Composable
private fun ArtistDetailHeroImage(
    artist: Artist,
    palette: ArtistDetailPalette,
    scrollState: State<ArtistDetailScrollState>,
    layoutState: ArtistDetailLayoutState,
) {
    val density: Density = LocalDensity.current
    val stretchFraction: Float = calculateStretchFraction(layoutState = layoutState)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = layoutState.heroImageHeight)
            .clipToBounds()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                translationY = with(density) { scrollState.value.heroImageOffset.toPx() }
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White,
                            ARTIST_DETAIL_HERO_FADE_START_FRACTION to Color.White,
                            1f to Color.Transparent,
                        ),
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        CoverArtImage(
            coverArt = artist.coverArt,
            coverImageUri = artist.coverImageUri,
            contentDescription = "${artist.name} 歌手头图",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f + stretchFraction * 0.16f
                    scaleY = 1f + stretchFraction * 0.16f
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
                },
            contentScale = ContentScale.Crop,
        )
        ArtistDetailHeroScrim(
            palette = palette,
            collapsedToolbarHeight = layoutState.collapsedToolbarHeight,
        )
    }
}

// 从图片中线附近开始压暗，保护展开态标题和按钮的可读性。
@Composable
private fun ArtistDetailHeroScrim(
    palette: ArtistDetailPalette,
    collapsedToolbarHeight: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to artistDetailHeroTopScrimColor.copy(alpha = 0.50f),
                        0.22f to Color.Transparent,
                        0.58f to Color.Transparent,
                        0.78f to palette.heroScrimColor.copy(alpha = 0.34f),
                        1f to palette.heroScrimColor.copy(alpha = 0.48f),
                    ),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = collapsedToolbarHeight + 72.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        artistDetailHeroTopScrimColor.copy(alpha = 0.54f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

// 把下拉高度归一化成图片缩放比例，最大值来自 [artistDetailMaxPullStretchHeight]。
private fun calculateStretchFraction(layoutState: ArtistDetailLayoutState): Float {
    return (layoutState.pullStretchHeight.value / artistDetailMaxPullStretchHeight.value).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}
