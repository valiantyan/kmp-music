package com.yanhao.kmpmusic.domain.model

/**
 * 本地音乐来源类型，value 与产品文档中的 sourceKind 保持一致。
 */
enum class LocalMusicSourceKind(
    val value: String,
    val displayName: String,
) {
    AndroidMediaStore(value = "androidMediaStore", displayName = "Android 媒体库"),
    IosImportedFile(value = "iosImportedFile", displayName = "iOS 导入文件"),
    IosMediaLibrary(value = "iosMediaLibrary", displayName = "iOS 音乐资料库"),
    DesktopFolder(value = "desktopFolder", displayName = "桌面文件夹"),
    FakeScanner(value = "fakeScanner", displayName = "演示扫描"),
}

/**
 * 本地音乐扫描请求，表达平台无关的扫描意图。
 */
sealed interface LocalMusicScanRequest {
    data object InitialScan : LocalMusicScanRequest
    data object Refresh : LocalMusicScanRequest
    data class Source(val sourceKind: LocalMusicSourceKind) : LocalMusicScanRequest
}

/**
 * 本地音乐扫描进度，供首页卡片和弹层展示状态摘要。
 */
data class LocalMusicScanProgress(
    val processedCount: Int = 0,
    val discoveredCount: Int = 0,
    val currentSourceName: String = "",
)

/**
 * 平台无关扫描错误类型，避免 UI 依赖平台异常。
 */
enum class LocalMusicScanErrorType {
    PermissionDenied,
    PermissionPermanentlyDenied,
    UserCancelled,
    FolderUnavailable,
    FileMissing,
    FileUnreadable,
    UnsupportedFormat,
    MetadataUnavailable,
    SecurityScopeExpired,
    Unknown,
}

/**
 * 扫描错误详情，用于错误页和来源分段说明失败原因。
 */
data class LocalMusicScanError(
    val type: LocalMusicScanErrorType,
    val message: String,
    val sourceKind: LocalMusicSourceKind? = null,
    val sourceId: String? = null,
)

/**
 * 平台 scanner 的可预期失败，控制器用它进入统一扫描错误态。
 *
 * @property error 平台无关扫描错误，供 UI 渲染和来源页展示。
 */
class LocalMusicScanException(
    val error: LocalMusicScanError,
    cause: Throwable? = null,
) : RuntimeException(error.message, cause)

/**
 * 平台 scanner 输出的音频元数据，UI 不直接构造这个模型。
 *
 * @property sourceId 来源内稳定标识，例如系统媒体库 id 或桌面文件路径 hash。
 * @property sourceKind 来源类型。
 * @property localUri 播放层后续可解析的本地 URI 字符串。
 * @property fileName 原始文件名，用于标题兜底和问题定位。
 * @property title 元数据标题，缺失时由合并层兜底。
 * @property artist 元数据歌手，缺失时由合并层兜底。
 * @property album 元数据专辑，缺失时由合并层兜底。
 * @property durationMs 音频时长毫秒数，未知时为空。
 * @property mimeType 媒体类型，未知时为空。
 * @property sizeBytes 文件大小字节数，未知时为空。
 * @property modifiedAt 来源修改时间戳，未知时为空。
 * @property coverArt 应用内封面兜底资源。
 * @property coverImageUri 平台扫描器提取出的封面图片 URI，缺失时使用 [coverArt]。
 */
data class MusicFileMetadata(
    val sourceId: String,
    val sourceKind: LocalMusicSourceKind,
    val localUri: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val modifiedAt: Long?,
    val coverArt: CoverArt,
    val coverImageUri: String? = null,
) {
    /**
     * 跨平台稳定来源 key，重新扫描后用它合并元数据和用户状态。
     */
    val sourceKey: String = "${sourceKind.value}:$sourceId"
}

/**
 * 扫描问题条目，不进入可播放歌曲列表。
 */
data class LocalMusicProblem(
    val sourceKind: LocalMusicSourceKind,
    val sourceId: String,
    val fileName: String,
    val error: LocalMusicScanError,
)

/**
 * 单个来源的扫描摘要，用于二级来源页。
 */
data class LocalMusicSourceSummary(
    val sourceKind: LocalMusicSourceKind,
    val displayName: String,
    val songCount: Int,
    val problemCount: Int,
    val lastScannedAt: Long?,
)

/**
 * 最近一次扫描摘要，用于首页卡片展示结果。
 */
data class LocalMusicLastScanSummary(
    val addedCount: Int,
    val updatedCount: Int,
    val removedCount: Int,
    val problemCount: Int,
    val completedAt: Long,
)

/**
 * Scanner 返回结果，discovered 同时承载新增和更新的可播放候选条目。
 */
data class LocalMusicScanResult(
    val discovered: List<MusicFileMetadata>,
    val removedSourceKeys: Set<String> = emptySet(),
    val failed: List<LocalMusicProblem> = emptyList(),
    val sourceSummaries: List<LocalMusicSourceSummary> = emptyList(),
    val completedAt: Long = 0L,
)

/**
 * 本地扫描状态，页面只根据这个平台无关状态渲染。
 */
sealed interface LocalMusicScanState {
    data object Idle : LocalMusicScanState
    data object WaitingForPermission : LocalMusicScanState
    data class Importing(val progress: LocalMusicScanProgress) : LocalMusicScanState
    data class Scanning(val progress: LocalMusicScanProgress) : LocalMusicScanState
    data class Done(val summary: LocalMusicLastScanSummary) : LocalMusicScanState
    data class Error(
        val error: LocalMusicScanError,
        val summary: LocalMusicLastScanSummary? = null,
    ) : LocalMusicScanState
}

/**
 * 曲库统计值，首页卡片和我的页共用。
 */
data class LibraryStats(
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
)

/**
 * UI 读取曲库的唯一快照，避免页面各自维护列表。
 */
data class LibrarySnapshot(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val stats: LibraryStats,
    val sources: List<LocalMusicSourceSummary>,
    val scanState: LocalMusicScanState,
    val lastScanSummary: LocalMusicLastScanSummary?,
    val problems: List<LocalMusicProblem>,
) {
    companion object {
        /**
         * 无来源时的空曲库快照，保证页面可以安全渲染入口状态。
         */
        val Empty: LibrarySnapshot = LibrarySnapshot(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            stats = LibraryStats(),
            sources = emptyList(),
            scanState = LocalMusicScanState.Idle,
            lastScanSummary = null,
            problems = emptyList(),
        )
    }
}
