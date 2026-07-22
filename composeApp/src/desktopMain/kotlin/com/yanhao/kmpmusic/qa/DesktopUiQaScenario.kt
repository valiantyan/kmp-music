package com.yanhao.kmpmusic.qa

import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.RootTab
import kotlinx.coroutines.delay
import java.nio.file.Path

/**
 * Desktop UI QA 的取证模式。
 */
internal enum class DesktopUiQaCaptureMode {
    Static,
    Scrollbar,
    PlaybackAnimation,
}

/**
 * 可直接启动的 Desktop UI QA 场景。
 *
 * @property argument 命令行场景名。
 * @property captureMode 与场景 claim 对应的取证方式。
 * @property windowWidth 场景要求的固定窗口宽度。
 * @property windowHeight 场景要求的固定窗口高度。
 */
internal enum class DesktopUiQaScenario(
    val argument: String,
    val captureMode: DesktopUiQaCaptureMode,
    val windowWidth: Int = DesktopUiQaCaptureSpec.WINDOW_WIDTH,
    val windowHeight: Int = DesktopUiQaCaptureSpec.WINDOW_HEIGHT,
) {
    Home(
        argument = "home",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
    ),
    HomePlaying(
        argument = "home-playing",
        captureMode = DesktopUiQaCaptureMode.PlaybackAnimation,
    ),
    Albums(
        argument = "albums",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
    ),
    Artists(
        argument = "artists",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
    ),
    Favorites(
        argument = "favorites",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
        windowWidth = 1280,
        windowHeight = 1024,
    ),
    Playlists(
        argument = "playlists",
        captureMode = DesktopUiQaCaptureMode.Static,
    ),
    PlaylistManagement(
        argument = "playlist-management",
        captureMode = DesktopUiQaCaptureMode.Static,
    ),
    Search(
        argument = "search",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 824,
    ),
    SearchPlaying(
        argument = "search-playing",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 824,
    ),
    SearchAlbums(
        argument = "search-albums",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 824,
    ),
    SearchArtists(
        argument = "search-artists",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 824,
    ),
    SearchPlaylists(
        argument = "search-playlists",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 824,
    ),
    SearchEmpty(
        argument = "search-empty",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 824,
    ),
    ;

    companion object {
        /** 将命令行名称解析为受支持场景，未知名称立即失败。 */
        fun parse(argument: String): DesktopUiQaScenario =
            entries.firstOrNull { scenario: DesktopUiQaScenario -> scenario.argument == argument }
                ?: error("不支持的 Desktop UI QA 场景: $argument")
    }
}

/**
 * Desktop UI QA 单次运行配置。
 *
 * @property scenario 需要直接打开的真实页面或播放状态。
 * @property outputDirectory 三帧截图输出目录。
 */
internal data class DesktopUiQaConfig(
    val scenario: DesktopUiQaScenario,
    val outputDirectory: Path,
) {
    companion object {
        /** 命令行必须明确给出场景和输出目录，避免证据写入未知位置。 */
        fun parse(args: Array<String>): DesktopUiQaConfig {
            require(args.size == EXPECTED_ARGUMENT_COUNT) {
                "用法: desktopUiQa <home|home-playing|albums|artists|favorites|playlists|playlist-management|search|search-playing|search-albums|search-artists|search-playlists|search-empty> <output-directory>"
            }
            return DesktopUiQaConfig(
                scenario = DesktopUiQaScenario.parse(argument = args[0]),
                outputDirectory = Path.of(args[1]),
            )
        }

        /** QA 入口固定接收场景和输出目录两个参数。 */
        private const val EXPECTED_ARGUMENT_COUNT: Int = 2
    }
}

/**
 * 使用内存 fake 曲库准备真实 Desktop 壳和目标路由，不读取用户数据库。
 */
internal suspend fun prepareDesktopUiQaScenario(
    controller: MusicAppController,
    scenario: DesktopUiQaScenario,
) {
    controller.scanLocalMusic()
    check(controller.uiState.songs.isNotEmpty()) { "Desktop UI QA fake 曲库为空" }
    when (scenario) {
        DesktopUiQaScenario.Home -> Unit
        DesktopUiQaScenario.Albums -> controller.openLocalMusic(section = LocalMusicSection.Albums)
        DesktopUiQaScenario.Artists -> controller.openLocalMusic(section = LocalMusicSection.Artists)
        DesktopUiQaScenario.Favorites -> controller.navigateToRoot(tab = RootTab.Favorites)
        DesktopUiQaScenario.Playlists -> prepareDesktopPlaylistScenario(controller = controller, opensManagement = false)
        DesktopUiQaScenario.PlaylistManagement -> prepareDesktopPlaylistScenario(controller = controller, opensManagement = true)
        DesktopUiQaScenario.HomePlaying -> prepareDesktopHomePlayingScenario(controller = controller)
        DesktopUiQaScenario.Search -> prepareDesktopSearchLandingScenario(controller = controller)
        DesktopUiQaScenario.SearchPlaying -> prepareDesktopSearchPlayingScenario(controller = controller)
        DesktopUiQaScenario.SearchAlbums -> prepareDesktopSearchAlbumScenario(controller = controller)
        DesktopUiQaScenario.SearchArtists -> prepareDesktopSearchArtistScenario(controller = controller)
        DesktopUiQaScenario.SearchPlaylists -> prepareDesktopSearchPlaylistScenario(controller = controller)
        DesktopUiQaScenario.SearchEmpty -> prepareDesktopSearchEmptyScenario(controller = controller)
    }
}

/** 使用控制器创建设计稿数量的演示歌单，保证首帧同时呈现网格、创建卡片和选择列表。 */
private fun prepareDesktopPlaylistScenario(
    controller: MusicAppController,
    opensManagement: Boolean,
) {
    repeat(times = DESKTOP_UI_QA_PLAYLIST_COUNT) { index: Int ->
        controller.openEmptyPlaylistDialog()
        controller.setEmptyPlaylistName(name = "QA 歌单 ${index + 1}")
        controller.createEmptyPlaylist()
    }
    controller.openDesktopLocalPlaylists()
    if (!opensManagement) {
        return
    }
    controller.openLocalPlaylistManagement()
    controller.uiState.localPlaylists.firstOrNull()?.let { playlist ->
        controller.toggleManagedLocalPlaylistSelection(playlistId = playlist.id)
    }
}

/** 保留历史词但清空输入，验证搜索初始态的历史和空态组合。 */
private fun prepareDesktopSearchLandingScenario(controller: MusicAppController) {
    controller.openSearch(initialScope = SearchScope.Songs)
    controller.setSearchQuery(query = "搜索历史")
    controller.commitSearchQueryToHistory()
    controller.setSearchQuery(query = "")
}

/** 搜索歌曲并进入真实播放状态，覆盖底部播放器与搜索结果共存。 */
private suspend fun prepareDesktopSearchPlayingScenario(controller: MusicAppController) {
    val song: Song = controller.uiState.songs.first()
    prepareDesktopSearchQuery(
        controller = controller,
        query = song.title,
        scope = SearchScope.Songs,
    )
    prepareDesktopHomePlayingScenario(controller = controller)
}

/** 选择专辑 Tab，搜索本地已有专辑。 */
private fun prepareDesktopSearchAlbumScenario(controller: MusicAppController) {
    controller.openSearch()
    prepareDesktopSearchQuery(
        controller = controller,
        query = requireNotNull(controller.uiState.localAlbums.firstOrNull()).title,
        scope = SearchScope.Albums,
    )
}

/** 选择歌手 Tab，搜索本地已有歌手。 */
private fun prepareDesktopSearchArtistScenario(controller: MusicAppController) {
    controller.openSearch()
    prepareDesktopSearchQuery(
        controller = controller,
        query = requireNotNull(controller.uiState.localArtists.firstOrNull()).name,
        scope = SearchScope.Artists,
    )
}

/** 选择歌单 Tab，搜索运行时创建的本地歌单。 */
private fun prepareDesktopSearchPlaylistScenario(controller: MusicAppController) {
    val playlistName: String = "QA 搜索歌单"
    controller.openEmptyPlaylistDialog()
    controller.setEmptyPlaylistName(name = playlistName)
    controller.createEmptyPlaylist()
    prepareDesktopSearchQuery(
        controller = controller,
        query = playlistName,
        scope = SearchScope.All,
    )
}

/** 输入不存在的词，验证无结果态而非初始空态。 */
private fun prepareDesktopSearchEmptyScenario(controller: MusicAppController) {
    prepareDesktopSearchQuery(
        controller = controller,
        query = "不存在的 QA 搜索词",
        scope = SearchScope.Songs,
    )
}

/** 使用控制器完整执行输入、提交和筛选范围切换。 */
private fun prepareDesktopSearchQuery(
    controller: MusicAppController,
    query: String,
    scope: SearchScope,
) {
    controller.openSearch(initialScope = scope)
    controller.setSearchQuery(query = query)
    controller.commitSearchQueryToHistory()
    controller.setSearchScope(scope = scope)
}

// 播放场景使用真实控制器状态切换，避免静态参数伪造播放中动画。
private suspend fun prepareDesktopHomePlayingScenario(controller: MusicAppController) {
    val songs: List<Song> = controller.uiState.songs
    controller.playSong(
        song = songs.first(),
        queueSongs = songs,
    )
    repeat(times = PLAYBACK_STATE_POLL_COUNT) {
        if (controller.uiState.isPlaying) {
            return
        }
        delay(timeMillis = PLAYBACK_STATE_POLL_INTERVAL_MILLIS)
    }
    check(controller.uiState.isPlaying) { "Desktop UI QA 未进入播放中状态" }
}

/** 播放状态最多等待 2 秒。 */
private const val PLAYBACK_STATE_POLL_COUNT: Int = 40

/** 播放状态轮询间隔（50ms）。 */
private const val PLAYBACK_STATE_POLL_INTERVAL_MILLIS: Long = 50L

/** 歌单 QA 使用 4 条数据，对齐两个 Figma 节点展示的默认内容密度。 */
private const val DESKTOP_UI_QA_PLAYLIST_COUNT: Int = 4
