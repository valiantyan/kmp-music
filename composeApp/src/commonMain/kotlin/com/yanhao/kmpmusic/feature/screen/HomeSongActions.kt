package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Song

// 首页行尾跟收藏页一致提供收藏和更多入口。
@Composable
internal fun HomeSongActions(
    song: Song,
    isCurrentSong: Boolean,
    onMore: ((Song) -> Unit)?,
    onLike: ((String) -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onLike != null) {
            IconButton(
                modifier = Modifier.size(size = favoritesSongActionSize),
                onClick = { onLike(song.id) },
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (song.isLiked) "取消收藏 ${song.title}" else "收藏 ${song.title}",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = resolveHomeFavoriteIconTint(
                        song = song,
                        isCurrentSong = isCurrentSong,
                    ),
                )
            }
        }
        if (onMore != null) {
            IconButton(
                modifier = Modifier.size(size = favoritesSongActionSize),
                onClick = { onMore(song) },
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "${song.title} 更多操作",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = favoritesMutedIconColor,
                )
            }
        }
    }
}

// 未收藏歌曲使用弱化心形，避免把当前播放态误表达成已收藏。
private fun resolveHomeFavoriteIconTint(
    song: Song,
    isCurrentSong: Boolean,
): Color {
    if (!song.isLiked) {
        return favoritesMutedIconColor
    }
    if (isCurrentSong) {
        return MusicColors.PlayingRed
    }
    return favoritesTitleColor
}
