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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MusicColors.Muted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onSettings != null) {
                RoundIconButton(onClick = onSettings) {
                    Text(text = "⚙", fontSize = 22.sp)
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
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MusicColors.Soft,
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
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (meta != null) {
                Text(
                    text = meta,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    color = MusicColors.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = "$actionLabel ›",
                modifier = Modifier.clickable(onClick = onAction),
                color = MusicColors.Muted,
                fontSize = 16.sp,
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
        modifier = Modifier.width(14.dp).height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.width(3.dp).height(11.dp).background(color))
        Box(modifier = Modifier.width(3.dp).height(7.dp).background(color))
        Box(modifier = Modifier.width(3.dp).height(13.dp).background(color))
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(weight = 1f).clickable { onOpen(song) },
            horizontalArrangement = Arrangement.spacedBy(if (dense) 12.dp else 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = coverArtPainter(song.coverArt),
                contentDescription = "${song.title} 封面",
                modifier = Modifier.size(if (dense) 52.dp else 62.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = song.title,
                    color = activeColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${song.artist} · ${song.album}",
                    color = secondaryColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayingGlyph(color = if (isCurrentSong) MusicColors.PlayingRed else MusicColors.Accent)
                    if (isCurrentSong) {
                        Text(text = "播放中", color = MusicColors.PlayingRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(text = song.duration, color = secondaryColor, fontSize = 14.sp)
                }
            }
        }
        if (onLike != null) {
            IconButton(onClick = { onLike(song.id) }) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (song.isLiked) "取消收藏" else "收藏",
                    tint = if (song.isLiked) MusicColors.Accent else MusicColors.Muted,
                )
            }
        }
        IconButton(onClick = { onPlay(song) }) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "播放 ${song.title}")
        }
        IconButton(onClick = { onMore(song) }) {
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
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Image(
            painter = coverArtPainter(album.coverArt),
            contentDescription = "${album.title} 专辑封面",
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(text = album.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = album.artist, color = MusicColors.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = "${album.songCount} 首", color = MusicColors.Muted, fontSize = 14.sp)
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
            modifier = Modifier.size(58.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(text = artist.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "${artist.tag} · ${artist.songCount} 首", color = MusicColors.Muted, fontSize = 13.sp)
        }
        Text(text = "›", color = MusicColors.Muted, fontSize = 24.sp)
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
    Button(
        modifier = modifier.height(46.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MusicColors.Accent),
        shape = RoundedCornerShape(20.dp),
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.ExtraBold)
    }
}
