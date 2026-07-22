package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 头像优先用扫描封面；缺图时使用 Figma 的音乐符号渐变占位。
@Composable
internal fun DesktopLocalArtistAvatar(
    artist: Artist,
    index: Int,
    visualSpec: DesktopLocalArtistListVisualSpec,
) {
    Surface(
        modifier = Modifier.size(size = visualSpec.avatarOuterSize),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(width = visualSpec.avatarBorderWidth, color = Color.White),
        shadowElevation = visualSpec.avatarShadowElevation,
    ) {
        Box(modifier = Modifier.padding(all = visualSpec.avatarInset)) {
            if (shouldUseDesktopLocalArtistPlaceholderArtwork(artist = artist)) {
                DesktopLocalArtistPlaceholderAvatar(index = index)
            } else {
                CoverArtImage(
                    coverArt = artist.coverArt,
                    coverImageUri = artist.coverImageUri,
                    contentDescription = "${artist.name} 歌手头像",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(shape = CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

// 占位头像用两组浅色渐变交替，接近 Figma 的绿色和蓝色缺图态。
@Composable
private fun DesktopLocalArtistPlaceholderAvatar(index: Int) {
    val isGreenVariant: Boolean = index % 2 == 1
    val backgroundColor: Color = if (isGreenVariant) Color(0x3300BFA5) else Color(0x33D0E1FB)
    val gradient: Brush =
        if (isGreenVariant) {
            Brush.linearGradient(
                colors = listOf(Color(0x4D00BFA5), Color(0x4DD0E1FB)),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 60f, y = 60f),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0x66D0E1FB), Color(0x66D8E3FB)),
                start = Offset(x = 0f, y = 60f),
                end = Offset(x = 60f, y = 0f),
            )
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(shape = CircleShape)
                .background(color = backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(brush = gradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DesktopFigmaMusicNoteIcon,
                contentDescription = null,
                tint = if (isGreenVariant) Color(0xFF006B5C) else Color(0xFF505F76),
                modifier = Modifier.size(width = 15.dp, height = 22.5.dp),
            )
        }
    }
}

/** 只有没有扫描封面且只有本地默认图时才使用歌手页占位头像。 */
internal fun shouldUseDesktopLocalArtistPlaceholderArtwork(artist: Artist): Boolean = artist.coverImageUri.isNullOrBlank() && artist.coverArt == CoverArt.HeroLocalMusic
