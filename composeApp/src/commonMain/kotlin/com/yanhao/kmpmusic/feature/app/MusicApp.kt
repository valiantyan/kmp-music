package com.yanhao.kmpmusic.feature.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MiniPlayerPalette
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.core.theme.LocalMusicScale
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.components.SongRow
import com.yanhao.kmpmusic.feature.components.rememberMiniPlayerPalette
import com.yanhao.kmpmusic.feature.screen.AlbumDetailScreen
import com.yanhao.kmpmusic.feature.screen.ArtistDetailScreen
import com.yanhao.kmpmusic.feature.screen.FavoritesScreen
import com.yanhao.kmpmusic.feature.screen.HomeScreen
import com.yanhao.kmpmusic.feature.screen.LocalMusicScreen
import com.yanhao.kmpmusic.feature.screen.LoginScreen
import com.yanhao.kmpmusic.feature.screen.MeScreen
import com.yanhao.kmpmusic.feature.screen.MissingLibraryItemScreen
import com.yanhao.kmpmusic.feature.screen.PlayerScreen
import com.yanhao.kmpmusic.feature.screen.SearchScreen
import com.yanhao.kmpmusic.feature.screen.SettingsScreen
import kotlinx.coroutines.launch

/**
 * 普通底部 chrome 切换时长(300ms)，用于一级页和仅 mini player 页面之间的轻量移动。
 */
private const val BOTTOM_CHROME_PARTIAL_TRANSITION_MILLIS = 300

/**
 * 完全隐藏或恢复底部 chrome 的切换时长(500ms)，避免沉浸页面转场显得过快。
 */
private const val BOTTOM_CHROME_HIDDEN_TRANSITION_MILLIS = 500

/**
 * KMP Music 共享 App 入口。
 */
@Composable
fun MusicApp(
    controller: MusicAppController,
) {
    val state: MusicAppUiState = controller.uiState
    val coroutineScope = rememberCoroutineScope()
    val scanLocalMusic: () -> Unit = {
        coroutineScope.launch {
            controller.scanLocalMusic(request = LocalMusicScanRequest.Refresh)
        }
    }
    KmpMusicTheme(themeMode = state.themeMode) {
        PlatformBackHandler(
            enabled = state.canHandleSystemBack,
            onBack = { controller.handleSystemBack() },
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEFF3F5))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MusicColors.Accent.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(x = 120f, y = 110f),
                        radius = 520f,
                    ),
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            val appWidth: androidx.compose.ui.unit.Dp = if (maxWidth < MusicDimens.AppMaxWidth) {
                maxWidth
            } else {
                MusicDimens.AppMaxWidth
            }
            val visualScale: Float = (appWidth.value / MusicDimens.AppMaxWidth.value).coerceAtMost(maximumValue = 1f)
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(
                LocalMusicScale provides visualScale,
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 1f,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .width(appWidth)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(MusicColors.Accent.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(x = 135f * visualScale, y = 130f * visualScale),
                                radius = 420f * visualScale,
                            ),
                        )
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
                ) {
                    val chromeMode: AppChromeMode = state.navigationState.chromeMode
                    AppContent(
                        state = state,
                        controller = controller,
                        chromeMode = chromeMode,
                        onScanLocalMusic = scanLocalMusic,
                    )
                    BottomChrome(
                        song = state.currentSong,
                        isPlaying = state.isPlaying,
                        playbackPositionMs = state.playbackPositionMs,
                        playbackDurationMs = state.playbackDurationMs,
                        placement = chromeMode.bottomChromePlacement,
                        showsBottomNavigation = chromeMode.showsBottomNavigation,
                        rootTab = state.navigationState.rootTab,
                        onOpen = controller::openPlayer,
                        onToggle = controller::togglePlayback,
                        onPrev = { controller.moveTrack(direction = -1) },
                        onQueue = controller::openQueue,
                        onRootTab = controller::navigateToRoot,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    AppOverlays(state = state, controller = controller)
                }
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
    chromeMode: AppChromeMode,
    onScanLocalMusic: () -> Unit,
) {
    val bottomPadding: Dp = getContentBottomPadding(contentBottomSpace = chromeMode.contentBottomSpace)
    val pagePadding: PaddingValues = PaddingValues(
        start = scaledDp(MusicDimens.PagePaddingHorizontal),
        top = scaledDp(MusicDimens.PagePaddingTop),
        end = scaledDp(MusicDimens.PagePaddingHorizontal),
        bottom = bottomPadding,
    )
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(key = state.navigationState.scrollStateKey) {
        when (val secondaryScreen = state.navigationState.secondaryScreen) {
            is SecondaryScreen.LocalMusic -> LocalMusicScreen(
                songs = state.localSongs,
                albums = state.localAlbums,
                artists = state.localArtists,
                sources = state.localMusicSources,
                problems = state.localMusicProblems,
                initialSection = secondaryScreen.initialSection,
                currentSongId = state.currentSongId,
                currentPlaybackStatus = state.playbackStatus,
                onBack = controller::navigateBack,
                onSongOpen = { song: Song, queueSongs: List<Song> ->
                    controller.openSong(song = song, queueSongs = queueSongs)
                },
                onSongPlay = { song: Song, queueSongs: List<Song> ->
                    controller.playSong(song = song, queueSongs = queueSongs)
                },
                onCurrentSongToggle = controller::togglePlayback,
                onMore = controller::openMore,
                onAlbumOpen = controller::openAlbum,
                onArtistOpen = controller::openArtist,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = pagePadding,
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(pagePadding),
                ) {
                    when (secondaryScreen) {
                        null -> RootScreen(
                            state = state,
                            controller = controller,
                            onScanLocalMusic = onScanLocalMusic,
                        )
                        SecondaryScreen.Search -> SearchScreen(
                            query = state.searchQuery,
                            scope = state.searchScope,
                            result = controller.search(),
                            currentSongId = state.currentSongId,
                            currentPlaybackStatus = state.playbackStatus,
                            onBack = controller::navigateBack,
                            onQuery = controller::setSearchQuery,
                            onScope = controller::setSearchScope,
                            onSongOpen = { song: Song, queueSongs: List<Song> ->
                                controller.openSong(song = song, queueSongs = queueSongs)
                            },
                            onSongPlay = { song: Song, queueSongs: List<Song> ->
                                controller.playSong(song = song, queueSongs = queueSongs)
                            },
                            onCurrentSongToggle = controller::togglePlayback,
                            onMore = controller::openMore,
                            onAlbumOpen = controller::openAlbum,
                            onArtistOpen = controller::openArtist,
                        )
                        SecondaryScreen.Player -> state.currentSong?.let { song ->
                            PlayerScreen(
                                song = song,
                                isPlaying = state.isPlaying,
                                playbackPositionMs = state.playbackPositionMs,
                                playbackDurationMs = state.playbackDurationMs,
                                playbackMode = state.playbackMode,
                                playbackError = state.playbackError,
                                onBack = controller::navigateBack,
                                onToggle = controller::togglePlayback,
                                onPrev = { controller.moveTrack(direction = -1) },
                                onNext = { controller.moveTrack(direction = 1) },
                                onSeek = controller::seekTo,
                                onMode = controller::cyclePlaybackMode,
                                onLike = controller::toggleFavorite,
                                onQueue = controller::openQueue,
                            )
                        } ?: MissingLibraryItemScreen(
                            title = "暂无播放",
                            subtitle = "播放一首本地歌曲后会在这里显示。",
                            onBack = controller::navigateBack,
                        )
                        SecondaryScreen.AlbumDetail -> state.selectedAlbum?.let { album ->
                            AlbumDetailScreen(
                                album = album,
                                songs = state.localSongs,
                                currentSongId = state.currentSongId,
                                currentPlaybackStatus = state.playbackStatus,
                                onBack = controller::navigateBack,
                                onSongOpen = { song: Song, queueSongs: List<Song> ->
                                    controller.openSong(song = song, queueSongs = queueSongs)
                                },
                                onSongPlay = { song: Song, queueSongs: List<Song> ->
                                    controller.playSong(song = song, queueSongs = queueSongs)
                                },
                                onCurrentSongToggle = controller::togglePlayback,
                                onMore = controller::openMore,
                                onLike = controller::toggleFavorite,
                            )
                        } ?: MissingLibraryItemScreen(
                            title = "专辑不可用",
                            onBack = controller::navigateBack,
                        )
                        SecondaryScreen.ArtistDetail -> state.selectedArtist?.let { artist ->
                            ArtistDetailScreen(
                                artist = artist,
                                songs = state.localSongs,
                                albums = state.localAlbums,
                                currentSongId = state.currentSongId,
                                currentPlaybackStatus = state.playbackStatus,
                                onBack = controller::navigateBack,
                                onSongOpen = { song: Song, queueSongs: List<Song> ->
                                    controller.openSong(song = song, queueSongs = queueSongs)
                                },
                                onSongPlay = { song: Song, queueSongs: List<Song> ->
                                    controller.playSong(song = song, queueSongs = queueSongs)
                                },
                                onCurrentSongToggle = controller::togglePlayback,
                                onMore = controller::openMore,
                                onLike = controller::toggleFavorite,
                                onAlbumOpen = controller::openAlbum,
                            )
                        } ?: MissingLibraryItemScreen(
                            title = "歌手不可用",
                            onBack = controller::navigateBack,
                        )
                        SecondaryScreen.Settings -> SettingsScreen(
                            themeMode = state.themeMode,
                            onThemeMode = controller::setThemeMode,
                            onBack = controller::navigateBack,
                            onScan = onScanLocalMusic,
                            onLocalMusicSources = {
                                controller.openLocalMusic(section = LocalMusicSection.Sources)
                            },
                            onClearCache = controller::openClearCacheDialog,
                        )
                        SecondaryScreen.Login -> LoginScreen(
                            email = state.email,
                            isMailSent = state.isMailSent,
                            onEmail = controller::setEmail,
                            onSend = controller::sendLoginMail,
                            onBack = controller::navigateBack,
                        )
                        is SecondaryScreen.LocalMusic -> Unit
                    }
                }
            }
        }
    }
}

/**
 * 根据 chrome 策略计算页面底部避让空间，避免隐藏播放器后留下空白。
 */
@Composable
private fun getContentBottomPadding(contentBottomSpace: ContentBottomSpace): Dp {
    return when (contentBottomSpace) {
        ContentBottomSpace.TopLevel -> scaledDp(MusicDimens.TopLevelContentBottom)
        ContentBottomSpace.SecondaryWithMiniPlayer -> scaledDp(MusicDimens.SecondaryContentBottom)
        ContentBottomSpace.Fullscreen -> scaledDp(MusicDimens.FullscreenContentBottom)
    }
}

/**
 * 渲染一级页面。
 */
@Composable
private fun RootScreen(
    state: MusicAppUiState,
    controller: MusicAppController,
    onScanLocalMusic: () -> Unit,
) {
    when (state.navigationState.rootTab) {
        RootTab.Home -> HomeScreen(
            songs = state.homeLocalSongPreview,
            albums = state.localAlbums,
            libraryStats = state.libraryStats,
            scanState = state.scanState,
            recentSongs = state.recentSongs,
            localSongPreview = state.homeLocalSongPreview,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            onSearch = controller::openSearch,
            onScan = onScanLocalMusic,
            onLocalMusic = { controller.openLocalMusic(section = LocalMusicSection.Songs) },
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(song = song, queueSongs = queueSongs)
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onCurrentSongToggle = controller::togglePlayback,
            onMore = controller::openMore,
            onAlbumOpen = controller::openAlbum,
        )
        RootTab.Favorites -> FavoritesScreen(
            songs = state.favoriteSongs,
            albums = state.favoriteAlbums,
            artists = state.favoriteArtists,
            currentSongId = state.currentSongId,
            currentPlaybackStatus = state.playbackStatus,
            section = state.favoriteSection,
            onSection = controller::setFavoriteSection,
            onSongOpen = { song: Song, queueSongs: List<Song> ->
                controller.openSong(song = song, queueSongs = queueSongs)
            },
            onSongPlay = { song: Song, queueSongs: List<Song> ->
                controller.playSong(song = song, queueSongs = queueSongs)
            },
            onCurrentSongToggle = controller::togglePlayback,
            onMore = controller::openMore,
            onLike = controller::toggleFavorite,
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
        RootTab.Me -> MeScreen(
            albums = state.albums,
            artists = state.artists,
            libraryStats = state.libraryStats,
            favoriteCount = state.likedSongIds.size,
            onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
            onLogin = { controller.navigateToSecondary(SecondaryScreen.Login) },
            onAlbumOpen = controller::openAlbum,
            onArtistOpen = controller::openArtist,
        )
    }
}

/**
 * 全局底部 chrome 容器，mini 与 Tab 保持相对位置并整体移动。
 */
@Composable
private fun BottomChrome(
    song: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    placement: BottomChromePlacement,
    showsBottomNavigation: Boolean,
    rootTab: RootTab,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationBarHeight: Dp = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(density = this).toDp()
    }
    val stackHeight: Dp = if (song == null) {
        scaledDp(MusicDimens.BottomNavHeight)
    } else {
        scaledDp(MusicDimens.MiniPlayerHeight + MusicDimens.BottomNavHeight)
    }
    val bottomChromeTransition = updateTransition(
        targetState = placement,
        label = "BottomChromePlacement",
    )
    val stackOffset: Dp by bottomChromeTransition.animateDp(
        transitionSpec = {
            val durationMillis: Int = if (
                initialState == BottomChromePlacement.Hidden ||
                    targetState == BottomChromePlacement.Hidden
            ) {
                BOTTOM_CHROME_HIDDEN_TRANSITION_MILLIS
            } else {
                BOTTOM_CHROME_PARTIAL_TRANSITION_MILLIS
            }
            tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        },
        label = "BottomChromeOffset",
    ) { targetPlacement: BottomChromePlacement ->
        when (targetPlacement) {
            BottomChromePlacement.TopLevel -> 0.dp
            BottomChromePlacement.MiniPlayerOnly -> if (song == null) {
                stackHeight + navigationBarHeight
            } else {
                scaledDp(MusicDimens.BottomNavHeight)
            }
            BottomChromePlacement.Hidden -> stackHeight + navigationBarHeight
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(stackHeight + navigationBarHeight),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(navigationBarHeight)
                .background(MusicColors.Paper),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stackHeight)
                .clipToBounds()
                .align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stackHeight)
                    .offset(y = stackOffset),
            ) {
                if (song != null) {
                    MiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        playbackPositionMs = playbackPositionMs,
                        playbackDurationMs = playbackDurationMs,
                        onOpen = onOpen,
                        onToggle = onToggle,
                        onPrev = onPrev,
                        onQueue = onQueue,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
                BottomNavigation(
                    rootTab = rootTab,
                    isEnabled = showsBottomNavigation,
                    onRootTab = onRootTab,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * 全局迷你播放器。
 */
@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val miniPlayerPalette: MiniPlayerPalette = rememberMiniPlayerPalette(
        coverArt = song.coverArt,
        coverImageUri = song.coverImageUri,
    )
    val containerColor: Color by animateColorAsState(
        targetValue = miniPlayerPalette.containerColor,
        animationSpec = tween(durationMillis = 260),
        label = "MiniPlayerContainerColor",
    )
    val progressFraction: Float = calculateMiniPlayerProgressFraction(
        playbackPositionMs = playbackPositionMs,
        playbackDurationMs = playbackDurationMs ?: song.durationMs,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = scaledDp(MusicDimens.PagePaddingHorizontal),
                end = scaledDp(MusicDimens.PagePaddingHorizontal),
            )
            .height(scaledDp(MusicDimens.MiniPlayerHeight)),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        onClick = onOpen,
    ) {
        Box(modifier = Modifier.height(scaledDp(MusicDimens.MiniPlayerHeight))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledDp(MusicDimens.MiniPlayerHeight))
                    .padding(start = scaledDp(10.dp), top = scaledDp(8.dp), end = scaledDp(17.dp), bottom = scaledDp(7.dp)),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(weight = 1f),
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(11.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArtImage(
                        coverArt = song.coverArt,
                        coverImageUri = song.coverImageUri,
                        contentDescription = "${song.title} 封面",
                        modifier = Modifier.size(scaledDp(45.dp)).clip(RoundedCornerShape(scaledDp(8.dp))),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier.weight(weight = 1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = song.title,
                            fontSize = scaledSp(15.sp),
                            lineHeight = scaledSp(19.sp),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            color = MusicColors.Muted,
                            fontSize = scaledSp(13.sp),
                            lineHeight = scaledSp(16.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(14.dp)), verticalAlignment = Alignment.CenterVertically) {
                    MiniControlButton(onClick = onPrev) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", tint = MusicColors.Ink)
                    }
                    MiniControlButton(onClick = onToggle) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = MusicColors.Ink)
                    }
                    MiniControlButton(onClick = onQueue) {
                        Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "播放队列", tint = MusicColors.Ink)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(scaledDp(3.dp))
                    .background(MusicColors.Line.copy(alpha = 0.65f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction = progressFraction)
                    .height(scaledDp(3.dp))
                    .background(MusicColors.Accent),
            )
        }
    }
}

// 迷你播放器只展示只读进度，所有数值必须来自真实播放状态而不是视觉占位。
private fun calculateMiniPlayerProgressFraction(
    playbackPositionMs: Long,
    playbackDurationMs: Long?,
): Float {
    val safeDurationMs: Long = playbackDurationMs?.takeIf { durationMs -> durationMs > 0L } ?: return 0f
    return playbackPositionMs
        .coerceIn(minimumValue = 0L, maximumValue = safeDurationMs)
        .toFloat() / safeDurationMs.toFloat()
}

/**
 * 迷你播放器控制按钮使用固定触控区，避免图标变化造成布局跳动。
 */
@Composable
private fun MiniControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = scaledDp(28.dp), height = scaledDp(42.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 自定义底部导航项，只保留原型需要的图标和文字状态。
 */
@Composable
private fun BottomNavigationItem(
    tab: RootTab,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val itemColor: Color = if (isSelected) MusicColors.Accent else Color(0xFF7D838D)
    Column(
        modifier = Modifier
            .size(width = scaledDp(74.dp), height = scaledDp(58.dp))
            .clickable(enabled = isEnabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = when (tab) {
                RootTab.Home -> Icons.Rounded.Home
                RootTab.Favorites -> Icons.Rounded.Favorite
                RootTab.Me -> Icons.Rounded.Person
            },
            contentDescription = tab.label(),
            tint = itemColor,
            modifier = Modifier.size(scaledDp(28.dp)),
        )
        Text(
            text = tab.label(),
            color = itemColor,
            fontSize = scaledSp(12.sp),
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 只在首页、收藏、我的三个一级页面显示的底部导航。
 */
@Composable
private fun BottomNavigation(
    rootTab: RootTab,
    isEnabled: Boolean,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(scaledDp(MusicDimens.BottomNavHeight))
            .border(width = 1.dp, color = MusicColors.Line.copy(alpha = 0.86f)),
        color = MusicColors.Paper,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = scaledDp(24.dp), top = scaledDp(10.dp), end = scaledDp(24.dp), bottom = scaledDp(8.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RootTab.entries.forEach { tab ->
                BottomNavigationItem(
                    tab = tab,
                    isSelected = rootTab == tab,
                    isEnabled = isEnabled,
                    onClick = { onRootTab(tab) },
                )
            }
        }
    }
}

/**
 * 全局弹层。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AppOverlays(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    if (state.isQueueOpen) {
        ModalBottomSheet(onDismissRequest = controller::closeQueue) {
            val queueSongs: List<Song> = state.queueSongs
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(all = 21.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "queue-title") {
                    Text(text = "播放队列", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                items(
                    items = queueSongs,
                    key = { song: Song -> song.id },
                    contentType = { "queue-song" },
                ) { song: Song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onOpen = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onPlay = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        dense = true,
                    )
                }
            }
        }
    }
    state.moreSongId?.let { songId ->
        val song: Song? = state.currentSong?.takeIf { item -> item.id == songId }
            ?: state.queueSongs.firstOrNull { item -> item.id == songId }
            ?: state.localSongs.firstOrNull { item -> item.id == songId }
            ?: state.homeLocalSongPreview.firstOrNull { item -> item.id == songId }
            ?: state.favoriteSongs.firstOrNull { item -> item.id == songId }
        if (song != null) {
            ModalBottomSheet(onDismissRequest = controller::closeMore) {
                Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = song.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
    if (state.isPermissionSettingsDialogOpen) {
        AlertDialog(
            onDismissRequest = controller::closePermissionSettingsDialog,
            confirmButton = {
                Button(onClick = controller::confirmPermissionSettings) {
                    Text(text = "去设置")
                }
            },
            dismissButton = {
                Button(onClick = controller::closePermissionSettingsDialog) {
                    Text(text = "取消")
                }
            },
            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MusicColors.Accent) },
            title = { Text(text = "开启音频权限") },
            text = { Text(text = "需要在系统设置中开启音频权限，才能扫描本机歌曲。") },
        )
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
