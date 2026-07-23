package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopArtistDetailTokens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

/** Figma 歌手详情表头固定五列，曲目、时长和行尾动作不会随窗口或名称变化而错位。 */
@Composable
internal fun DesktopArtistDetailTableHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = DesktopArtistDetailTokens.ContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopArtistDetailHeaderText(text = "#", modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        DesktopArtistDetailHeaderText(text = "标题", modifier = Modifier.weight(1f))
        DesktopArtistDetailHeaderText(text = "专辑", modifier = Modifier.weight(1f))
        DesktopArtistDetailHeaderText(
            text = "时长",
            modifier = Modifier.width(DesktopArtistDetailTokens.DurationColumnWidth),
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.width(DesktopArtistDetailTokens.ActionColumnWidth))
    }
}

/** 表头文案统一使用紧凑的辅助文字层级，避免夺走 hero 的视觉焦点。 */
@Composable
private fun DesktopArtistDetailHeaderText(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = DesktopArtistDetailTokens.SupportingText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
        maxLines = 1,
    )
}

/** 曲目行复用 Desktop 收藏页的整行播放与独立操作热区，同时匹配 Figma 的双行标题列。 */
@Composable
internal fun DesktopArtistDetailSongRow(
    index: Int,
    song: Song,
    songs: List<Song>,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val titleColor: Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopArtistDetailTokens.Title
    val metaColor: Color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopArtistDetailTokens.SupportingText
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isHovered: Boolean by interactionSource.collectIsHoveredAsState()
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesktopArtistDetailTokens.ContentPadding, vertical = 2.dp)
                .height(64.dp)
                .hoverable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (shouldToggleDesktopHomeSongCardPlayback(isCurrentSong = isCurrentSong)) {
                            onCurrentSongToggle()
                        } else {
                            onSongPlay(song, songs)
                        }
                    },
                ),
        shape = RoundedCornerShape(12.dp),
        color =
            resolveDesktopArtistDetailSongRowColor(
                isCurrentSong = isCurrentSong,
                isHovered = isHovered,
                isPressed = isPressed,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DesktopArtistDetailTrackIndex(index = index, isCurrentSong = isCurrentSong, isPlaying = isPlaying)
            DesktopArtistDetailTrackTitle(song = song, titleColor = titleColor, metaColor = metaColor, modifier = Modifier.weight(1f))
            Text(
                text = song.album,
                modifier = Modifier.weight(1f),
                color = metaColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.duration,
                modifier = Modifier.width(DesktopArtistDetailTokens.DurationColumnWidth),
                color = metaColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.width(DesktopArtistDetailTokens.ActionColumnWidth),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopArtistDetailIconAction(
                    icon = resolveDesktopFavoritesLikeIcon(isLiked = song.isLiked),
                    contentDescription = if (song.isLiked) "取消收藏 ${song.title}" else "收藏 ${song.title}",
                    onClick = { onLike(song.id) },
                )
                DesktopArtistDetailIconAction(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = "${song.title} 更多操作",
                    onClick = { onMore(song) },
                )
            }
        }
    }
}

/** 悬停和按下仅改变浅底色，不使用缩放或动画，以保留桌面表格的稳定密度。 */
private fun resolveDesktopArtistDetailSongRowColor(
    isCurrentSong: Boolean,
    isHovered: Boolean,
    isPressed: Boolean,
): Color =
    when {
        isCurrentSong && isPressed -> DesktopArtistDetailTokens.ActiveRowPressed
        isCurrentSong && isHovered -> DesktopArtistDetailTokens.ActiveRowHover
        isCurrentSong -> DesktopArtistDetailTokens.ActiveRow
        isPressed -> DesktopArtistDetailTokens.RowPressed
        isHovered -> DesktopArtistDetailTokens.RowHover
        else -> Color.Transparent
    }

/** 当前歌曲播放时在序号槽位显示等高器，暂停后仍以红色序号保持当前项识别。 */
@Composable
private fun DesktopArtistDetailTrackIndex(
    index: Int,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
) {
    Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        if (shouldShowDesktopHomePlayingIndicator(isCurrentSong = isCurrentSong, isPlaying = isPlaying)) {
            DesktopHomePlayingIndicator(modifier = Modifier.size(width = 20.dp, height = 16.dp))
            return@Box
        }
        Text(
            text = (index + 1).toString().padStart(length = 2, padChar = '0'),
            color = if (isCurrentSong) DesktopMusicColors.PlayerRed else DesktopArtistDetailTokens.SupportingText,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** 标题列固定 40dp 封面和两行文案，长文本只在各自列内截断。 */
@Composable
private fun DesktopArtistDetailTrackTitle(
    song: Song,
    titleColor: Color,
    metaColor: Color,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = titleColor,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                color = metaColor,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 收藏和更多使用独立圆形热区，防止点击动作冒泡为整行播放。 */
@Composable
private fun DesktopArtistDetailIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = DesktopArtistDetailTokens.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 空态仍放在表头后，保留与非空列表一致的页面骨架。 */
@Composable
internal fun DesktopArtistDetailEmptyState(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = DesktopArtistDetailTokens.SupportingText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}
