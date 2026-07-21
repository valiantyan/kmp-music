package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.PlaybackError
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

// 底部辅助动作复刻 Figma 的语义区，倍速暂时只作为视觉占位。
@Composable
internal fun PlayerAuxiliaryActions(
    song: Song,
    onLike: (String) -> Unit,
) {
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
            tint = MusicColors.Muted,
            onClick = null,
        )
    }
}

// 单个辅助动作固定宽度，避免图标或文案状态变化造成底部跳动。
@Composable
private fun PlayerAuxiliaryAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)?,
) {
    val actionModifier: Modifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(onClick = onClick)
        }
    Column(
        modifier = actionModifier.width(width = scaledDp(64.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = scaledDp(4.dp)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(size = scaledDp(26.dp)),
        )
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

// 歌手和专辑共用一行，既满足 PRD 信息量，又不破坏 Figma 的轻量层级。
private fun buildPlayerSubtitle(song: Song): String {
    if (song.album.isBlank()) {
        return song.artist
    }
    return "${song.artist} · ${song.album}"
}
