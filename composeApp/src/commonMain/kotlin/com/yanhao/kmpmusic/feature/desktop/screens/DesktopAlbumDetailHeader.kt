package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopAlbumDetailTokens

/** 专辑头部按 Figma 显示专辑级封面、元信息和两种完整队列播放入口。 */
@Composable
internal fun DesktopAlbumDetailHeader(
    displayModel: DesktopAlbumDetailDisplayModel,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = DesktopAlbumDetailTokens.ContentPadding,
                    top = 40.dp,
                    end = DesktopAlbumDetailTokens.ContentPadding,
                    bottom = 32.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        DesktopAlbumDetailHeroArtwork(displayModel = displayModel)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayModel.title,
                color = DesktopAlbumDetailTokens.Title,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = displayModel.artist,
                color = DesktopAlbumDetailTokens.SupportingText,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DesktopAlbumDetailActionButton(
                    text = displayModel.playAllLabel,
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    enabled = displayModel.isPlaybackEnabled,
                    onClick = onPlayAll,
                )
                DesktopAlbumDetailActionButton(
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

/** 大封面只读取专辑投影字段，保持专辑列表、收藏页和详情页的选择结果一致。 */
@Composable
private fun DesktopAlbumDetailHeroArtwork(displayModel: DesktopAlbumDetailDisplayModel) {
    val artworkShape: RoundedCornerShape = RoundedCornerShape(16.dp)
    val artworkModifier: Modifier =
        Modifier
            .size(DesktopAlbumDetailTokens.HeroArtworkSize)
            .shadow(
                elevation = 16.dp,
                shape = artworkShape,
                clip = false,
            ).clip(artworkShape)
    val album = displayModel.album
    if (album != null) {
        CoverArtImage(
            coverArt = album.coverArt,
            coverImageUri = album.coverImageUri,
            contentDescription = "${album.title} 专辑封面",
            modifier = artworkModifier,
            contentScale = ContentScale.Crop,
        )
        return
    }
    CoverArtImage(
        coverArt = CoverArt.HeroLocalMusic,
        coverImageUri = null,
        contentDescription = "专辑不可用",
        modifier = artworkModifier,
        contentScale = ContentScale.Crop,
    )
}

/** 页面专用按钮保持 Figma 的圆角、尺寸和强调层级，不改变收藏页既有视觉。 */
@Composable
private fun DesktopAlbumDetailActionButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val buttonContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = if (isPrimary) 32.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    resolveDesktopAlbumDetailActionContentColor(
                        isPrimary = isPrimary,
                        enabled = enabled,
                    ),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                color =
                    resolveDesktopAlbumDetailActionContentColor(
                        isPrimary = isPrimary,
                        enabled = enabled,
                    ),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    val buttonModifier: Modifier =
        Modifier
            .height(46.dp)
            .then(
                if (isPrimary && enabled) {
                    Modifier.shadow(elevation = 4.dp, shape = CircleShape)
                } else {
                    Modifier
                },
            )
    if (enabled) {
        Surface(
            modifier = buttonModifier,
            shape = CircleShape,
            color = resolveDesktopAlbumDetailActionBackground(isPrimary = isPrimary, enabled = true),
            onClick = onClick,
            content = buttonContent,
        )
        return
    }
    Surface(
        modifier = buttonModifier,
        shape = CircleShape,
        color = resolveDesktopAlbumDetailActionBackground(isPrimary = isPrimary, enabled = false),
        content = buttonContent,
    )
}

/** 禁用态仅降低可用性提示，不改变 Figma 按钮占位和列宽。 */
private fun resolveDesktopAlbumDetailActionBackground(
    isPrimary: Boolean,
    enabled: Boolean,
): Color =
    when {
        isPrimary && enabled -> DesktopAlbumDetailTokens.Accent
        isPrimary -> DesktopAlbumDetailTokens.Accent.copy(alpha = 0.35f)
        enabled -> DesktopAlbumDetailTokens.SecondaryAction
        else -> DesktopAlbumDetailTokens.SecondaryAction.copy(alpha = 0.55f)
    }

/** 主按钮使用白字，次按钮和禁用态保留与页面背景足够区分的深色文字。 */
private fun resolveDesktopAlbumDetailActionContentColor(
    isPrimary: Boolean,
    enabled: Boolean,
): Color {
    if (!enabled) {
        return DesktopAlbumDetailTokens.SupportingText.copy(alpha = 0.55f)
    }
    return if (isPrimary) Color.White else DesktopAlbumDetailTokens.Title
}
