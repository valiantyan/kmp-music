package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.userMessage

// 歌曲信息保持居中，长标题用省略避免挤压进度和控制区。
@Composable
internal fun PlayerMetadata(
    song: Song,
    playbackError: PlaybackError?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = scaledDp(6.dp)),
    ) {
        Text(
            text = song.title,
            color = MusicColors.Ink,
            fontSize = scaledSp(40.sp),
            lineHeight = scaledSp(46.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = buildPlayerSubtitle(song = song),
            color = MusicColors.AccentDeep,
            fontSize = scaledSp(18.sp),
            lineHeight = scaledSp(24.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (playbackError != null) {
            Text(
                text = playbackError.userMessage(songTitle = song.title),
                color = MaterialTheme.colorScheme.error,
                fontSize = scaledSp(13.sp),
                lineHeight = scaledSp(16.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// 底部辅助动作复刻 Figma 的语义区，倍速点击交给全局底部面板承接。
@Composable
internal fun PlayerAuxiliaryActions(
    song: Song,
    playbackSpeed: PlaybackSpeed,
    onLike: (String) -> Unit,
    onSpeed: () -> Unit,
) {
    val playbackSpeedVisual: PlayerPlaybackSpeedVisual =
        buildPlayerPlaybackSpeedVisual(playbackSpeed = playbackSpeed)
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = scaledDp(126.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAuxiliaryAction(
            icon = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = "喜欢",
            tint = if (song.isLiked) MusicColors.AccentDeep else MusicColors.Ink,
            onClick = { onLike(song.id) },
        )
        PlayerAuxiliaryAction(
            icon = Icons.Rounded.Speed,
            label = "倍速",
            tint = playbackSpeedVisual.tint,
            badgeText = playbackSpeedVisual.badgeText,
            onClick = onSpeed,
        )
    }
}

// 单个辅助动作固定宽度，避免图标或文案状态变化造成底部跳动。
@Composable
private fun PlayerAuxiliaryAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    badgeText: String? = null,
    onClick: (() -> Unit)?,
) {
    val actionModifier: Modifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(onClick = onClick)
        }
    Column(
        modifier = actionModifier.width(width = scaledDp(PlayerAuxiliaryActionDesignSpec.width)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = scaledDp(4.dp)),
    ) {
        Box(
            modifier = Modifier.size(size = scaledDp(PlayerAuxiliaryActionDesignSpec.iconSlotSize)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(size = scaledDp(PlayerAuxiliaryActionDesignSpec.iconSize)),
            )
            if (badgeText != null) {
                PlayerAuxiliaryActionBadge(
                    text = badgeText,
                    modifier =
                        Modifier
                            .align(alignment = Alignment.TopEnd)
                            .offset(
                                x = scaledDp(PlayerAuxiliaryActionDesignSpec.badgeOffsetX),
                                y = scaledDp(PlayerAuxiliaryActionDesignSpec.badgeOffsetY),
                            ),
                )
            }
        }
        Text(
            text = label,
            color = tint,
            fontSize = scaledSp(14.sp),
            lineHeight = scaledSp(18.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

// 角标固定在图标右上，显示当前已生效的全局倍速。
@Composable
private fun PlayerAuxiliaryActionBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(height = scaledDp(PlayerAuxiliaryActionDesignSpec.badgeHeight))
                .widthIn(min = scaledDp(PlayerAuxiliaryActionDesignSpec.badgeMinWidth)),
        shape = RoundedCornerShape(size = scaledDp(999.dp)),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = scaledDp(1.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = MusicColors.AccentDeep,
                fontSize = scaledSp(9.sp),
                lineHeight = scaledSp(11.sp),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(horizontal = scaledDp(5.dp)),
            )
        }
    }
}

/**
 * 播放页倍速入口的视觉状态，默认 [PlaybackSpeed.Normal] 也是有效设置，不能显示成禁用色。
 *
 * @property tint 图标与文字使用的有效状态颜色。
 * @property badgeText 右上角展示的当前倍速文本。
 */
internal data class PlayerPlaybackSpeedVisual(
    val tint: Color,
    val badgeText: String,
)

// 汇总倍速入口的显示规则，避免默认 1.0 被误判为未激活状态。
internal fun buildPlayerPlaybackSpeedVisual(playbackSpeed: PlaybackSpeed): PlayerPlaybackSpeedVisual =
    PlayerPlaybackSpeedVisual(
        tint = MusicColors.AccentDeep,
        badgeText = "${playbackSpeed.label}x",
    )

// 播放页辅助动作尺寸集中管理，确保角标出现时不挤压文字布局。
internal object PlayerAuxiliaryActionDesignSpec {
    /** 辅助动作占位宽度。 */
    val width: Dp = 64.dp

    /** 图标和角标共用的稳定槽位。 */
    val iconSlotSize: Dp = 38.dp

    /** 主图标尺寸。 */
    val iconSize: Dp = 26.dp

    /** 当前倍速角标最小宽度。 */
    val badgeMinWidth: Dp = 36.dp

    /** 当前倍速角标高度。 */
    val badgeHeight: Dp = 16.dp

    /** 角标向右覆盖图标边缘，形成右上角标识。 */
    val badgeOffsetX: Dp = 9.dp

    /** 角标略微上移，避免压住速度图标主体。 */
    val badgeOffsetY: Dp = (-3).dp
}

// 歌手和专辑共用一行，既满足 PRD 信息量，又不破坏 Figma 的轻量层级。
private fun buildPlayerSubtitle(song: Song): String {
    if (song.album.isBlank()) {
        return song.artist
    }
    return "${song.artist} · ${song.album}"
}
