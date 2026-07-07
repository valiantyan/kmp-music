package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
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
import coil3.compose.AsyncImage
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAlbumGrid
import com.yanhao.kmpmusic.feature.desktop.components.DesktopArtistStrip
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRow
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRowFavoritesIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRowFolderIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRowSyncIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import com.yanhao.kmpmusic.feature.desktop.components.DesktopStatCard
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val ARTIST_STRIP_COUNT = 4

/**
 * 我的页汇总个人资料、收藏与最近播放概览，最近播放必须来自真实播放历史。
 */
@Composable
fun DesktopMeRootScreen(
    albums: List<Album>,
    recentSongs: List<Song>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onFavorites: () -> Unit,
    onFolders: () -> Unit,
    onSettings: () -> Unit,
    onBrowseAlbums: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val recentAlbums: List<Album> = buildRecentAlbums(
        recentSongs = recentSongs,
        albums = albums,
    )
    val frequentArtists: List<Artist> = buildFrequentArtists(
        recentSongs = recentSongs,
        artists = artists,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "我的",
            eyebrow = "个人中心与本地音乐资产",
        )
        DesktopProfileHeader()
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DesktopStatCard(
                icon = "●",
                title = "本地专辑",
                value = libraryStats.albumCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♟",
                title = "歌手",
                value = libraryStats.artistCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♥",
                title = "收藏",
                value = favoriteCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DesktopStatCard(
                icon = "♫",
                title = "最近播放",
                value = recentSongs.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DesktopContentRow(
                icon = DesktopContentRowFavoritesIcon,
                title = "我的收藏",
                subtitle = "查看收藏的歌曲、专辑和歌手",
                actionLabel = "查看全部",
                onClick = onFavorites,
            )
            DesktopContentRow(
                icon = Icons.Rounded.Person,
                title = "常听歌手",
                subtitle = "你常听的歌手",
                actionLabel = null,
                onClick = null,
                extraContent = {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (frequentArtists.isNotEmpty()) {
                        DesktopArtistStrip(
                            artists = frequentArtists.take(ARTIST_STRIP_COUNT),
                            onArtistOpen = onArtistOpen,
                        )
                    } else {
                        DesktopSectionEmptyMessage(
                            message = "播放后会在这里显示你最近常听的歌手。",
                        )
                    }
                },
            )
            DesktopContentRow(
                icon = DesktopContentRowFolderIcon,
                title = "本地文件夹",
                subtitle = "管理你的本地音乐文件与目录",
                actionLabel = "管理",
                onClick = onFolders,
            )
            DesktopContentRow(
                icon = DesktopContentRowSyncIcon,
                title = "同步与备份",
                subtitle = "同步状态、备份与恢复选项",
                actionLabel = "设置",
                onClick = onSettings,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        DesktopSectionHeader(
            title = "最近播放的专辑",
            actionLabel = "查看全部",
            onAction = onBrowseAlbums,
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (recentAlbums.isNotEmpty()) {
            DesktopAlbumGrid(
                albums = recentAlbums,
                onAlbumOpen = onAlbumOpen,
            )
        } else {
            DesktopSectionEmptyMessage(
                message = "还没有最近播放的专辑，先播放一些音乐吧。",
            )
        }
    }
}

/**
 * 桌面“我的”页个人资料头只承载静态资料，不重新暴露旧登录入口。
 */
@Composable
private fun DesktopProfileHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopProfileAvatar()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "高保真听众",
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.PageTitle,
                    lineHeight = DesktopMusicType.PageTitle,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "音乐是我的灵魂",
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Eyebrow,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// 头像来自 Figma 静态资源，桌面端只做宽屏资料头展示。
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun DesktopProfileAvatar() {
    Surface(
        modifier = Modifier.size(104.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.82f),
        border = BorderStroke(width = 3.dp, color = DesktopMusicColors.Accent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        ) {
            AsyncImage(
                model = Res.getUri("drawable/me_profile_avatar.jpg"),
                contentDescription = "个人头像",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
