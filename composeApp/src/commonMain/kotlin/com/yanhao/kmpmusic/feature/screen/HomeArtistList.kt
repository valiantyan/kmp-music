package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
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
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 渲染首页歌手列表，数据来源与本地音乐歌手分段保持一致。
 */
internal fun LazyListScope.homeArtistListItems(
    artists: List<Artist>,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScan: () -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        item(key = "home-empty-artists") {
            HomeEmptyArtistsCard(
                scanState = scanState,
                discoveryPlatform = discoveryPlatform,
                onScan = onScan,
            )
        }
        return
    }
    items(
        items = artists,
        key = { artist: Artist -> artist.id },
        contentType = { "home-artist" },
    ) { artist: Artist ->
        HomeArtistRow(
            artist = artist,
            onArtistOpen = onArtistOpen,
        )
        Spacer(modifier = Modifier.height(height = homeArtistListGap))
    }
}

// 歌手行只表达进入歌手详情，不承载播放或当前播放高亮语义。
@Composable
private fun HomeArtistRow(
    artist: Artist,
    onArtistOpen: (Artist) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = homeArtistRowHeight)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(size = 12.dp),
        color = Color.White,
        onClick = { onArtistOpen(artist) },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = homeArtistRowVerticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeArtistAvatar(artist = artist)
            HomeArtistText(
                artist = artist,
                modifier = Modifier.weight(weight = 1f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "进入 ${artist.name} 歌手页",
                modifier = Modifier.size(width = 18.dp, height = 18.dp),
                tint = homeArtistChevronColor,
            )
        }
    }
}

// 头像外圈保留 Figma 的浅绿色圆形描边，图片本身仍使用扫描封面优先。
@Composable
private fun HomeArtistAvatar(artist: Artist) {
    Box(
        modifier = Modifier
            .size(size = homeArtistAvatarOuterSize)
            .border(
                width = homeArtistAvatarBorderWidth,
                color = homeArtistAvatarBorderColor,
                shape = CircleShape,
            )
            .padding(all = homeArtistAvatarInset),
    ) {
        CoverArtImage(
            coverArt = artist.coverArt,
            coverImageUri = artist.coverImageUri,
            contentDescription = "${artist.name} 歌手头像",
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

// 文案按 Figma 使用姓名加歌曲/专辑统计，统计来自 [Artist] 聚合事实。
@Composable
private fun HomeArtistText(
    artist: Artist,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = artist.name,
            color = Color.Black,
            fontSize = homeArtistNameFontSize,
            lineHeight = homeArtistNameLineHeight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${artist.songCount} 首歌曲 · ${artist.albumCount} 张专辑",
            color = Color(0xFF3D4947),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
