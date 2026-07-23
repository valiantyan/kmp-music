package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/** 最近播放卡在默认态保持 Figma 的纯封面，悬停后显露已有播放与更多操作。 */
@Composable
internal fun DesktopMeRecentPlayedCard(
    row: DesktopMeRecentPlayedSongDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DesktopMeRecentPlayedArtwork(
            row = row,
            onSongPlay = onSongPlay,
            onSongMore = onSongMore,
        )
        Text(
            text = row.title,
            modifier = Modifier.padding(top = 8.dp),
            color = if (row.isCurrentSong) DesktopMeFigmaTokens.Accent else Color.Black,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.playingIndicatorLabel ?: row.subtitle,
            color = if (row.isCurrentSong) DesktopMeFigmaTokens.Accent else DesktopMeFigmaTokens.Muted,
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 封面保持正方形和 16dp 圆角，悬停层不改变卡片尺寸或网格排版。 */
@Composable
private fun DesktopMeRecentPlayedArtwork(
    row: DesktopMeRecentPlayedSongDisplayModel,
    onSongPlay: (Song) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isHovered: Boolean by interactionSource.collectIsHoveredAsState()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .hoverable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = row.hasPlaybackAction,
                    onClick = { onSongPlay(row.song) },
                ),
    ) {
        CoverArtImage(
            coverArt = row.song.coverArt,
            coverImageUri = row.song.coverImageUri,
            contentDescription = "${row.title} 封面",
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentScale = ContentScale.Crop,
        )
        if (isHovered) {
            Box(
                modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "播放${row.title}",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                if (row.hasMoreAction) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.16f),
                        onClick = { onSongMore(row.song) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "${row.title} 更多操作",
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}
