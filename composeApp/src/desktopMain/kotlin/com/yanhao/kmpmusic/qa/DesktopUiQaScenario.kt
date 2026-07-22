package com.yanhao.kmpmusic.qa

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
                "用法: desktopUiQa <home|home-playing|albums|artists|favorites|playlists|playlist-management> <output-directory>"
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
