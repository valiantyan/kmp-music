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
import androidx.compose.material.icons.rounded.FavoriteBorder
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

// 歌曲行使用收藏页同款卡片，保持首页自己的播放队列。
@Composable
internal fun HomeSongRow(
    song: Song,
    isCurrentSong: Boolean,
    queueSongs: List<Song>,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
) {
    val rowStyle: HomeSongRowStyle = resolveHomeSongRowStyle(
        isCurrentSong = isCurrentSong,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = favoritesSongRowHeight)
            .padding(horizontal = favoritesHorizontalPadding),
        shape = RoundedCornerShape(size = favoritesSongRowRadius),
        color = rowStyle.containerColor,
        border = rowStyle.border,
        shadowElevation = rowStyle.shadowElevation,
        onClick = { onSongPlay(song, queueSongs) },
    ) {
        Row(
            modifier = Modifier.padding(all = favoritesSongRowPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeSongCover(
                song = song,
                showsPlaybackBadge = rowStyle.showsCoverPlaybackBadge,
            )
            HomeSongText(
                song = song,
                textColor = rowStyle.textColor,
                metaColor = rowStyle.metaColor,
                modifier = Modifier.weight(weight = 1f),
            )
            HomeSongActions(
                song = song,
                isCurrentSong = isCurrentSong,
                onMore = onMore,
                onLike = onLike,
            )
        }
    }
}

// 封面跟收藏页保持同款尺寸，当前歌曲用角标补充播放态。
@Composable
private fun HomeSongCover(
    song: Song,
    showsPlaybackBadge: Boolean,
) {
    Box(modifier = Modifier.size(size = favoritesSongCoverSize)) {
        CoverArtImage(
            coverArt = song.coverArt,
            coverImageUri = song.coverImageUri,
            contentDescription = "${song.title} 封面",
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = RoundedCornerShape(size = favoritesSongCoverRadius)),
            contentScale = ContentScale.Crop,
        )
        if (showsPlaybackBadge) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.BottomEnd)
                    .clip(shape = CircleShape)
                    .padding(all = 4.dp),
            ) {
                PlayingGlyph(color = MusicColors.PlayingRed)
            }
        }
    }
}

// 歌曲标题和歌手名跟收藏页层级一致，当前歌曲由 [HomeSongRowStyle] 切到红色。
@Composable
private fun HomeSongText(
    song: Song,
    textColor: Color,
    metaColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
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

// 首页行尾跟收藏页一致提供收藏和更多入口。
@Composable
private fun HomeSongActions(
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
                imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (song.isLiked) "取消收藏 ${song.title}" else "收藏 ${song.title}",
                modifier = Modifier.size(width = 20.dp, height = 20.dp),
                tint = resolveHomeFavoriteIconTint(
                    song = song,
                    isCurrentSong = isCurrentSong,
                ),
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
