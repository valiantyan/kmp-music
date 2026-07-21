package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.data.createDesktopPlaybackDatabaseAtPath
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.persistence.LocalSongEntity
import com.yanhao.kmpmusic.domain.persistence.PlaybackDatabase
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.playback.ApplePlaybackBridge
import com.yanhao.kmpmusic.playback.ApplePlaybackBridgeCommandAck
import com.yanhao.kmpmusic.playback.ApplePlaybackBridgeEvent
import com.yanhao.kmpmusic.playback.ApplePlaybackBridgePrepareRequest
import com.yanhao.kmpmusic.playback.ApplePlaybackBridgeSeekRequest
import com.yanhao.kmpmusic.playback.MacosAvFoundationPlaybackBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections

/**
 * 单次重启恢复 smoke 进程的核心对象集合。
 *
 * @property runtime 负责关闭和快照落盘的桌面会话运行时。
 * @property controller 当前进程的共享播放控制器。
 * @property bridge 带 prepare 记录能力的 Apple bridge。
 */
internal data class RestartResumeSmokeSession(
    val runtime: DesktopPlaybackSessionRuntime,
    val controller: MusicAppController,
    val bridge: RecordingApplePlaybackBridge,
)

/**
 * 记录 prepare 请求的 bridge 装饰器，用来证明恢复后 native 准备的是同一首歌。
 */
internal class RecordingApplePlaybackBridge(
    // 真实 macOS AVFoundation bridge。
    private val delegate: ApplePlaybackBridge,
) : ApplePlaybackBridge {
    // 已观察到的 prepare 请求。
    private val prepareRequests: MutableList<ApplePlaybackBridgePrepareRequest> =
        Collections.synchronizedList(
            mutableListOf(),
        )

    override val events: Flow<ApplePlaybackBridgeEvent>
        get() = delegate.events

    /** 查找匹配的 prepare 请求，供 smoke 在恢复点做断言。 */
    fun findPrepareRequest(
        predicate: (ApplePlaybackBridgePrepareRequest) -> Boolean,
    ): ApplePlaybackBridgePrepareRequest? =
        synchronized(prepareRequests) {
            prepareRequests.lastOrNull(predicate = predicate)
        }

    /** 记录后转发 prepare 请求。 */
    override suspend fun prepare(request: ApplePlaybackBridgePrepareRequest): ApplePlaybackBridgeCommandAck {
        prepareRequests += request
        return delegate.prepare(request = request)
    }

    /** 转发播放命令。 */
    override suspend fun play(generation: Long): ApplePlaybackBridgeCommandAck = delegate.play(generation = generation)

    /** 转发暂停命令。 */
    override suspend fun pause(generation: Long): ApplePlaybackBridgeCommandAck = delegate.pause(generation = generation)

    /** 转发 seek 命令。 */
    override suspend fun seekTo(request: ApplePlaybackBridgeSeekRequest): ApplePlaybackBridgeCommandAck = delegate.seekTo(request = request)

    /** 转发停止命令。 */
    override suspend fun stop(generation: Long): ApplePlaybackBridgeCommandAck = delegate.stop(generation = generation)

    /** 转发音量命令。 */
    override suspend fun setVolume(volume: Float): ApplePlaybackBridgeCommandAck = delegate.setVolume(volume = volume)

    /** 释放真实 bridge。 */
    override suspend fun release(): ApplePlaybackBridgeCommandAck = delegate.release()
}

/** 创建一段独立桌面会话，模拟一次新的 App 进程。 */
internal suspend fun createRestartResumeSmokeSession(
    databasePath: Path,
    seedSongs: List<Song>,
): RestartResumeSmokeSession {
    val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val playbackDatabase: PlaybackDatabase =
        createDesktopPlaybackDatabaseAtPath(
            databasePath = databasePath.toString(),
        )
    if (seedSongs.isNotEmpty()) {
        playbackDatabase.localSongDao().upsertSongs(
            songs = seedSongs.map { song: Song -> song.toRestartResumeLocalSongEntity() },
        )
    }
    val bridge: RecordingApplePlaybackBridge =
        RecordingApplePlaybackBridge(
            delegate = MacosAvFoundationPlaybackBridge.create(),
        )
    val audioRuntime: DesktopAudioRuntime =
        DesktopAudioRuntimeFactory.create(
            sessionScope = sessionScope,
            bridgeFactory = { bridge },
        )
    val controller: MusicAppController =
        createDesktopPlaybackController(
            playbackDatabase = playbackDatabase,
            audioPlayerEngine = audioRuntime.audioEngine,
            controllerScope = sessionScope,
        )
    val runtime: DesktopPlaybackSessionRuntime =
        DesktopPlaybackSessionRuntime(
            controller = controller,
            sessionScope = sessionScope,
            releaseAudioEngineAndAwait = {
                audioRuntime.audioEngine.releaseAndAwait()
            },
            closePlaybackDatabase = {
                playbackDatabase.close()
            },
        )
    return RestartResumeSmokeSession(runtime = runtime, controller = controller, bridge = bridge)
}

/** 构造真实文件对应的歌曲实体，模拟桌面本地曲库中的两首可播放歌曲。 */
internal fun createRestartResumeSmokeSong(
    id: String,
    mediaPath: Path,
    trackNumber: Int,
): Song =
    Song(
        id = id,
        title = "macOS AVFoundation Restart Resume $trackNumber",
        artist = "KMP Music",
        album = "Restart Resume Smoke",
        duration = "00:01",
        coverArt = CoverArt.HeroLocalMusic,
        isLiked = false,
        lastPlayed = "",
        quality = "AAC",
        lyric = "",
        trackNumber = trackNumber,
        durationMs = RESTART_RESUME_SMOKE_DURATION_MS,
        sourceId = mediaPath.fileName.toString(),
        sourceKind = LocalMusicSourceKind.DesktopFolder,
        localUri = mediaPath.toUri().toString(),
        mimeType = "audio/mp4",
        sizeBytes = mediaPath.toFile().length(),
        modifiedAt = mediaPath.toFile().lastModified(),
    )

/** 转成 Room 可恢复的本地歌曲记录。 */
private fun Song.toRestartResumeLocalSongEntity(): LocalSongEntity =
    LocalSongEntity(
        id = id,
        sourceId = sourceId,
        sourceKind = sourceKind.value,
        concreteSourceId = "restart-resume-smoke",
        localUri = localUri,
        fileName = sourceId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        modifiedAt = modifiedAt,
        coverArt = coverArt.name,
        lastScannedAt = System.currentTimeMillis(),
        isAvailable = true,
    )

/** 清理上次 smoke 留下的 Room 主文件和 WAL 文件，保证每次从空库开始。 */
internal fun deleteRestartResumeSmokeDatabaseFiles(databasePath: Path) {
    Files.deleteIfExists(databasePath)
    Files.deleteIfExists(databasePath.resolveSibling("${databasePath.fileName}-shm"))
    Files.deleteIfExists(databasePath.resolveSibling("${databasePath.fileName}-wal"))
}

/** 重启恢复 smoke 样本总时长，和 [com.yanhao.kmpmusic.playback.MacosAvFoundationBridgeSmoke.prepareSmokeM4a] 保持一致。 */
private const val RESTART_RESUME_SMOKE_DURATION_MS = 1_500L
