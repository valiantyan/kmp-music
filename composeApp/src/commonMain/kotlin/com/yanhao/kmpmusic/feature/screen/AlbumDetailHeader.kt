package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// Figma 详情页封面尺寸，专辑和歌单详情共用同一视觉规格。
internal val detailHeroCoverSize: Dp = 256.dp

// Figma 详情页封面圆角，避免不同详情页产生视觉分叉。
internal val detailHeroCoverShape: RoundedCornerShape = RoundedCornerShape(size = 32.dp)

// Figma 详情页封面内层圆角。
internal val detailHeroCoverInnerShape: RoundedCornerShape = RoundedCornerShape(size = 30.dp)

// Figma 详情页封面环境光宽度。
internal val detailHeroCoverGlowWidth: Dp = 244.dp

// Figma 详情页封面环境光高度。
internal val detailHeroCoverGlowHeight: Dp = 214.dp

// Figma 详情页封面环境光下移距离。
internal val detailHeroCoverGlowOffsetY: Dp = 18.dp

// Figma 详情页封面环境光模糊半径。
internal val detailHeroCoverGlowBlurRadius: Dp = 20.dp

// Figma 详情页封面边框宽度。
internal val detailHeroCoverBorderWidth: Dp = 2.dp

// Figma 详情页封面内边距。
internal val detailHeroCoverPadding: Dp = 2.dp

// Figma 详情页封面白色描边透明度。
internal const val DETAIL_HERO_COVER_BORDER_ALPHA = 0.30f

/**
 * 专辑详情页头部使用居中大封面和两行文字，避免复用旧横向详情样式。
 */
@Composable
internal fun AlbumDetailHeader(album: Album) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumDetailCover(album = album)
        Text(
            text = album.title,
            color = albumDetailTextColor,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 32.dp, end = 20.dp),
        )
        Text(
            text = album.artist,
            color = albumDetailMetaColor,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
    }
}

// 封面背后的模糊色块来自 Figma，用主题青色模拟封面环境光。
@Composable
private fun AlbumDetailCover(album: Album) {
    Box(
        modifier = Modifier.size(size = detailHeroCoverSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .offset(y = detailHeroCoverGlowOffsetY)
                .size(width = detailHeroCoverGlowWidth, height = detailHeroCoverGlowHeight)
                .blur(radius = detailHeroCoverGlowBlurRadius)
                .background(
                    color = albumDetailActionColor.copy(alpha = 0.20f),
                    shape = detailHeroCoverShape,
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = detailHeroCoverShape)
                .border(
                    border = BorderStroke(
                        width = detailHeroCoverBorderWidth,
                        color = Color.White.copy(alpha = DETAIL_HERO_COVER_BORDER_ALPHA),
                    ),
                    shape = detailHeroCoverShape,
                )
                .padding(all = detailHeroCoverPadding),
        ) {
            CoverArtImage(
                coverArt = album.coverArt,
                coverImageUri = album.coverImageUri,
                contentDescription = "${album.title} 专辑封面",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = detailHeroCoverInnerShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
