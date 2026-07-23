package com.yanhao.kmpmusic.qa

import com.yanhao.kmpmusic.data.FakeLocalMusicScanner
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.SearchScope
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.RootTab
import kmpmusic.composeapp.generated.resources.Res
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.nio.file.Path

/**
 * Desktop UI QA 的取证模式。
 */
internal enum class DesktopUiQaCaptureMode {
    Static,
    Scrollbar,
    PlaybackAnimation,
    Interaction,
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
    Me(
        argument = "me",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1280,
        windowHeight = 1024,
    ),
    RecentPlayed(
        argument = "recent-played",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
    ),
    AlbumDetail(
        argument = "album-detail",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
    ),
    AlbumDetailPlaying(
        argument = "album-detail-playing",
        captureMode = DesktopUiQaCaptureMode.PlaybackAnimation,
    ),
    ArtistDetailCompact(
        argument = "artist-detail-compact",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
        windowWidth = 1120,
        windowHeight = 760,
    ),
    ArtistDetail(
        argument = "artist-detail",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 800,
    ),
    ArtistDetailWide(
        argument = "artist-detail-wide",
        captureMode = DesktopUiQaCaptureMode.Scrollbar,
        windowWidth = 1440,
        windowHeight = 900,
    ),
    ArtistDetailPlaying(
        argument = "artist-detail-playing",
        captureMode = DesktopUiQaCaptureMode.PlaybackAnimation,
        windowWidth = 1240,
        windowHeight = 800,
    ),
    ArtistDetailNoCover(
        argument = "artist-detail-no-cover",
        captureMode = DesktopUiQaCaptureMode.Static,
        windowWidth = 1240,
        windowHeight = 800,
    ),
    ArtistDetailInteraction(
        argument = "artist-detail-interaction",
        captureMode = DesktopUiQaCaptureMode.Interaction,
        windowWidth = 1240,
        windowHeight = 800,
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
                "用法: desktopUiQa <home|home-playing|albums|artists|favorites|me|recent-played|album-detail|album-detail-playing|artist-detail-compact|artist-detail|artist-detail-wide|artist-detail-playing|artist-detail-no-cover|artist-detail-interaction|playlists|playlist-management|search|search-playing|search-albums|search-artists|search-playlists|search-empty> <output-directory>"
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

        DesktopUiQaScenario.Me -> prepareDesktopMeScenario(controller = controller)

        DesktopUiQaScenario.RecentPlayed -> prepareDesktopRecentPlayedScenario(controller = controller)

        DesktopUiQaScenario.AlbumDetail -> controller.openAlbumFromSong(song = controller.uiState.songs.first())

        DesktopUiQaScenario.AlbumDetailPlaying -> prepareDesktopAlbumDetailPlayingScenario(controller = controller)

        DesktopUiQaScenario.ArtistDetailCompact,
        DesktopUiQaScenario.ArtistDetail,
        DesktopUiQaScenario.ArtistDetailWide,
        DesktopUiQaScenario.ArtistDetailNoCover,
        DesktopUiQaScenario.ArtistDetailInteraction,
        -> controller.openArtistFromSong(song = controller.uiState.songs.first())

        DesktopUiQaScenario.ArtistDetailPlaying -> prepareDesktopArtistDetailPlayingScenario(controller = controller)

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

/** 为每个 QA 场景创建独立内存扫描器，避免测试数据读取或写入用户曲库。 */
internal fun createDesktopUiQaScanner(scenario: DesktopUiQaScenario): LocalMusicScanner {
    val scanner: LocalMusicScanner = FakeLocalMusicScanner(demoSongCount = DESKTOP_UI_QA_SONG_COUNT)
    return when (scenario) {
        DesktopUiQaScenario.AlbumDetail,
        DesktopUiQaScenario.AlbumDetailPlaying,
        -> {
            DesktopAlbumDetailUiQaScanner(delegate = scanner)
        }

        DesktopUiQaScenario.ArtistDetailCompact,
        DesktopUiQaScenario.ArtistDetail,
        DesktopUiQaScenario.ArtistDetailWide,
        DesktopUiQaScenario.ArtistDetailPlaying,
        DesktopUiQaScenario.ArtistDetailInteraction,
        -> {
            DesktopArtistDetailUiQaScanner(delegate = scanner, usesDefaultArtwork = false)
        }

        DesktopUiQaScenario.ArtistDetailNoCover -> {
            DesktopArtistDetailUiQaScanner(delegate = scanner, usesDefaultArtwork = true)
        }

        DesktopUiQaScenario.Me -> {
            DesktopMeUiQaScanner(delegate = scanner)
        }

        else -> {
            scanner
        }
    }
}

/** 专辑详情滚动验证需要足够长的单专辑队列，仅在 QA 入口重写扫描结果的展示元数据。 */
private class DesktopAlbumDetailUiQaScanner(
    // 委托保留既有 fake 音频 URI、时长和收藏数据，仅覆盖专辑归属。
    private val delegate: LocalMusicScanner,
) : LocalMusicScanner {
    /** 先取得既有 fake 结果，再把曲目投影到同一张 QA 专辑以产生真实长列表。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        val result: LocalMusicScanResult = delegate.scan(request = request)
        return result.copy(
            discovered =
                result.discovered.map { metadata ->
                    metadata.copy(
                        artist = DESKTOP_ALBUM_DETAIL_QA_ARTIST,
                        album = DESKTOP_ALBUM_DETAIL_QA_TITLE,
                    )
                },
        )
    }
}

/** 歌手详情 QA 将 fake 曲目投影为同一歌手；缺封面场景额外清空所有候选封面。 */
private class DesktopArtistDetailUiQaScanner(
    private val delegate: LocalMusicScanner,
    private val usesDefaultArtwork: Boolean,
) : LocalMusicScanner {
    /** 仅改写 QA 展示元数据，保证 120 首曲目在真实详情列表内形成可滚动队列。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        val result: LocalMusicScanResult = delegate.scan(request = request)
        return result.copy(
            discovered =
                result.discovered.map { metadata ->
                    metadata.copy(
                        artist = DESKTOP_ARTIST_DETAIL_QA_ARTIST,
                        coverArt = if (usesDefaultArtwork) CoverArt.HeroLocalMusic else metadata.coverArt,
                        coverImageUri = if (usesDefaultArtwork) null else metadata.coverImageUri,
                    )
                },
        )
    }
}

/** 我的页 QA 将前四首 fake 曲目投影为 Figma 参考内容，仅用于固定截图取证。 */
@OptIn(ExperimentalResourceApi::class)
private class DesktopMeUiQaScanner(
    private val delegate: LocalMusicScanner,
) : LocalMusicScanner {
    /** 保留 fake 音频和播放链路，只替换最近播放网格的可视元数据。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        val result: LocalMusicScanResult = delegate.scan(request = request)
        return result.copy(
            discovered =
                result.discovered.mapIndexed { index: Int, metadata ->
                    val fixture: DesktopMeUiQaSongFixture? = DESKTOP_ME_UI_QA_SONG_FIXTURES.getOrNull(index = index)
                    if (fixture == null) {
                        metadata
                    } else {
                        metadata.copy(
                            title = fixture.title,
                            artist = fixture.artist,
                            coverImageUri = Res.getUri(path = fixture.coverImageResourcePath),
                        )
                    }
                },
        )
    }
}

/** 我的页 QA 最近播放卡的固定展示元数据。 */
private data class DesktopMeUiQaSongFixture(
    /** 卡片标题。 */
    val title: String,
    /** 卡片歌手。 */
    val artist: String,
    /** 仓库内可复现封面的 Compose 资源路径。 */
    val coverImageResourcePath: String,
)

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

/** 通过真实播放历史生成四张最近播放卡，再回到我的页作为 Figma 默认内容密度。 */
private suspend fun prepareDesktopMeScenario(controller: MusicAppController) {
    val songs: List<Song> = controller.uiState.songs.take(n = DESKTOP_ME_UI_QA_RECENT_SONG_COUNT)
    check(songs.size == DESKTOP_ME_UI_QA_RECENT_SONG_COUNT) { "我的页 QA 缺少四首假歌曲" }
    songs.asReversed().forEach { song: Song ->
        controller.playSong(
            song = song,
            queueSongs = songs,
        )
        waitForDesktopUiQaCurrentSong(
            controller = controller,
            songId = song.id,
        )
    }
    controller.pause()
    controller.navigateToRoot(tab = RootTab.Me)
    check(controller.uiState.recentSongs.size >= DESKTOP_ME_UI_QA_RECENT_SONG_COUNT) {
        "我的页 QA 未生成四条真实最近播放记录"
    }
}

/** 通过真实最近播放历史构造可滚动列表，再进入完整列表页面验证固定顶栏。 */
private suspend fun prepareDesktopRecentPlayedScenario(controller: MusicAppController) {
    controller.loadLocalMusicLibrary()
    val songs: List<Song> = controller.uiState.localSongs.take(n = DESKTOP_RECENT_PLAYED_UI_QA_SONG_COUNT)
    check(songs.size == DESKTOP_RECENT_PLAYED_UI_QA_SONG_COUNT) { "最近播放 QA 缺少足够的假歌曲" }
    songs.asReversed().forEach { song: Song ->
        controller.playSong(
            song = song,
            queueSongs = songs,
        )
        waitForDesktopUiQaCurrentSong(
            controller = controller,
            songId = song.id,
        )
    }
    controller.pause()
    controller.openRecentPlayed()
    check(controller.uiState.recentSongs.size >= DESKTOP_RECENT_PLAYED_UI_QA_SONG_COUNT) {
        "最近播放 QA 未生成足够的真实播放历史"
    }
}

/** 逐首等待真实播放状态回写，避免异步播放历史记录被下一次 QA 命令覆盖。 */
private suspend fun waitForDesktopUiQaCurrentSong(
    controller: MusicAppController,
    songId: String,
) {
    repeat(times = PLAYBACK_STATE_POLL_COUNT) {
        if (controller.uiState.currentSongId == songId) {
            return
        }
        delay(timeMillis = PLAYBACK_STATE_POLL_INTERVAL_MILLIS)
    }
    check(controller.uiState.currentSongId == songId) { "Desktop UI QA 未切换到目标歌曲: $songId" }
}

/** 专辑详情播放态先进入真实专辑路由，再以完整专辑队列播放首曲。 */
private suspend fun prepareDesktopAlbumDetailPlayingScenario(controller: MusicAppController) {
    val song: Song = controller.uiState.songs.first()
    controller.openAlbumFromSong(song = song)
    prepareDesktopHomePlayingScenario(controller = controller)
}

/** 歌手详情播放态先进入真实详情路由，再以该歌手的完整 fake 队列播放首曲。 */
private suspend fun prepareDesktopArtistDetailPlayingScenario(controller: MusicAppController) {
    val song: Song = controller.uiState.songs.first()
    controller.openArtistFromSong(song = song)
    prepareDesktopHomePlayingScenario(controller = controller)
}

/** 播放状态最多等待 2 秒。 */
private const val PLAYBACK_STATE_POLL_COUNT: Int = 40

/** 播放状态轮询间隔（50ms）。 */
private const val PLAYBACK_STATE_POLL_INTERVAL_MILLIS: Long = 50L

/** 我的页 Figma 网格固定展示 4 张最近播放卡。 */
private const val DESKTOP_ME_UI_QA_RECENT_SONG_COUNT: Int = 4

/** 最近播放页 QA 最少需要 12 条真实历史，确保首页卡片列表可滚动。 */
private const val DESKTOP_RECENT_PLAYED_UI_QA_SONG_COUNT: Int = 12

/** Figma 最近播放第一张封面。 */
private const val DESKTOP_ME_UI_QA_COVER_VITAS: String = "drawable/desktop_me_qa_vitas.png"

/** Figma 最近播放第二张封面。 */
private const val DESKTOP_ME_UI_QA_COVER_M83: String = "drawable/desktop_me_qa_m83.jpg"

/** Figma 最近播放第三张封面。 */
private const val DESKTOP_ME_UI_QA_COVER_ZEDD: String = "drawable/desktop_me_qa_zedd.jpg"

/** Figma 最近播放第四张封面。 */
private const val DESKTOP_ME_UI_QA_COVER_BILLIE_EILISH: String = "drawable/desktop_me_qa_billie_eilish.jpg"

/** 我的页 QA 使用 Figma 对应的四首歌曲和封面，以便固定窗口截图逐项比对。 */
private val DESKTOP_ME_UI_QA_SONG_FIXTURES: List<DesktopMeUiQaSongFixture> =
    listOf(
        DesktopMeUiQaSongFixture(
            title = "Я тебя никогда не забуду",
            artist = "Vitas",
            coverImageResourcePath = DESKTOP_ME_UI_QA_COVER_VITAS,
        ),
        DesktopMeUiQaSongFixture(
            title = "Midnight City",
            artist = "M83",
            coverImageResourcePath = DESKTOP_ME_UI_QA_COVER_M83,
        ),
        DesktopMeUiQaSongFixture(
            title = "Clarity",
            artist = "Zedd",
            coverImageResourcePath = DESKTOP_ME_UI_QA_COVER_ZEDD,
        ),
        DesktopMeUiQaSongFixture(
            title = "Ocean Eyes",
            artist = "Billie Eilish",
            coverImageResourcePath = DESKTOP_ME_UI_QA_COVER_BILLIE_EILISH,
        ),
    )

/** 歌单 QA 使用 4 条数据，对齐两个 Figma 节点展示的默认内容密度。 */
private const val DESKTOP_UI_QA_PLAYLIST_COUNT: Int = 4

/** 专辑详情 QA 使用 120 首数据，以覆盖曲目表滚动与滚动条自动隐藏。 */
private const val DESKTOP_UI_QA_SONG_COUNT: Int = 120

/** 专辑详情 QA 的统一歌手，避免头部元信息因扫描顺序变化。 */
private const val DESKTOP_ALBUM_DETAIL_QA_ARTIST: String = "QA Artist"

/** 专辑详情 QA 的统一专辑名，确保全部 fake 曲目进入同一条详情路由。 */
private const val DESKTOP_ALBUM_DETAIL_QA_TITLE: String = "QA Album Detail"

/** 歌手详情 QA 的统一歌手，保证长列表与 hero 标题不受 fake catalog 分组影响。 */
private const val DESKTOP_ARTIST_DETAIL_QA_ARTIST: String = "QA Artist Detail"
