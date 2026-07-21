package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

// 歌名与歌手使用固定两行高度，避免超长标题撑高卡片。
@Composable
internal fun DesktopHomeSongText(
    song: Song,
    textColor: Color,
    metaColor: Color,
    isCurrentSong: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = song.title,
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            color = metaColor,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 没有真实封面时显示设计稿里的音乐符号色块，避免误导为扫描到专辑图。
@Composable
internal fun DesktopHomeSongArtwork(
    song: Song,
    isCurrentSong: Boolean,
    visualSpec: DesktopHomeSongCardVisualSpec,
) {
    val showsPlaceholder: Boolean = song.coverImageUri.isNullOrBlank() && song.coverArt == CoverArt.HeroLocalMusic
    if (showsPlaceholder) {
        DesktopHomeSongPlaceholderArtwork(
            isCurrentSong = isCurrentSong,
            visualSpec = visualSpec,
        )
        return
    }
    CoverArtImage(
        coverArt = song.coverArt,
        coverImageUri = song.coverImageUri,
        contentDescription = "${song.title} 封面",
        modifier =
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
    )
}

// 占位封面沿用 Figma 的浅绿/浅蓝色块节奏。
@Composable
private fun DesktopHomeSongPlaceholderArtwork(
    isCurrentSong: Boolean,
    visualSpec: DesktopHomeSongCardVisualSpec,
) {
    val placeholderIcon: ImageVector =
        when (visualSpec.artworkIconStyle) {
            DesktopHomeArtworkIconStyle.LibraryMusic -> Icons.Rounded.LibraryMusic
            DesktopHomeArtworkIconStyle.FigmaMusicNote -> DesktopHomeFigmaMusicNoteIcon
        }
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = visualSpec.artworkColor,
        shadowElevation = visualSpec.artworkShadowElevation,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = if (isCurrentSong) Color(0xFF006B5C) else Color(0xFF5E7085),
                modifier =
                    Modifier.size(
                        width = visualSpec.artworkIconWidth,
                        height = visualSpec.artworkIconHeight,
                    ),
            )
        }
    }
}

// 收藏、更多和时长保持固定可见，收藏按钮按真实收藏状态切换图标。
@Composable
internal fun DesktopHomeSongActions(
    song: Song,
    isCurrentSong: Boolean,
    isCurrentSongPlaying: Boolean,
    visualSpec: DesktopHomeSongCardVisualSpec,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val actionColor: Color = if (isCurrentSong || song.isLiked) DesktopMusicColors.PlayerRed else Color(0xFF3C4A46)
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (visualSpec.actionLeadingSpacerWidth > 0.dp) {
            if (isCurrentSongPlaying) {
                DesktopHomePlayingIndicator(
                    modifier =
                        Modifier.size(
                            width = visualSpec.actionLeadingSpacerWidth,
                            height = 16.dp,
                        ),
                )
            } else {
                Spacer(
                    modifier =
                        Modifier.size(
                            width = visualSpec.actionLeadingSpacerWidth,
                            height = 16.dp,
                        ),
                )
            }
        }
        DesktopHomeSongIconAction(
            icon = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (song.isLiked) "取消收藏" else "收藏",
            tint = actionColor,
            onClick = { onLike(song.id) },
        )
        DesktopHomeSongIconAction(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "${song.title} 更多操作",
            tint = if (isCurrentSong) DesktopMusicColors.PlayerRed else Color(0xFF3C4A46),
            onClick = { onMore(song) },
        )
    }
}

// 图标动作独立 Surface 消费点击，避免事件传给整行播放动作。
@Composable
private fun DesktopHomeSongIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(28.dp),
        shape = CircleShape,
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val DesktopHomeFigmaMusicNoteIcon: ImageVector =
    ImageVector
        .Builder(
            name = "DesktopHomeFigmaMusicNoteIcon",
            defaultWidth = 15.dp,
            defaultHeight = 22.5.dp,
            viewportWidth = 15f,
            viewportHeight = 22.5f,
        ).apply {
            path(fill = SolidColor(Color(0xFF006B5C))) {
                moveTo(5f, 22.5f)
                curveTo(3.625f, 22.5f, 2.44792f, 22.0104f, 1.46875f, 21.0312f)
                curveTo(0.489583f, 20.0521f, 0f, 18.875f, 0f, 17.5f)
                curveTo(0f, 16.125f, 0.489583f, 14.9479f, 1.46875f, 13.9688f)
                curveTo(2.44792f, 12.9896f, 3.625f, 12.5f, 5f, 12.5f)
                curveTo(5.47917f, 12.5f, 5.92188f, 12.5573f, 6.32812f, 12.6719f)
                curveTo(6.73438f, 12.7865f, 7.125f, 12.9583f, 7.5f, 13.1875f)
                verticalLineTo(0f)
                horizontalLineTo(15f)
                verticalLineTo(5f)
                horizontalLineTo(10f)
                verticalLineTo(17.5f)
                curveTo(10f, 18.875f, 9.51042f, 20.0521f, 8.53125f, 21.0312f)
                curveTo(7.55208f, 22.0104f, 6.375f, 22.5f, 5f, 22.5f)
                close()
            }
        }.build()
