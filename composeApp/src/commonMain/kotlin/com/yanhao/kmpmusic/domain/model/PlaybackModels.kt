package com.yanhao.kmpmusic.domain.model

/**
 * 搜索结果的范围过滤。
 */
enum class SearchScope {
    All,
    Songs,
    Albums,
    Artists,
}

/**
 * App 外观模式。
 */
enum class ThemeMode {
    Light,
    Dark,
    System,
}

/**
 * 平台无关的播放状态。
 */
enum class PlaybackStatus {
    Idle,
    Loading,
    Playing,
    Paused,
    Buffering,
    Ended,
    Error,
}

/**
 * 平台无关的队列播放模式。
 */
enum class PlaybackMode {
    LoopAll,
    LoopOne,
    Shuffle,
}

/**
 * 播放失败类型。
 */
enum class PlaybackErrorType {
    MissingFile,
    UnsupportedFormat,
    PermissionDenied,
    EngineUnavailable,
    Unknown,
}

/**
 * 播放错误信息。
 *
 * @property type 错误类别，供平台和 UI 显式分支。
 * @property songId 出错歌曲标识，无法定位歌曲时为 null。
 * @property message 面向诊断的错误信息。
 */
data class PlaybackError(
    val type: PlaybackErrorType,
    val songId: String?,
    val message: String,
)

/**
 * 可交给播放引擎的媒体项。
 *
 * @property songId 领域层歌曲标识。
 * @property title 媒体标题。
 * @property artist 媒体歌手名。
 * @property album 媒体专辑名。
 * @property durationMs 媒体总时长，未知时为 null。
 * @property localUri 平台 scanner 提供的可播放 URI。
 * @property coverArt 当前媒体封面。
 * @property mimeType 平台识别的媒体类型，未知时为 null。
 */
data class PlayableMedia(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long?,
    val localUri: String,
    val coverArt: CoverArt,
    val mimeType: String?,
)

/**
 * 当前播放状态，供迷你播放器和真实引擎协同使用。
 *
 * @property currentSongId 当前歌曲标识，没有活动歌曲时为 null。
 * @property status 当前播放状态枚举。
 * @property positionMs 当前进度，单位毫秒。
 * @property durationMs 当前歌曲总时长，未知时为 null。
 * @property error 最近一次播放错误，没有错误时为 null。
 */
data class PlaybackState(
    val currentSongId: String? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val error: PlaybackError? = null,
) {
    /**
     * 兼容旧 UI 读取，直到状态消费方完成迁移。
     */
    val isPlaying: Boolean
        get() = status == PlaybackStatus.Playing
}

/**
 * 播放队列状态。
 *
 * @property songIds 当前播放队列中的歌曲标识。
 * @property currentIndex 当前正在消费的队列下标，没有活动项时为 -1。
 * @property playbackMode 当前队列的循环/随机模式。
 * @property shuffleHistory Shuffle 模式下已播放过的下标历史。
 * @property shuffleRemaining Shuffle 模式下待播放的下标集合。
 */
data class QueueState(
    val songIds: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val playbackMode: PlaybackMode = PlaybackMode.LoopAll,
    val shuffleHistory: List<Int> = emptyList(),
    val shuffleRemaining: List<Int> = emptyList(),
) {
    /**
     * 兼容旧逻辑按歌曲标识读取当前队列项。
     */
    val currentSongId: String?
        get() = songIds.getOrNull(index = currentIndex)
}

/**
 * 真实播放历史，扫描结果不会自动进入这里。
 *
 * @property songIds 最近播放歌曲标识，新播放的歌曲排在最前。
 */
data class PlaybackHistory(
    val songIds: List<String> = emptyList(),
)

/**
 * 播放状态快照，供平台协调器做持久化和恢复。
 *
 * @property playbackState 当前播放状态。
 * @property queueState 当前队列状态。
 * @property updatedAt 最近一次刷新时间戳。
 */
data class PlaybackSnapshot(
    val playbackState: PlaybackState = PlaybackState(),
    val queueState: QueueState = QueueState(),
    val updatedAt: Long = 0L,
)
