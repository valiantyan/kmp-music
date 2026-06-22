package com.yanhao.kmpmusic.feature.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 页面头部组件，统一一级和二级页面标题节奏。
 */
@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            if (onBack != null) {
                RoundIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Spacer(modifier = Modifier.height(scaledDp(10.dp)))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = scaledSp(34.sp),
                    lineHeight = scaledSp(38.sp),
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = scaledDp(9.dp)),
                    color = MusicColors.Muted,
                    fontSize = scaledSp(16.sp),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = scaledDp(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(scaledDp(10.dp)),
        ) {
            if (onSettings != null) {
                RoundIconButton(onClick = onSettings) {
                    Text(text = "⚙", fontSize = scaledSp(22.sp))
                }
            }
            if (onSearch != null) {
                RoundIconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "搜索")
                }
            }
        }
    }
}

/**
 * 圆形图标按钮，复刻原型头部和歌曲操作按钮。
 */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.size(scaledDp(MusicDimens.HeaderActionSize)),
        shape = CircleShape,
        color = MusicColors.Soft.copy(alpha = 0.94f),
        onClick = onClick,
        content = {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        },
    )
}

/**
 * 分区标题组件。
 */
@Composable
fun SectionTitle(
    title: String,
    meta: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.alignBy(LastBaseline),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = scaledSp(21.sp),
                    lineHeight = scaledSp(25.sp),
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (meta != null) {
                Text(
                    text = meta,
                    modifier = Modifier
                        .padding(start = scaledDp(8.dp))
                        .alignBy(LastBaseline),
                    color = MusicColors.Muted,
                    fontSize = scaledSp(15.sp),
                    lineHeight = scaledSp(18.sp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = "$actionLabel  ›",
                modifier = Modifier.clickable(onClick = onAction),
                color = Color(0xFF8D939D),
                fontSize = scaledSp(16.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 当前播放等化器标识，避免只靠红色表达状态。
 */
@Composable
fun PlayingGlyph(color: Color = MusicColors.Accent) {
    Row(
        modifier = Modifier.width(scaledDp(14.dp)).height(scaledDp(13.dp)),
        horizontalArrangement = Arrangement.spacedBy(scaledDp(2.dp)),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.width(scaledDp(3.dp)).height(scaledDp(13.dp)).background(color))
        Box(modifier = Modifier.width(scaledDp(3.dp)).height(scaledDp(8.dp)).background(color))
        Box(modifier = Modifier.width(scaledDp(3.dp)).height(scaledDp(13.dp)).background(color))
    }
}

/**
 * 歌曲行组件，所有列表共享当前播放红色规则。
 */
@Composable
fun SongRow(
    song: Song,
    isCurrentSong: Boolean,
    onOpen: (Song) -> Unit,
    onPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onLike: ((String) -> Unit)? = null,
    dense: Boolean = false,
) {
    val activeColor: Color = if (isCurrentSong) MusicColors.PlayingRed else MaterialTheme.colorScheme.onBackground
    val secondaryColor: Color = if (isCurrentSong) MusicColors.PlayingRed else MusicColors.Muted
    val coverSize = scaledDp(if (dense) MusicDimens.DenseSongCoverSize else MusicDimens.SongCoverSize)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(scaledDp(if (dense) 9.dp else 12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(weight = 1f).clickable { onOpen(song) },
            horizontalArrangement = Arrangement.spacedBy(scaledDp(if (dense) 12.dp else 15.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = coverArtPainter(song.coverArt),
                contentDescription = "${song.title} 封面",
                modifier = Modifier
                    .size(coverSize)
                    .shadow(elevation = scaledDp(10.dp), shape = RoundedCornerShape(scaledDp(8.dp)), clip = false)
                    .clip(RoundedCornerShape(scaledDp(8.dp))),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(scaledDp(4.dp))) {
                Text(
                    text = song.title,
                    color = activeColor,
                    fontSize = scaledSp(17.sp),
                    lineHeight = scaledSp(20.sp),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${song.artist} · ${song.album}",
                    color = secondaryColor,
                    fontSize = scaledSp(14.sp),
                    lineHeight = scaledSp(17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(7.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayingGlyph(color = if (isCurrentSong) MusicColors.PlayingRed else MusicColors.Accent)
                    if (isCurrentSong) {
                        Text(text = "播放中", color = MusicColors.PlayingRed, fontSize = scaledSp(12.sp), lineHeight = scaledSp(14.sp), fontWeight = FontWeight.ExtraBold)
                    }
                    Text(text = song.duration, color = secondaryColor, fontSize = scaledSp(14.sp), lineHeight = scaledSp(16.sp))
                }
            }
        }
        if (onLike != null) {
            IconButton(
                modifier = Modifier.size(scaledDp(42.dp)),
                onClick = { onLike(song.id) },
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (song.isLiked) "取消收藏" else "收藏",
                    tint = if (song.isLiked) MusicColors.Accent else MusicColors.Muted,
                )
            }
        }
        Surface(
            modifier = Modifier.size(scaledDp(42.dp)),
            shape = CircleShape,
            color = MusicColors.Soft,
            onClick = { onPlay(song) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放 ${song.title}", tint = MusicColors.Ink)
            }
        }
        IconButton(
            modifier = Modifier.size(scaledDp(42.dp)),
            onClick = { onMore(song) },
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "${song.title} 更多操作", tint = MusicColors.Muted)
        }
    }
}

/**
 * 专辑卡片组件。
 */
@Composable
fun AlbumCard(
    album: Album,
    onOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable { onOpen(album) },
    ) {
        Image(
            painter = coverArtPainter(album.coverArt),
            contentDescription = "${album.title} 专辑封面",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1f)
                .shadow(elevation = scaledDp(12.dp), shape = RoundedCornerShape(scaledDp(MusicDimens.AlbumRadius)), clip = false)
                .clip(RoundedCornerShape(scaledDp(MusicDimens.AlbumRadius))),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(scaledDp(MusicDimens.AlbumTextTopGap)))
        Column(verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.AlbumTextLineGap))) {
            Text(
                text = album.title,
                fontSize = scaledSp(13.sp),
                lineHeight = scaledSp(16.sp),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artist,
                color = MusicColors.Muted,
                fontSize = scaledSp(13.sp),
                lineHeight = scaledSp(16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${album.songCount} 首",
                color = MusicColors.Muted,
                fontSize = scaledSp(13.sp),
                lineHeight = scaledSp(16.sp),
                maxLines = 1,
            )
        }
    }
}

/**
 * 歌手行组件。
 */
@Composable
fun ArtistRow(
    artist: Artist,
    onOpen: (Artist) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(artist) }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = coverArtPainter(artist.coverArt),
            contentDescription = "${artist.name} 图片",
            modifier = Modifier.size(scaledDp(58.dp)).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(text = artist.name, fontSize = scaledSp(16.sp), fontWeight = FontWeight.Bold)
            Text(text = "${artist.tag} · ${artist.songCount} 首", color = MusicColors.Muted, fontSize = scaledSp(13.sp))
        }
        Text(text = "›", color = MusicColors.Muted, fontSize = scaledSp(24.sp))
    }
}

/**
 * 主按钮组件。
 */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(scaledDp(46.dp)),
        shape = RoundedCornerShape(scaledDp(20.dp)),
        color = Color.Transparent,
        shadowElevation = scaledDp(8.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .height(scaledDp(46.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MusicColors.Accent, MusicColors.AccentDeep),
                    ),
                    shape = RoundedCornerShape(scaledDp(20.dp)),
                )
                .padding(horizontal = scaledDp(20.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(scaledDp(8.dp)))
            Text(text = text, color = Color.White, fontSize = scaledSp(16.sp), fontWeight = FontWeight.ExtraBold)
        }
    }
}
