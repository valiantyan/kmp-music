package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
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
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.components.coverArtPainter

private const val RECENT_SIDEBAR_SONG_COUNT = 4

/**
 * 桌面首页左侧资料库侧栏，承接效果图中的本地音乐库入口和最近播放摘要。
 */
@Composable
fun DesktopLibrarySidebar(
    libraryStats: LibraryStats,
    recentSongs: List<Song>,
    onSearch: () -> Unit,
    onSection: (LocalMusicSection) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(DesktopMusicDimens.LibrarySidebarWidth)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.62f))
            .padding(horizontal = 22.dp, vertical = 28.dp),
    ) {
        DesktopLibrarySidebarHeader(onAdd = { onSection(LocalMusicSection.Sources) })
        Spacer(modifier = Modifier.height(20.dp))
        DesktopLibrarySearch(onClick = onSearch)
        Spacer(modifier = Modifier.height(18.dp))
        DesktopLibraryTabs(onSection = onSection)
        Spacer(modifier = Modifier.height(28.dp))
        DesktopLibraryDatabaseStats(
            libraryStats = libraryStats,
            onSection = onSection,
        )
        Spacer(modifier = Modifier.height(30.dp))
        DesktopLibraryRecentSongs(
            songs = recentSongs.take(n = RECENT_SIDEBAR_SONG_COUNT),
            onSongPlay = onSongPlay,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

// 侧栏标题保留下拉语义，后续可扩展为多资料库切换。
@Composable
private fun DesktopLibrarySidebarHeader(onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "本地音乐库",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.SidebarTitle,
                fontWeight = FontWeight.ExtraBold,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = DesktopMusicColors.Ink,
                modifier = Modifier.size(16.dp),
            )
        }
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = Color.Transparent,
            onClick = onAdd,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "添加本地音乐来源",
                    tint = DesktopMusicColors.MutedStrong,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// 侧栏内搜索入口与顶部全局搜索共用动作，避免产生两套搜索状态。
@Composable
private fun DesktopLibrarySearch(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = DesktopMusicColors.Soft,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = DesktopMusicColors.MutedStrong,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "搜索本地库",
                color = DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.SidebarBody,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// 效果图中的资料库分段全部跳到同一个二级页，由分段参数决定初始内容。
@Composable
private fun DesktopLibraryTabs(onSection: (LocalMusicSection) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopLibraryTab(text = "全部", isActive = true) { onSection(LocalMusicSection.Songs) }
        DesktopLibraryTab(text = "歌曲", isActive = false) { onSection(LocalMusicSection.Songs) }
        DesktopLibraryTab(text = "专辑", isActive = false) { onSection(LocalMusicSection.Albums) }
        DesktopLibraryTab(text = "歌手", isActive = false) { onSection(LocalMusicSection.Artists) }
        DesktopLibraryTab(text = "来源", isActive = false) { onSection(LocalMusicSection.Sources) }
    }
}

// 单个分段用稳定高度，防止不同字数导致侧栏跳动。
@Composable
private fun DesktopLibraryTab(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(9.dp),
        color = if (isActive) DesktopMusicColors.AccentSoft else Color.Transparent,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (isActive) DesktopMusicColors.AccentDeep else DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.SidebarBody,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// 资料库统计对应效果图里的“资料库”块。
@Composable
private fun DesktopLibraryDatabaseStats(
    libraryStats: LibraryStats,
    onSection: (LocalMusicSection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "资料库",
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.SidebarBody,
            fontWeight = FontWeight.ExtraBold,
        )
        DesktopLibraryStatRow(
            icon = { Text(text = "◷", color = DesktopMusicColors.MutedStrong, fontSize = DesktopMusicType.StatTitle) },
            label = "最近添加",
            value = libraryStats.songCount.toString(),
            onClick = { onSection(LocalMusicSection.Songs) },
        )
        DesktopLibraryStatRow(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = DesktopMusicColors.MutedStrong,
                    modifier = Modifier.size(19.dp),
                )
            },
            label = "播放历史",
            value = libraryStats.albumCount.toString(),
            onClick = { onSection(LocalMusicSection.Albums) },
        )
        DesktopLibraryStatRow(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = DesktopMusicColors.MutedStrong,
                    modifier = Modifier.size(19.dp),
                )
            },
            label = "本地文件夹",
            value = libraryStats.artistCount.toString(),
            onClick = { onSection(LocalMusicSection.Sources) },
        )
    }
}

// 统计行右侧数值直接使用曲库数据，避免侧栏出现静态假数据。
@Composable
private fun DesktopLibraryStatRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = DesktopMusicColors.Ink,
            fontSize = DesktopMusicType.SidebarBody,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.SidebarBody,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// 最近播放列表在没有历史时保持留白说明，避免侧栏显得缺块。
@Composable
private fun DesktopLibraryRecentSongs(
    songs: List<Song>,
    onSongPlay: (Song, List<Song>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最近播放",
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.SidebarBody,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "清空",
                color = DesktopMusicColors.AccentDeep,
                fontSize = DesktopMusicType.SidebarBody,
                fontWeight = FontWeight.Bold,
            )
        }
        if (songs.isEmpty()) {
            Text(
                text = "播放后会显示最近听过的歌曲。",
                color = DesktopMusicColors.Muted,
                fontSize = DesktopMusicType.SidebarBody,
            )
            return
        }
        songs.forEach { song: Song ->
            DesktopLibraryRecentSongRow(
                song = song,
                onClick = { onSongPlay(song, songs) },
            )
        }
    }
}

// 最近播放歌曲保留封面、标题、歌手和时间，贴近效果图的信息密度。
@Composable
private fun DesktopLibraryRecentSongRow(
    song: Song,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = coverArtPainter(song.coverArt),
            contentDescription = "${song.title} 封面",
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = song.title,
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.SidebarBody,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                color = DesktopMusicColors.MutedStrong,
                fontSize = DesktopMusicType.TableHeader,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.lastPlayed,
            color = DesktopMusicColors.MutedStrong,
            fontSize = DesktopMusicType.TableHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
