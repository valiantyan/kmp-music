package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.playback.ApplePlaybackBridgePrepareRequest
import com.yanhao.kmpmusic.playback.MacosAvFoundationBridgeSmoke
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * macOS AVFoundation 真实重启恢复 smoke，模拟播放、拖动进度、关闭进程、重开并继续播放。
 */
object MacosAvFoundationRestartResumeSmoke {
    /** 运行真实重启恢复 smoke。 */
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        val workDir: Path = MacosAvFoundationBridgeSmoke.smokeWorkDir(args = args)
        Files.createDirectories(workDir)
        val firstMediaPath: Path = MacosAvFoundationBridgeSmoke.prepareSmokeM4a(workDir = workDir)
        val secondMediaPath: Path = workDir.resolve("macos-avfoundation-restart-resume-second.m4a")
        Files.copy(firstMediaPath, secondMediaPath, StandardCopyOption.REPLACE_EXISTING)
        val databasePath: Path = workDir.resolve("macos-avfoundation-restart-resume.db")
        deleteRestartResumeSmokeDatabaseFiles(databasePath = databasePath)
        val songs: List<Song> = listOf(
            createRestartResumeSmokeSong(id = "macos-avfoundation-restart-resume-1", mediaPath = firstMediaPath, trackNumber = 1,),
            createRestartResumeSmokeSong(id = "macos-avfoundation-restart-resume-2", mediaPath = secondMediaPath, trackNumber = 2,),
        )
        val restoredSong: Song = songs[1]
        runFirstProcess(databasePath = databasePath, songs = songs, restoredSong = restoredSong)
        runSecondProcess(databasePath = databasePath, restoredSong = restoredSong)
        println("macOS AVFoundation 重启恢复 smoke 通过：songId=${restoredSong.id}, mediaUri=${restoredSong.localUri}")
    }

    /** 执行第一次进程：播放第二首，seek 后关闭并持久化最后进度。 */
    private suspend fun runFirstProcess(
        databasePath: Path,
        songs: List<Song>,
        restoredSong: Song,
    ) {
        val session: RestartResumeSmokeSession = createRestartResumeSmokeSession(
            databasePath = databasePath,
            seedSongs = songs,
        )
        try {
            session.controller.playSong(song = restoredSong, queueSongs = songs)
            waitForState(label = "first-process-playing", controller = session.controller) { state: MusicAppUiState ->
                state.currentSongId == restoredSong.id && state.playbackStatus == PlaybackStatus.Playing
            }
            session.controller.seekTo(positionMs = RESTORE_TARGET_POSITION_MS)
            val seekedState: MusicAppUiState = waitForState(
                label = "first-process-seek",
                controller = session.controller,
            ) { state: MusicAppUiState ->
                state.currentSongId == restoredSong.id &&
                    state.playbackStatus == PlaybackStatus.Playing &&
                    state.playbackPositionMs >= RESTORE_POSITION_LOWER_BOUND_MS
            }
            val prepareRequest: ApplePlaybackBridgePrepareRequest = waitForPrepareRequest(
                label = "first-process-prepare",
                bridge = session.bridge,
            ) { request: ApplePlaybackBridgePrepareRequest ->
                request.songId == restoredSong.id
            }
            checkPrepareRequest(
                label = "first-process",
                request = prepareRequest,
                expectedSong = restoredSong,
                minimumStartPositionMs = 0L,
            )
            printState(label = "first-process-before-close", state = seekedState)
        } finally {
            session.runtime.close()
        }
    }

    /** 执行第二次进程：从 Room 快照恢复同一首歌，再继续播放并确认进度前进。 */
    private suspend fun runSecondProcess(databasePath: Path, restoredSong: Song) {
        val session: RestartResumeSmokeSession = createRestartResumeSmokeSession(
            databasePath = databasePath,
            seedSongs = emptyList(),
        )
        try {
            session.runtime.ensurePlaybackSnapshotRestoreRequested()
            val restoredState: MusicAppUiState = waitForState(
                label = "second-process-restored",
                controller = session.controller,
            ) { state: MusicAppUiState ->
                state.currentSongId == restoredSong.id &&
                    state.playbackStatus == PlaybackStatus.Paused &&
                    state.playbackPositionMs >= RESTORE_POSITION_LOWER_BOUND_MS
            }
            val prepareRequest: ApplePlaybackBridgePrepareRequest = waitForPrepareRequest(
                label = "second-process-restore-prepare",
                bridge = session.bridge,
            ) { request: ApplePlaybackBridgePrepareRequest ->
                request.songId == restoredSong.id &&
                    request.startPositionMs >= RESTORE_POSITION_LOWER_BOUND_MS
            }
            checkPrepareRequest(
                label = "second-process-restore",
                request = prepareRequest,
                expectedSong = restoredSong,
                minimumStartPositionMs = RESTORE_POSITION_LOWER_BOUND_MS,
            )
            session.controller.play()
            waitForState(label = "second-process-playing", controller = session.controller) { state: MusicAppUiState ->
                state.currentSongId == restoredSong.id &&
                    state.playbackStatus == PlaybackStatus.Playing &&
                    state.playbackPositionMs >= RESTORE_POSITION_LOWER_BOUND_MS
            }
            val progressedState: MusicAppUiState = waitForState(
                label = "second-process-progressed",
                controller = session.controller,
            ) { state: MusicAppUiState ->
                state.currentSongId == restoredSong.id &&
                    state.playbackStatus == PlaybackStatus.Playing &&
                    state.playbackPositionMs > restoredState.playbackPositionMs
            }
            printState(label = "second-process-after-resume", state = progressedState)
        } finally {
            session.runtime.close()
        }
    }

    /** 等待控制器进入目标状态，并输出关键播放事实。 */
    private suspend fun waitForState(
        label: String,
        controller: MusicAppController,
        predicate: (MusicAppUiState) -> Boolean,
    ): MusicAppUiState {
        val state: MusicAppUiState = withTimeout(timeMillis = 10_000L) {
            while (!predicate(controller.uiState)) {
                delay(timeMillis = 50L)
            }
            controller.uiState
        }
        printState(label = label, state = state)
        return state
    }

    /** 等待 native bridge 收到 prepare 请求，避免只验证数据库初始化出的 UI 状态。 */
    private suspend fun waitForPrepareRequest(
        label: String,
        bridge: RecordingApplePlaybackBridge,
        predicate: (ApplePlaybackBridgePrepareRequest) -> Boolean,
    ): ApplePlaybackBridgePrepareRequest {
        val request: ApplePlaybackBridgePrepareRequest = withTimeout(timeMillis = 10_000L) {
            while (bridge.findPrepareRequest(predicate = predicate) == null) {
                delay(timeMillis = 50L)
            }
            checkNotNull(bridge.findPrepareRequest(predicate = predicate))
        }
        println(
            "restart-resume-prepare-check: label=$label,songId=${request.songId},startPositionMs=${request.startPositionMs},mediaUri=${request.mediaUri}",
        )
        return request
    }

    /** 校验底层 bridge 准备的是目标歌曲和目标起始进度。 */
    private fun checkPrepareRequest(
        label: String,
        request: ApplePlaybackBridgePrepareRequest,
        expectedSong: Song,
        minimumStartPositionMs: Long,
    ) {
        check(request.songId == expectedSong.id) {
            "$label 歌曲不匹配：actual=${request.songId}, expected=${expectedSong.id}"
        }
        check(request.mediaUri == expectedSong.localUri) {
            "$label 媒体 URI 不匹配：actual=${request.mediaUri}, expected=${expectedSong.localUri}"
        }
        check(request.startPositionMs >= minimumStartPositionMs) {
            "$label 起始进度不匹配：actual=${request.startPositionMs}, expected>=$minimumStartPositionMs"
        }
        println(
            "restart-resume-prepare: label=$label,songId=${request.songId},startPositionMs=${request.startPositionMs},mediaUri=${request.mediaUri}",
        )
    }

    /** 输出当前控制器状态，便于人工核对 songId、队列下标、进度和总时长。 */
    private fun printState(label: String, state: MusicAppUiState) {
        val queueIndex: Int = state.queueSongIds.indexOf(element = state.currentSongId)
        println(
            "restart-resume-state: label=$label,songId=${state.currentSongId},queueIndex=$queueIndex,status=${state.playbackStatus},positionMs=${state.playbackPositionMs},durationMs=${state.playbackDurationMs}",
        )
    }

    /** seek 目标进度，落在 1.5 秒样本中段。 */
    private const val RESTORE_TARGET_POSITION_MS = 750L

    /** 允许 AVFoundation 进度存在少量误差，但恢复不能退回开头。 */
    private const val RESTORE_POSITION_LOWER_BOUND_MS = 600L
}
