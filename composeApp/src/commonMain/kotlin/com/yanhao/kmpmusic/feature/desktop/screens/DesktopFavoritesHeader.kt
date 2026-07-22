package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/** Figma 收藏页顶部由动态封面、歌曲数量和两种播放命令组成。 */
@Composable
internal fun DesktopFavoritesHeader(
    displayModel: DesktopFavoritesDisplayModel,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        DesktopFavoritesHeroArtwork(song = displayModel.heroArtworkSong)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
        ) {
            Text(
                text = "收藏",
                color = Color(0xFF111C2D),
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DesktopFavoritesSongCount(label = displayModel.songCountLabel)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopFavoritesActionButton(
                    text = displayModel.playAllLabel,
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    enabled = displayModel.isPlaybackEnabled,
                    onClick = onPlayAll,
                )
                DesktopFavoritesActionButton(
                    text = "随机播放",
                    icon = Icons.Rounded.Shuffle,
                    isPrimary = false,
                    enabled = displayModel.isPlaybackEnabled,
                    onClick = onShuffle,
                )
            }
        }
    }
}

/** 优先显示收藏列表的第一张真实歌曲封面，全部缺失时保留 Figma 默认封面。 */
@Composable
private fun DesktopFavoritesHeroArtwork(song: Song?) {
    val artworkShape: RoundedCornerShape = RoundedCornerShape(16.dp)
    val artworkModifier: Modifier =
        Modifier
            .size(192.dp)
            .shadow(
                elevation = 16.dp,
                shape = artworkShape,
                clip = false,
            ).clip(artworkShape)
    if (song != null) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 收藏封面",
            modifier = artworkModifier,
            contentScale = ContentScale.Crop,
        )
        return
    }
    Box(
        modifier =
            artworkModifier.background(
                brush = Brush.linearGradient(colors = listOf(Color(0xFF006B5C), Color(0xFF00BFA5))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(80.dp),
        )
    }
}

/** 数量后的圆点来自 Figma 元信息行，始终保留以稳定标题区宽度。 */
@Composable
private fun DesktopFavoritesSongCount(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xB33C4A46),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
        Box(
            modifier =
                Modifier
                    .size(4.dp)
                    .background(
                        color = Color(0x4D3C4A46),
                        shape = CircleShape,
                    ),
        )
    }
}
