package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 折叠 Toolbar 使用深色遮罩接管状态栏，确保系统图标和标题始终可读。
internal val artistDetailToolbarColor: Color = Color(0xFF050607)

/**
 * 歌手详情页顶部折叠 chrome，承载沉浸头图、返回按钮、更多按钮和折叠标题。
 */
@Composable
internal fun ArtistDetailCollapsingChrome(
    artist: Artist,
    scrollState: ArtistDetailScrollState,
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ArtistDetailHeroImage(
            artist = artist,
            scrollState = scrollState,
        )
        ArtistDetailToolbar(
            artistName = artist.name,
            scrollState = scrollState,
            onBack = onBack,
            onMore = onMore,
        )
    }
}

// 头图随滚动折叠，回到顶部继续下拉时按上限轻微放大。
@Composable
private fun ArtistDetailHeroImage(
    artist: Artist,
    scrollState: ArtistDetailScrollState,
) {
    val stretchFraction: Float = calculateStretchFraction(scrollState = scrollState)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = scrollState.headerVisibleHeight)
            .clipToBounds(),
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
        ArtistDetailHeroScrim(scrollState = scrollState)
        ArtistDetailExpandedIdentity(
            artistName = artist.name,
            scrollState = scrollState,
        )
    }
}

// 顶部和底部渐变分别保护状态栏按钮、歌手名和列表入口的可读性。
@Composable
private fun ArtistDetailHeroScrim(scrollState: ArtistDetailScrollState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        artistDetailToolbarColor.copy(alpha = 0.48f),
                        Color.Transparent,
                        artistDetailToolbarColor.copy(alpha = 0.58f),
                    ),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = scrollState.collapsedToolbarHeight + 72.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        artistDetailToolbarColor.copy(alpha = 0.46f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

// 展开态歌手名放在头图底部，折叠时让位给 Toolbar 标题。
@Composable
private fun BoxScope.ArtistDetailExpandedIdentity(
    artistName: String,
    scrollState: ArtistDetailScrollState,
) {
    Text(
        text = artistName,
        color = Color.White,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.38f),
                blurRadius = 10f,
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .align(alignment = Alignment.BottomStart)
            .alpha(alpha = scrollState.expandedContentAlpha)
            .padding(start = 20.dp, end = 20.dp, bottom = 34.dp),
    )
}

// 把下拉高度归一化成图片缩放比例，最大值来自 [artistDetailMaxPullStretchHeight]。
private fun calculateStretchFraction(scrollState: ArtistDetailScrollState): Float {
    return (scrollState.pullStretchHeight.value / artistDetailMaxPullStretchHeight.value).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}
