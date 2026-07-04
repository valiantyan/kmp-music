package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

// Figma 专辑详情页封面尺寸。
private val albumDetailCoverSize: Dp = 256.dp

// Figma 专辑详情页封面圆角。
private val albumDetailCoverShape: RoundedCornerShape = RoundedCornerShape(size = 32.dp)

/**
 * 专辑详情页顶部栏，在展开态只显示返回入口。
 */
@Composable
internal fun AlbumDetailToolbar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(size = 40.dp),
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = albumDetailTextColor,
            )
        }
        Spacer(modifier = Modifier.size(size = 40.dp))
    }
}

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
        modifier = Modifier.size(size = albumDetailCoverSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .offset(y = 18.dp)
                .size(width = 244.dp, height = 214.dp)
                .blur(radius = 20.dp)
                .background(
                    color = albumDetailActionColor.copy(alpha = 0.20f),
                    shape = albumDetailCoverShape,
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = albumDetailCoverShape)
                .border(
                    border = BorderStroke(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.30f),
                    ),
                    shape = albumDetailCoverShape,
                )
                .padding(all = 2.dp),
        ) {
            CoverArtImage(
                coverArt = album.coverArt,
                coverImageUri = album.coverImageUri,
                contentDescription = "${album.title} 专辑封面",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = RoundedCornerShape(size = 30.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
