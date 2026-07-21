package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.PlayingGlyph

/**
 * 收藏页歌曲卡片，按 Figma 展示封面、歌曲信息、收藏和更多入口。
 */
@Composable
internal fun FavoritesSongRow(
    song: Song,
    isCurrentSong: Boolean,
    queueSongs: List<Song>,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = favoritesSongRowHeight),
        shape = RoundedCornerShape(size = favoritesSongRowRadius),
        color = Color.White,
        shadowElevation = 2.dp,
        onClick = { onSongPlay(song, queueSongs) },
    ) {
        Row(
            modifier = Modifier.padding(all = favoritesSongRowPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoritesSongCover(
                song = song,
                isCurrentSong = isCurrentSong,
            )
            FavoritesSongText(
                song = song,
                isCurrentSong = isCurrentSong,
                modifier = Modifier.weight(weight = 1f),
            )
            FavoritesSongActions(
                song = song,
                isCurrentSong = isCurrentSong,
                onMore = onMore,
                onLike = onLike,
            )
        }
    }
}

// 当前播放辅助标识贴在封面右下角，普通状态保持 Figma 的纯封面样式。
@Composable
private fun FavoritesSongCover(
    song: Song,
    isCurrentSong: Boolean,
) {
    Box(modifier = Modifier.size(size = favoritesSongCoverSize)) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(shape = RoundedCornerShape(size = favoritesSongCoverRadius)),
            contentScale = ContentScale.Crop,
        )
        if (isCurrentSong) {
            Box(
                modifier =
                    Modifier
                        .align(alignment = Alignment.BottomEnd)
                        .clip(shape = CircleShape)
                        .padding(all = 4.dp),
            ) {
                PlayingGlyph(color = MusicColors.PlayingRed)
            }
        }
    }
}

// 歌曲文字按 Figma 的 14sp 层级渲染；当前播放保留全局红色同步规则。
@Composable
private fun FavoritesSongText(
    song: Song,
    isCurrentSong: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor: Color = if (isCurrentSong) MusicColors.PlayingRed else favoritesTextColor
    val metaColor: Color = if (isCurrentSong) MusicColors.PlayingRed else favoritesMetaColor
    Column(modifier = modifier) {
        Text(
            text = song.title,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            color = metaColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 收藏页行尾只保留 Figma 中的心形和更多操作。
@Composable
private fun FavoritesSongActions(
    song: Song,
    isCurrentSong: Boolean,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(size = favoritesSongActionSize),
            onClick = { onLike(song.id) },
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "取消收藏 ${song.title}",
                modifier = Modifier.size(width = 20.dp, height = 20.dp),
                tint = if (isCurrentSong) MusicColors.PlayingRed else favoritesTitleColor,
            )
        }
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
