package com.yanhao.kmpmusic.feature.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.ScanStatus
import com.yanhao.kmpmusic.feature.components.PlayingGlyph
import com.yanhao.kmpmusic.feature.components.SongRow
import com.yanhao.kmpmusic.feature.components.coverArtPainter
import com.yanhao.kmpmusic.feature.screen.AlbumDetailScreen
import com.yanhao.kmpmusic.feature.screen.ArtistDetailScreen
import com.yanhao.kmpmusic.feature.screen.FavoritesScreen
import com.yanhao.kmpmusic.feature.screen.HomeScreen
import com.yanhao.kmpmusic.feature.screen.LocalFolderScreen
import com.yanhao.kmpmusic.feature.screen.LoginScreen
import com.yanhao.kmpmusic.feature.screen.MeScreen
import com.yanhao.kmpmusic.feature.screen.PlayerScreen
import com.yanhao.kmpmusic.feature.screen.SearchScreen
import com.yanhao.kmpmusic.feature.screen.SettingsScreen

/**
 * KMP Music 共享 App 入口。
 */
@Composable
fun MusicApp(
    controller: MusicAppController = remember { MusicAppController() },
) {
    val state: MusicAppUiState = controller.uiState
    KmpMusicTheme(themeMode = state.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MusicColors.Accent.copy(alpha = 0.10f), MaterialTheme.colorScheme.background),
                    ),
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 430.dp)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                AppContent(state = state, controller = controller)
                MiniPlayer(
                    song = state.currentSong,
                    isPlaying = state.isPlaying,
                    isTopLevel = state.navigationState.isTopLevel,
                    onOpen = { controller.navigateToSecondary(SecondaryScreen.Player) },
                    onToggle = controller::togglePlayback,
                    onPrev = { controller.moveTrack(direction = -1) },
                    onQueue = controller::openQueue,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                if (state.navigationState.isTopLevel) {
                    BottomNavigation(
                        rootTab = state.navigationState.rootTab,
                        onRootTab = controller::navigateToRoot,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                AppOverlays(state = state, controller = controller)
            }
        }
    }
}

/**
 * 根据导航状态渲染页面内容，并为底部播放器和 Tab 预留空间。
 */
@Composable
private fun AppContent(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    val bottomPadding = if (state.navigationState.isTopLevel) 164.dp else 96.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 21.dp, top = 42.dp, end = 21.dp, bottom = bottomPadding),
    ) {
        when (val secondaryScreen = state.navigationState.secondaryScreen) {
            null -> RootScreen(state = state, controller = controller)
            SecondaryScreen.Search -> SearchScreen(
                query = state.searchQuery,
                scope = state.searchScope,
                result = controller.search(),
                currentSongId = state.currentSongId,
                onBack = controller::navigateBack,
                onQuery = controller::setSearchQuery,
                onScope = controller::setSearchScope,
                onSongOpen = controller::openSong,
                onSongPlay = controller::playSong,
                onMore = controller::openMore,
                onAlbumOpen = controller::openAlbum,
                onArtistOpen = controller::openArtist,
            )
            SecondaryScreen.Player -> PlayerScreen(
                song = state.currentSong,
                isPlaying = state.isPlaying,
                onBack = controller::navigateBack,
                onToggle = controller::togglePlayback,
                onPrev = { controller.moveTrack(direction = -1) },
                onNext = { controller.moveTrack(direction = 1) },
                onLike = controller::toggleFavorite,
                onQueue = controller::openQueue,
            )
            SecondaryScreen.AlbumDetail -> AlbumDetailScreen(
                album = state.selectedAlbum,
                songs = state.songs,
                currentSongId = state.currentSongId,
                onBack = controller::navigateBack,
                onSongOpen = controller::openSong,
                onSongPlay = controller::playSong,
                onMore = controller::openMore,
                onLike = controller::toggleFavorite,
            )
            SecondaryScreen.ArtistDetail -> ArtistDetailScreen(
                artist = state.selectedArtist,
                songs = state.songs,
                albums = state.albums,
                currentSongId = state.currentSongId,
                onBack = controller::navigateBack,
                onSongOpen = controller::openSong,
                onSongPlay = controller::playSong,
                onMore = controller::openMore,
                onLike = controller::toggleFavorite,
                onAlbumOpen = controller::openAlbum,
            )
            SecondaryScreen.Settings -> SettingsScreen(
                themeMode = state.themeMode,
                onThemeMode = controller::setThemeMode,
                onBack = controller::navigateBack,
                onScan = controller::openScan,
                onClearCache = controller::openClearCacheDialog,
            )
            SecondaryScreen.Login -> LoginScreen(
                email = state.email,
                isMailSent = state.isMailSent,
                onEmail = controller::setEmail,
                onSend = controller::sendLoginMail,
                onBack = controller::navigateBack,
            )
            SecondaryScreen.LocalFolder -> LocalFolderScreen(
                songs = state.songs,
                currentSongId = state.currentSongId,
                onBack = controller::navigateBack,
                onSongOpen = controller::openSong,
                onSongPlay = controller::playSong,
                onMore = controller::openMore,
            )
        }
    }
}

/**
 * 渲染一级页面。
 */
@Composable
private fun RootScreen(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    when (state.navigationState.rootTab) {
        RootTab.Home -> HomeScreen(
            songs = state.songs,
            albums = state.albums,
            currentSongId = state.currentSongId,
            onSearch = { controller.navigateToSecondary(SecondaryScreen.Search) },
            onScan = controller::openScan,
            onLocalFolder = { controller.navigateToSecondary(SecondaryScreen.LocalFolder) },
            onSongOpen = controller::openSong,
            onSongPlay = controller::playSong,
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
        )
        RootTab.Favorites -> FavoritesScreen(
            songs = state.songs,
            albums = state.albums,
            artists = state.artists,
            currentSongId = state.currentSongId,
            section = state.favoriteSection,
            onSection = controller::setFavoriteSection,
            onSongOpen = controller::openSong,
            onSongPlay = controller::playSong,
            onMore = controller::openMore,
            onLike = controller::toggleFavorite,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
        RootTab.Me -> MeScreen(
            albums = state.albums,
            artists = state.artists,
            onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
            onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
    }
}

/**
 * 全局迷你播放器，二级页面时贴齐底部，一级页面时位于 Tab 上方。
 */
@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isTopLevel: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = if (isTopLevel) 78.dp else 0.dp
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 21.dp, end = 21.dp, bottom = bottomPadding)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(18.dp),
        color = MusicColors.Paper,
        tonalElevation = 4.dp,
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = coverArtPainter(song.coverArt),
                contentDescription = "${song.title} 封面",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = song.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = song.artist, color = MusicColors.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                PlayingGlyph(color = MusicColors.Accent)
            }
            IconButton(onClick = onPrev) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首") }
            IconButton(onClick = onToggle) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放") }
            IconButton(onClick = onQueue) { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "播放队列") }
        }
    }
}

/**
 * 只在首页、收藏、我的三个一级页面显示的底部导航。
 */
@Composable
private fun BottomNavigation(
    rootTab: RootTab,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        RootTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = rootTab == tab,
                onClick = { onRootTab(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            RootTab.Home -> Icons.Rounded.Home
                            RootTab.Favorites -> Icons.Rounded.Favorite
                            RootTab.Me -> Icons.Rounded.Person
                        },
                        contentDescription = tab.label(),
                    )
                },
                label = { Text(text = tab.label()) },
            )
        }
    }
}

/**
 * 全局弹层和提示。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppOverlays(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    if (state.isQueueOpen) {
        ModalBottomSheet(onDismissRequest = controller::closeQueue) {
            Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "播放队列", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                state.queueSongs.forEach { song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == state.currentSongId,
                        onOpen = controller::playSong,
                        onPlay = controller::playSong,
                        onMore = controller::openMore,
                        dense = true,
                    )
                }
            }
        }
    }
    if (state.scanStatus != ScanStatus.Idle) {
        AlertDialog(
            onDismissRequest = controller::closeScan,
            confirmButton = {
                Button(onClick = controller::advanceScan) {
                    Text(text = if (state.scanStatus == ScanStatus.Done) "回到音乐库" else "完成扫描")
                }
            },
            title = { Text(text = if (state.scanStatus == ScanStatus.Done) "扫描完成" else "正在扫描本地音乐") },
            text = { Text(text = if (state.scanStatus == ScanStatus.Done) "新增 24 首歌曲，已更新 3 张专辑。" else "正在读取 /Music/KMP Library，已识别 1,248 首歌曲。") },
        )
    }
    state.moreSongId?.let { songId ->
        val song: Song? = state.songs.firstOrNull { item -> item.id == songId }
        if (song != null) {
            ModalBottomSheet(onDismissRequest = controller::closeMore) {
                Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(text = song.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    BottomSheetAction("加入收藏", Icons.Rounded.Favorite) { controller.toggleFavorite(song.id) }
                    BottomSheetAction("查看专辑", Icons.Rounded.LibraryMusic) { controller.openAlbumFromSong(song) }
                    BottomSheetAction("查看歌手", Icons.Rounded.Person) { controller.openArtistFromSong(song) }
                }
            }
        }
    }
    if (state.isClearCacheDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closeClearCacheDialog,
            confirmButton = {
                Button(onClick = controller::confirmClearCache) {
                    Text(text = "清理")
                }
            },
            dismissButton = {
                Button(onClick = controller::closeClearCacheDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MusicColors.Danger) },
            title = { Text(text = "清理 428 MB 缓存？") },
            text = { Text(text = "只会删除封面缓存和临时文件，本地歌曲不会受到影响。") },
        )
    }
    state.toast?.let { toast ->
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(18.dp),
            color = MusicColors.Ink,
            onClick = controller::clearToast,
        ) {
            Text(text = toast, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = MusicColors.Paper)
        }
    }
}

/**
 * 更多操作行。
 */
@Composable
private fun BottomSheetAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MusicColors.Soft, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Text(text = label, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.MoreHoriz, contentDescription = null, tint = MusicColors.Muted)
        }
    }
}

/**
 * 根 Tab 中文名。
 */
private fun RootTab.label(): String {
    return when (this) {
        RootTab.Home -> "首页"
        RootTab.Favorites -> "收藏"
        RootTab.Me -> "我的"
    }
}
