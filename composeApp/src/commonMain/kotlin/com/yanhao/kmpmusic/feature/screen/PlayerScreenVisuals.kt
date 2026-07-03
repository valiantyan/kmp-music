package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 背景使用当前封面取色和模糊封面，保证播放页主视觉随歌曲变化。
@Composable
internal fun PlayerBackground(
    song: Song,
    palette: PlayerPagePalette,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor),
    ) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha = 0.16f)
                .blur(radius = scaledDp(54.dp)),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.ambientColor.copy(alpha = 0.46f),
                            Color.Transparent,
                        ),
                        radius = 720f,
                    ),
                )
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.30f),
                            0.42f to backgroundColor.copy(alpha = 0.72f),
                            1f to backgroundColor,
                        ),
                    ),
                ),
        )
    }
}

// 顶栏只保留向下返回按钮，让播放页更接近全屏 Now Playing。
@Composable
internal fun PlayerTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.width(width = scaledDp(326.dp)),
        horizontalArrangement = Arrangement.Start,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(size = scaledDp(44.dp)),
        ) {
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = "返回",
                tint = MusicColors.AccentDeep,
            )
        }
    }
}

// 封面保留 Figma 的大圆角、柔和投影和细边框。
@Composable
internal fun PlayerCoverArt(
    song: Song,
    coverSize: Dp,
) {
    Box(
        modifier = Modifier
            .size(size = coverSize)
            .shadow(
                elevation = scaledDp(24.dp),
                shape = RoundedCornerShape(size = scaledDp(32.dp)),
                ambientColor = MusicColors.AccentDeep.copy(alpha = 0.22f),
                spotColor = MusicColors.AccentDeep.copy(alpha = 0.22f),
            )
            .clip(shape = RoundedCornerShape(size = scaledDp(32.dp)))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = RoundedCornerShape(size = scaledDp(30.dp))),
            contentScale = ContentScale.Crop,
        )
    }
}
