package com.yanhao.kmpmusic.feature.app.navigation

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.hasSameAlbumTitle
import com.yanhao.kmpmusic.domain.model.hasSameArtistName
import com.yanhao.kmpmusic.feature.app.HomeContentSection
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer

/**
 * 内容导航工作流控制器，集中管理需要曲库预热的内容页导航规则。
 */
class ContentNavigationController(
    // 复用现有曲库同步器，避免在导航层重复维护歌曲、专辑和歌手投影规则。
    private val libraryStateSynchronizer: LibraryStateSynchronizer,
) {
    /**
     * 内容导航归约结果；当 [loadedFullLibrary] 为 true 时，门面需要补跑待恢复的播放快照。
     *
     * @property state 归约后的最新 UI 状态
     * @property loadedFullLibrary 本次导航是否触发了完整曲库预热
     */
    data class Result(
        val state: MusicAppUiState,
        val loadedFullLibrary: Boolean = false,
    )

    /**
     * 首页内容分段切换在专辑和歌手页签下需要预热完整曲库，避免列表为空。
     */
    fun setHomeContentSection(state: MusicAppUiState, section: HomeContentSection): Result {
        val loadResult: Result = if (section.requiresFullLibrary()) {
            loadLocalMusicLibrary(state = state)
        } else {
            Result(state = state)
        }
        return loadResult.copy(
            state = loadResult.state.copy(homeContentSection = section),
        )
    }

    /**
     * 我的页歌曲统计入口必须回到首页歌曲分段，同时彻底退出二级页。
     */
    fun openHomeSongs(state: MusicAppUiState): Result {
        val rootState: MusicAppUiState = NavigationStateController.navigateToRoot(
            state = state,
            tab = RootTab.Home,
        )
        return Result(
            state = rootState.copy(homeContentSection = HomeContentSection.Songs),
        )
    }

    /**
     * 本地音乐页依赖完整曲库，否则歌曲、专辑和歌手分段都会不完整。
     */
    fun openLocalMusic(state: MusicAppUiState, section: LocalMusicSection): Result {
        val loadResult: Result = loadLocalMusicLibrary(state = state)
        return loadResult.navigateToSecondary(screen = SecondaryScreen.LocalMusic(initialSection = section))
    }

    /**
     * 扫描页只是路由切换入口，不应为了打开页面预热完整曲库。
     */
    fun openAudioScan(state: MusicAppUiState): Result {
        return Result(
            state = NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.AudioScan,
            ),
        )
    }

    /**
     * 最近播放页直接复用现有列表，不需要为导航额外触发全量加载。
     */
    fun openRecentPlayed(state: MusicAppUiState): Result {
        return Result(
            state = NavigationStateController.navigateToSecondary(
                state = state,
                screen = SecondaryScreen.RecentPlayed,
            ),
        )
    }

    /**
     * 专辑详情依赖完整曲库中的聚合实体，先加载再写入选中身份。
     */
    fun openAlbum(state: MusicAppUiState, album: Album): Result {
        val loadResult: Result = loadLocalMusicLibrary(state = state)
        return loadResult.navigateToSecondary(
            screen = SecondaryScreen.AlbumDetail,
            state = loadResult.state.copy(selectedAlbumId = album.id),
        )
    }

    /**
     * 歌手详情依赖完整曲库中的聚合实体，先加载再写入选中身份。
     */
    fun openArtist(state: MusicAppUiState, artist: Artist): Result {
        val loadResult: Result = loadLocalMusicLibrary(state = state)
        return loadResult.navigateToSecondary(
            screen = SecondaryScreen.ArtistDetail,
            state = loadResult.state.copy(selectedArtistId = artist.id),
        )
    }

    /**
     * 从歌曲打开专辑详情时先补齐完整曲库，再按专辑标题归一化匹配详情实体。
     */
    fun openAlbumFromSong(state: MusicAppUiState, song: Song): Result {
        val loadResult: Result = loadLocalMusicLibrary(state = state)
        val targetAlbum: Album = loadResult.state.detailAlbums.firstOrNull { album: Album ->
            hasSameAlbumTitle(
                firstTitle = album.title,
                secondTitle = song.album,
            )
        } ?: return loadResult.copy(
            state = loadResult.state.copy(moreSongId = null),
        )
        return openAlbum(
            state = loadResult.state.copy(moreSongId = null),
            album = targetAlbum,
        ).copy(
            loadedFullLibrary = loadResult.loadedFullLibrary,
        )
    }

    /**
     * 从歌曲打开歌手详情时先补齐完整曲库，再按歌手名归一化匹配详情实体。
     */
    fun openArtistFromSong(state: MusicAppUiState, song: Song): Result {
        val loadResult: Result = loadLocalMusicLibrary(state = state)
        val targetArtist: Artist = loadResult.state.detailArtists.firstOrNull { artist: Artist ->
            hasSameArtistName(
                firstName = artist.name,
                secondName = song.artist,
            )
        } ?: return loadResult.copy(
            state = loadResult.state.copy(moreSongId = null),
        )
        return openArtist(
            state = loadResult.state.copy(moreSongId = null),
            artist = targetArtist,
        ).copy(
            loadedFullLibrary = loadResult.loadedFullLibrary,
        )
    }

    /**
     * 只在首次需要完整曲库时真正加载，减少首页冷启动和普通导航成本。
     */
    fun loadLocalMusicLibrary(state: MusicAppUiState): Result {
        if (state.localSongs.isNotEmpty()) {
            return Result(state = state)
        }
        val nextState: MusicAppUiState = libraryStateSynchronizer.loadLocalMusicLibrary(state = state)
        return Result(
            state = nextState,
            loadedFullLibrary = nextState.localSongs.isNotEmpty(),
        )
    }

    /** 聚合型首页分段都依赖完整曲库。 */
    private fun HomeContentSection.requiresFullLibrary(): Boolean {
        return this == HomeContentSection.Albums || this == HomeContentSection.Artists
    }

    /** 统一封装二级页面跳转，避免各入口重复保留 [loadedFullLibrary]。 */
    private fun Result.navigateToSecondary(
        screen: SecondaryScreen,
        state: MusicAppUiState = this.state,
    ): Result {
        return copy(
            state = NavigationStateController.navigateToSecondary(
                state = state,
                screen = screen,
            ),
        )
    }
}
