package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
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
import coil3.compose.AsyncImage
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.SectionTitle
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 我的页收藏摘要最多展示 3 张，完整内容通过“查看”进入，避免窄屏被数据数量挤坏。
 */
private const val FAVORITE_ALBUM_PREVIEW_COUNT = 3

/**
 * 我的页，提供本地资料、收藏资产和常听歌手摘要。
 */
@Composable
fun MeScreen(
    albums: List<Album>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ProfileSummary()
        MetricRow(
            libraryStats = libraryStats,
            favoriteCount = favoriteCount,
        )
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle(title = "我的收藏", actionLabel = "查看", onAction = { albums.firstOrNull()?.let(onAlbumOpen) })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    albums.take(FAVORITE_ALBUM_PREVIEW_COUNT).forEach { album ->
                        Column(
                            modifier = Modifier.weight(weight = 1f).clickable { onAlbumOpen(album) },
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            CoverArtImage(
                                coverArt = album.coverArt,
                                coverImageUri = album.coverImageUri,
                                contentDescription = "${album.title} 封面",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(ratio = 1f)
                                    .clip(RoundedCornerShape(11.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = album.title,
                                color = MusicColors.Muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp)) {
                SectionTitle(title = "常听歌手", actionLabel = "更多", onAction = { artists.firstOrNull()?.let(onArtistOpen) })
                artists.take(3).forEach { artist -> ArtistRow(artist = artist, onOpen = onArtistOpen) }
            }
        }
    }
}

/**
 * 我的页基础资料区，按 Figma 展示固定头像、静态编辑徽标和用户文案。
 */
@Composable
private fun ProfileSummary() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileAvatar()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "高保真听众", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "音乐是我的灵魂", color = MusicColors.Muted, fontSize = 14.sp)
        }
    }
}

// 头像编辑能力尚未实现，徽标只表达 Figma 静态视觉，不暴露交互入口。
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun ProfileAvatar() {
    Box(
        modifier = Modifier
            .size(78.dp)
            .border(
                width = 2.dp,
                color = homeAccentColor,
                shape = CircleShape,
            )
            .padding(3.dp),
    ) {
        AsyncImage(
            model = Res.getUri("drawable/me_profile_avatar.jpg"),
            contentDescription = "个人头像",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp),
            shape = CircleShape,
            color = homeAccentColor,
            shadowElevation = 2.dp,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.padding(5.dp),
                tint = Color.White,
            )
        }
    }
}

/**
 * 我的页统计指标。
 */
@Composable
private fun MetricRow(
    libraryStats: LibraryStats,
    favoriteCount: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            "本地专辑" to libraryStats.albumCount.toString(),
            "歌手" to libraryStats.artistCount.toString(),
            "收藏" to favoriteCount.toString(),
        ).forEach { item ->
            Surface(
                modifier = Modifier.weight(weight = 1f),
                shape = RoundedCornerShape(18.dp),
                color = MusicColors.AccentSoft,
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.second, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = item.first, modifier = Modifier.padding(horizontal = 8.dp), color = MusicColors.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
