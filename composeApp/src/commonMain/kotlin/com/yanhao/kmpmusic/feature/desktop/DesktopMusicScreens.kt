package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.app.FavoriteSection
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

private const val HOME_ALBUM_PREVIEW_COUNT = 4
private const val FAVORITE_ALBUM_PREVIEW_COUNT = 4
private const val ARTIST_STRIP_COUNT = 4

/**
 * 桌面二级页统一从这里分发，避免 [DesktopMusicApp] 复制具体页面路由。
 */
@Composable
fun DesktopSecondaryScreen(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    when (state.navigationState.secondaryScreen) {
        SecondaryScreen.Search -> DesktopSearchScreen(
            query = state.searchQuery,
            resultSongs = controller.search().songs,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onQuery = controller::setSearchQuery,
            onBack = controller::navigateBack,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
        )
        SecondaryScreen.Player -> DesktopPlayerDetailScreen(
            song = state.currentSong,
            isPlaying = state.isPlaying,
            onBack = controller::navigateBack,
            onToggle = controller::togglePlayback,
            onPrev = { controller.moveTrack(direction = -1) },
            onNext = { controller.moveTrack(direction = 1) },
            onQueue = controller::openQueue,
        )
        SecondaryScreen.AlbumDetail -> DesktopAlbumDetailScreen(
            album = state.selectedAlbum,
            songs = state.localSongs,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
        )
        SecondaryScreen.ArtistDetail -> DesktopArtistDetailScreen(
            artist = state.selectedArtist,
            songs = state.localSongs,
            albums = state.localAlbums,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
        )
        SecondaryScreen.Settings -> DesktopSettingsScreen(
            themeMode = state.themeMode,
            onThemeMode = controller::setThemeMode,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onLocalMusicSources = {
                controller.openLocalMusic(section = LocalMusicSection.Sources)
            },
            onClearCache = controller::openClearCacheDialog,
        )
        SecondaryScreen.Login -> DesktopLoginScreen(
            email = state.email,
            isMailSent = state.isMailSent,
            onEmail = controller::setEmail,
            onSend = controller::sendLoginMail,
            onBack = controller::navigateBack,
        )
        is SecondaryScreen.LocalMusic -> DesktopLocalSourcesScreen(
            songs = state.localSongs,
            sources = state.localMusicSources,
            problems = state.localMusicProblems,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onBack = controller::navigateBack,
            onScan = onScanLocalMusic,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(
                    song = song,
                    queueSongs = queueSongs,
                )
            },
            onMore = controller::openMore,
        )
        null -> DesktopEmptyStateScreen(
            title = "本地音乐",
            subtitle = "桌面首页",
        )
    }
}

@Composable
// 本地音乐首页只展示播放历史反推的最近专辑，避免把全库误标成最近播放。
fun DesktopLocalMusicRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    recentSongs: List<Song>,
    libraryStats: LibraryStats,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onScan: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onBrowseAlbums: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onAlbumOpen: (Album) -> Unit,
) {
    val playAllLabel: String = rootPlayAllLabel(
        songs = songs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    val recentAlbums: List<Album> = buildRecentAlbums(
        recentSongs = recentSongs,
        albums = albums,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "本地音乐",
            eyebrow = "已扫描 ${libraryStats.songCount} 首歌曲，${libraryStats.albumCount} 张专辑，${libraryStats.artistCount} 位歌手",
        ) {
            DesktopSecondaryButton(
                text = "重新扫描",
                icon = DesktopScanIcon,
                onClick = onScan,
            )
            DesktopMoreButton(onClick = onBrowseLibrary)
        }
        DesktopThreeStatRow(
            firstTitle = "歌曲",
            firstValue = libraryStats.songCount.toString(),
            secondTitle = "专辑",
            secondValue = libraryStats.albumCount.toString(),
            thirdTitle = "歌手",
            thirdValue = libraryStats.artistCount.toString(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        DesktopToolbar(
            playAllLabel = playAllLabel,
            sortLabel = "排序：最近添加",
            onPlayAll = {
                playOrToggleRootCollection(
                    songs = songs,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                )
            },
        )
        Spacer(modifier = Modifier.height(14.dp))
        DesktopSongTable(
            songs = songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = onCurrentSongToggle,
            onMore = onMore,
        )
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
                message = "播放后会在这里显示最近听过的专辑。",
            )
        }
    }
}

@Composable
fun DesktopFavoritesRootScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    section: FavoriteSection,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSection: (FavoriteSection) -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onMore: (Song) -> Unit,
    onLike: (String) -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    val likedSongs: List<Song> = songs.filter { song: Song -> song.isLiked }
    val playAllLabel: String = rootPlayAllLabel(
        songs = likedSongs,
        currentSongId = currentSongId,
        currentPlaybackStatus = currentPlaybackStatus,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "收藏",
            eyebrow = "喜欢的音乐都在这里",
        ) {
            DesktopSegmentedControl(
                labels = listOf("歌曲", "专辑", "歌手"),
                selectedIndex = section.ordinal,
                onSelect = { index: Int -> onSection(FavoriteSection.entries[index]) },
            )
        }
        DesktopThreeStatRow(
            firstTitle = "收藏歌曲",
            firstValue = likedSongs.size.toString(),
            secondTitle = "收藏专辑",
            secondValue = albums.size.toString(),
            thirdTitle = "收藏歌手",
            thirdValue = artists.size.toString(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        when (section) {
            FavoriteSection.Songs -> {
                DesktopToolbar(
                    playAllLabel = playAllLabel,
                    sortLabel = "排序：最近收藏",
                    onPlayAll = {
                        playOrToggleRootCollection(
                            songs = likedSongs,
                            currentSongId = currentSongId,
                            currentPlaybackStatus = currentPlaybackStatus,
                            onSongPlay = onSongPlay,
                            onCurrentSongToggle = onCurrentSongToggle,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopSongTable(
                    songs = likedSongs,
                    currentSongId = currentSongId,
                    currentPlaybackStatus = currentPlaybackStatus,
                    showFavoriteColumn = true,
                    trailingDateLabel = "收藏时间",
                    onSongOpen = onSongOpen,
                    onSongPlay = onSongPlay,
                    onCurrentSongToggle = onCurrentSongToggle,
                    onMore = onMore,
                    onLike = onLike,
                )
                if (albums.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DesktopSectionHeader(
                        title = "收藏的专辑",
                        actionLabel = "查看全部",
                        onAction = { onSection(FavoriteSection.Albums) },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DesktopAlbumGrid(
                        albums = albums.take(FAVORITE_ALBUM_PREVIEW_COUNT),
                        onAlbumOpen = onAlbumOpen,
                    )
                }
            }
            FavoriteSection.Albums -> {
                DesktopSectionHeader(
                    title = "收藏的专辑",
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopAlbumGrid(
                    albums = albums,
                    onAlbumOpen = onAlbumOpen,
                )
            }
            FavoriteSection.Artists -> {
                DesktopSectionHeader(
                    title = "收藏的歌手",
                )
                Spacer(modifier = Modifier.height(14.dp))
                DesktopArtistStrip(
                    artists = artists.take(ARTIST_STRIP_COUNT),
                    onArtistOpen = onArtistOpen,
                )
                if (artists.size > ARTIST_STRIP_COUNT) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        artists.drop(ARTIST_STRIP_COUNT).forEach { artist: Artist ->
                            DesktopContentRow(
                                icon = Icons.Rounded.Person,
                                title = artist.name,
                                subtitle = "${artist.songCount} 首歌曲 · ${artist.tag}",
                                actionLabel = "打开",
                                onClick = { onArtistOpen(artist) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
// 我的页汇总账号、收藏与最近播放概览，最近播放必须来自真实播放历史。
fun DesktopMeRootScreen(
    albums: List<Album>,
    recentSongs: List<Song>,
    artists: List<Artist>,
    libraryStats: LibraryStats,
    favoriteCount: Int,
    onLogin: () -> Unit,
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
            eyebrow = "本地资料与同步状态",
        )
        DesktopProfilePanel(
            title = "登录音乐账号",
            description = "使用 Supabase 同步收藏、播放记录和多端资料，让你的音乐在所有设备上保持一致。",
            buttonText = "立即登录",
            onClick = onLogin,
        )
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
 * 搜索页沿用桌面表格承载结果，保持和根页一致的交互密度。
 */
@Composable
private fun DesktopSearchScreen(
    query: String,
    resultSongs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val eyebrow: String = if (query.isBlank()) {
        "搜索歌曲、专辑、歌手"
    } else {
        "搜索结果：$query"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "搜索",
            eyebrow = eyebrow,
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopContentRow(
            icon = Icons.Rounded.Search,
            title = if (query.isBlank()) "开始搜索" else query,
            subtitle = "点击上方标题栏继续输入关键词",
            actionLabel = if (query.isBlank()) null else "清空",
            onClick = if (query.isBlank()) null else ({ onQuery("") }),
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopSongTable(
            songs = resultSongs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

/**
 * 播放详情页只暴露桌面版必要控制，真实播放逻辑仍由控制器负责。
 */
@Composable
private fun DesktopPlayerDetailScreen(
    song: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = song?.title ?: "暂无播放",
            eyebrow = song?.artist ?: "播放一首本地歌曲后会显示详情",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = if (isPlaying) "暂停" else "播放", onClick = onToggle)
            DesktopPrimaryButton(text = "队列", onClick = onQueue)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DesktopPrimaryButton(text = "上一首", onClick = onPrev)
            DesktopPrimaryButton(text = "下一首", onClick = onNext)
        }
    }
}

/**
 * 专辑详情直接基于当前共享曲库过滤，避免再维护桌面专属数据投影。
 */
@Composable
private fun DesktopAlbumDetailScreen(
    album: Album?,
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val albumSongs: List<Song> = album?.let { selectedAlbum: Album ->
        songs.filter { song: Song -> song.album == selectedAlbum.title }
    }.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = album?.title ?: "专辑不可用",
            eyebrow = album?.artist ?: "没有找到专辑信息",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(
                text = "▶ 播放全部",
                onClick = {
                    albumSongs.firstOrNull()?.let { song: Song ->
                        onSongPlay(song, albumSongs)
                    }
                },
            )
        }
        DesktopSongTable(
            songs = albumSongs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

/**
 * 歌手详情页复用桌面表格与统计文案，减少重复布局。
 */
@Composable
private fun DesktopArtistDetailScreen(
    artist: Artist?,
    songs: List<Song>,
    albums: List<Album>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    val artistSongs: List<Song> = artist?.let { selectedArtist: Artist ->
        songs.filter { song: Song -> song.artist == selectedArtist.name }
    }.orEmpty()
    val artistAlbumCount: Int = artist?.let { selectedArtist: Artist ->
        albums.count { album: Album -> album.artist == selectedArtist.name }
    } ?: 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = artist?.name ?: "歌手不可用",
            eyebrow = "歌曲 ${artistSongs.size} 首，专辑 $artistAlbumCount 张",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopSongTable(
            songs = artistSongs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

/**
 * 设置页只暴露当前桌面端已实现的偏好与维护动作。
 */
@Composable
private fun DesktopSettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onLocalMusicSources: () -> Unit,
    onClearCache: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "设置",
            eyebrow = "播放、扫描与显示偏好",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        DesktopSegmentedControl(
            labels = ThemeMode.entries.map { themeEntry: ThemeMode -> themeEntry.name },
            selectedIndex = ThemeMode.entries.indexOf(themeMode),
            onSelect = { index: Int -> onThemeMode(ThemeMode.entries[index]) },
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopPrimaryButton(text = "管理本地文件夹", onClick = onLocalMusicSources)
        Spacer(modifier = Modifier.height(12.dp))
        DesktopPrimaryButton(text = "重新扫描", onClick = onScan)
        Spacer(modifier = Modifier.height(12.dp))
        DesktopPrimaryButton(text = "清理缓存", onClick = onClearCache)
    }
}

/**
 * 登录页当前仅承接邮箱登录入口，不复制移动端整套表单样式。
 */
@Composable
private fun DesktopLoginScreen(
    email: String,
    isMailSent: Boolean,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "登录音乐账号",
            eyebrow = if (isMailSent) "登录邮件已发送" else "使用邮箱接收魔法链接",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "发送登录邮件", onClick = onSend)
        }
        DesktopContentRow(
            icon = DesktopContentRowSyncIcon,
            title = email.ifBlank { "未填写邮箱" },
            subtitle = "当前桌面入口沿用共享登录状态，后续可在这里补完整输入体验。",
            actionLabel = if (email.isBlank()) null else "清空",
            onClick = if (email.isBlank()) null else ({ onEmail("") }),
        )
    }
}

/**
 * 本地来源页先复用桌面表格展示扫描结果，并通过页头暴露扫描入口。
 */
@Composable
private fun DesktopLocalSourcesScreen(
    songs: List<Song>,
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onSongOpen: (Song, List<Song>) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "本地文件夹",
            eyebrow = "来源 ${sources.size} 个，问题 ${problems.size} 个",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "重新扫描", onClick = onScan)
        }
        DesktopSongTable(
            songs = songs,
            currentSongId = currentSongId,
            currentPlaybackStatus = currentPlaybackStatus,
            showFavoriteColumn = false,
            trailingDateLabel = "添加时间",
            onSongOpen = onSongOpen,
            onSongPlay = onSongPlay,
            onCurrentSongToggle = {},
            onMore = onMore,
        )
    }
}

@Composable
fun DesktopEmptyStateScreen(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = title,
            eyebrow = subtitle,
        )
    }
}

@Composable
private fun DesktopThreeStatRow(
    firstTitle: String,
    firstValue: String,
    secondTitle: String,
    secondValue: String,
    thirdTitle: String,
    thirdValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DesktopStatCard(
            icon = "♫",
            title = firstTitle,
            value = firstValue,
            modifier = Modifier.weight(1f),
        )
        DesktopStatCard(
            icon = "●",
            title = secondTitle,
            value = secondValue,
            modifier = Modifier.weight(1f),
        )
        DesktopStatCard(
            icon = "♟",
            title = thirdTitle,
            value = thirdValue,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun rootPlayAllLabel(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
): String {
    val containsCurrentSong: Boolean = songs.any { song: Song -> song.id == currentSongId }
    if (!containsCurrentSong) {
        return "播放全部"
    }
    return when (currentPlaybackStatus) {
        PlaybackStatus.Playing,
        PlaybackStatus.Buffering,
        PlaybackStatus.Loading,
        -> "暂停播放"
        PlaybackStatus.Paused,
        PlaybackStatus.Ended,
        PlaybackStatus.Idle,
        PlaybackStatus.Error,
        -> "继续播放"
    }
}

private fun playOrToggleRootCollection(
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
    onSongPlay: (Song, List<Song>) -> Unit,
    onCurrentSongToggle: () -> Unit,
) {
    val containsCurrentSong: Boolean = songs.any { song: Song -> song.id == currentSongId }
    if (containsCurrentSong && currentPlaybackStatus != PlaybackStatus.Error) {
        onCurrentSongToggle()
        return
    }
    songs.firstOrNull()?.let { song: Song -> onSongPlay(song, songs) }
}

/**
 * 根据最近播放歌曲反推最近播放的专辑，按歌曲顺序保留首次出现的专辑。
 */
private fun buildRecentAlbums(
    recentSongs: List<Song>,
    albums: List<Album>,
): List<Album> {
    val albumsByTitle: Map<String, List<Album>> = albums.groupBy { album ->
        normalizeDesktopLookupKey(album.title)
    }
    return recentSongs.mapNotNull { song: Song ->
        val normalizedAlbumTitle: String = normalizeDesktopLookupKey(song.album)
        val normalizedArtistName: String = normalizeDesktopLookupKey(song.artist)
        val titleMatches: List<Album> = albumsByTitle[normalizedAlbumTitle].orEmpty()
        titleMatches.firstOrNull { album ->
            normalizeDesktopLookupKey(album.artist) == normalizedArtistName
        } ?: titleMatches.singleOrNull()
    }
        .distinctBy { album -> album.id }
        .take(HOME_ALBUM_PREVIEW_COUNT)
}

private fun buildFrequentArtists(
    recentSongs: List<Song>,
    artists: List<Artist>,
): List<Artist> {
    if (recentSongs.isEmpty()) {
        return emptyList()
    }
    data class RecentArtistAccumulator(
        val name: String,
        val recentCount: Int,
        val firstRecentIndex: Int,
        val coverArt: com.yanhao.kmpmusic.domain.model.CoverArt,
    )

    val artistsByNormalizedName: Map<String, Artist> = artists.associateBy { artist ->
        normalizeDesktopLookupKey(artist.name)
    }
    val recentArtistStats: Map<String, RecentArtistAccumulator> =
        recentSongs.withIndex().fold(mutableMapOf()) { acc, indexedSong ->
            val normalizedArtistName: String = normalizeDesktopLookupKey(indexedSong.value.artist)
            val existing: RecentArtistAccumulator? = acc[normalizedArtistName]
            acc[normalizedArtistName] = if (existing == null) {
                RecentArtistAccumulator(
                    name = indexedSong.value.artist,
                    recentCount = 1,
                    firstRecentIndex = indexedSong.index,
                    coverArt = indexedSong.value.coverArt,
                )
            } else {
                existing.copy(recentCount = existing.recentCount + 1)
            }
            acc
        }
    return recentArtistStats.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, RecentArtistAccumulator>> { entry ->
                entry.value.recentCount
            }.thenBy { entry ->
                entry.value.firstRecentIndex
            },
        )
        .map { entry ->
            val recentArtist: RecentArtistAccumulator = entry.value
            artistsByNormalizedName[entry.key]?.copy(songCount = recentArtist.recentCount)
                ?: Artist(
                    id = "artist:${entry.key}",
                    name = recentArtist.name,
                    songCount = recentArtist.recentCount,
                    coverArt = recentArtist.coverArt,
                    tag = "最近播放",
                )
        }
}

private fun normalizeDesktopLookupKey(value: String): String {
    return value.trim().lowercase()
}

/**
 * 最近播放区为空时的轻提示，避免用全库内容冒充最近播放。
 */
@Composable
private fun DesktopSectionEmptyMessage(
    message: String,
) {
    androidx.compose.material3.Text(
        text = message,
        color = DesktopMusicColors.MutedStrong,
        fontSize = DesktopMusicType.Eyebrow,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
