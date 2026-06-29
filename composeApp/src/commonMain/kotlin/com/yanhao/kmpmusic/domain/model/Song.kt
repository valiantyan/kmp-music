package com.yanhao.kmpmusic.domain.model

/**
 * 本地音乐歌曲模型，作为播放、搜索、收藏和队列的统一数据源。
 *
 * @property id App 内稳定歌曲标识，真实扫描歌曲由 sourceKey 派生。
 * @property title 歌曲标题。
 * @property artist 歌手名称。
 * @property album 所属专辑名称。
 * @property duration 展示用时长。
 * @property coverArt 原型或扫描封面资源标识。
 * @property coverImageUri 扫描音频提取出的封面图片 URI，缺失时使用 [coverArt]。
 * @property isLiked 当前收藏状态。
 * @property lastPlayed 最近播放文案。
 * @property quality 本地音质标签。
 * @property lyric 播放页展示的短句。
 * @property trackNumber 专辑内曲序。
 * @property durationMs 真实音频毫秒时长，缺失时为 null。
 * @property sourceId 平台来源侧稳定标识。
 * @property sourceKind 平台来源类型。
 * @property localUri 平台 scanner 生成的本地媒体 URI 或路径。
 * @property mimeType 音频 MIME 类型。
 * @property sizeBytes 文件大小。
 * @property modifiedAt 文件或媒体条目的修改时间戳。
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val coverArt: CoverArt,
    val coverImageUri: String? = null,
    val isLiked: Boolean,
    val lastPlayed: String,
    val quality: String,
    val lyric: String,
    val trackNumber: Int,
    val durationMs: Long? = null,
    val sourceId: String = id,
    val sourceKind: LocalMusicSourceKind = LocalMusicSourceKind.FakeScanner,
    val localUri: String = "",
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val modifiedAt: Long? = null,
) {
    /**
     * 只有 scanner 或 importer 生成了 localUri，歌曲才进入可播放列表。
     */
    val isPlayable: Boolean
        get() = localUri.isNotBlank()
}
