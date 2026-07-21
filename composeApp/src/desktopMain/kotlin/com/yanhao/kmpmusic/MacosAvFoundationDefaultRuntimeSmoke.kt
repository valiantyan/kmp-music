package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.playback.MacosAvFoundationBridgeSmoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Path

/**
 * macOS 默认桌面运行时真实播放 smoke，验证生产装配链路已切到 AVFoundation。
 */
object MacosAvFoundationDefaultRuntimeSmoke {
    /** 运行默认桌面播放链路 smoke。 */
    @JvmStatic
    fun main(args: Array<String>): Unit =
        runBlocking {
            val workDir: Path = MacosAvFoundationBridgeSmoke.smokeWorkDir(args = args)
            val mediaPath: Path = MacosAvFoundationBridgeSmoke.prepareSmokeM4a(workDir = workDir)
            val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val audioRuntime: DesktopAudioRuntime = DesktopAudioRuntimeFactory.create(sessionScope = sessionScope)
            val controller: MusicAppController =
                MusicAppController(
                    audioPlayerEngine = audioRuntime.audioEngine,
                    controllerScope = sessionScope,
                )
            val songs: List<Song> =
                listOf(
                    smokeSong(id = "macos-avfoundation-default-runtime-smoke-1", mediaPath = mediaPath),
                    smokeSong(id = "macos-avfoundation-default-runtime-smoke-2", mediaPath = mediaPath),
                )
            val firstSong: Song = songs[0]
            val secondSong: Song = songs[1]
            try {
                controller.playSong(song = firstSong, queueSongs = songs)
                waitForState(label = "current-media", controller = controller) { state: MusicAppUiState ->
                    state.currentSongId == firstSong.id
                }
                waitForState(label = "playing", controller = controller) { state: MusicAppUiState ->
                    state.playbackStatus == PlaybackStatus.Playing
                }
                waitForState(label = "progress", controller = controller) { state: MusicAppUiState ->
                    state.playbackPositionMs > 0L
                }
                controller.seekTo(positionMs = 750L)
                waitForState(label = "seek", controller = controller) { state: MusicAppUiState ->
                    state.playbackPositionMs >= 700L
                }
                controller.pause()
                waitForState(label = "paused", controller = controller) { state: MusicAppUiState ->
                    state.playbackStatus == PlaybackStatus.Paused
                }
                controller.play()
                waitForState(label = "resume", controller = controller) { state: MusicAppUiState ->
                    state.playbackStatus == PlaybackStatus.Playing
                }
                controller.moveTrack(direction = 1)
                waitForState(label = "next", controller = controller) { state: MusicAppUiState ->
                    state.currentSongId == secondSong.id
                }
                controller.moveTrack(direction = -1)
                waitForState(label = "previous", controller = controller) { state: MusicAppUiState ->
                    state.currentSongId == firstSong.id
                }
                audioRuntime.audioEngine.stop()
                waitForState(label = "stop", controller = controller) { state: MusicAppUiState ->
                    state.playbackStatus == PlaybackStatus.Idle
                }
                println("macOS AVFoundation 默认桌面运行时 smoke 通过：${mediaPath.toUri()}")
            } finally {
                audioRuntime.audioEngine.releaseAndAwait()
                sessionScope.cancel()
            }
        }

    /** 构造真实文件对应的歌曲实体，模拟本地曲库点击后的播放输入。 */
    private fun smokeSong(
        id: String,
        mediaPath: Path,
    ): Song =
        Song(
            id = id,
            title = "macOS AVFoundation Default Runtime Smoke",
            artist = "KMP Music",
            album = "Smoke",
            duration = "00:01",
            coverArt = CoverArt.HeroLocalMusic,
            isLiked = false,
            lastPlayed = "",
            quality = "AAC",
            lyric = "",
            trackNumber = 1,
            durationMs = 1_500L,
            sourceId = mediaPath.fileName.toString(),
            sourceKind = LocalMusicSourceKind.DesktopFolder,
            localUri = mediaPath.toUri().toString(),
            mimeType = "audio/mp4",
            sizeBytes = mediaPath.toFile().length(),
            modifiedAt = mediaPath.toFile().lastModified(),
        )

    /** 等待控制器状态满足 smoke 检查点，超时即失败。 */
    private suspend fun waitForState(
        label: String,
        controller: MusicAppController,
        predicate: (MusicAppUiState) -> Boolean,
    ) {
        withTimeout(timeMillis = 10_000L) {
            while (!predicate(controller.uiState)) {
                delay(timeMillis = 50L)
            }
        }
        println("default-runtime-smoke-check: $label")
    }
}
