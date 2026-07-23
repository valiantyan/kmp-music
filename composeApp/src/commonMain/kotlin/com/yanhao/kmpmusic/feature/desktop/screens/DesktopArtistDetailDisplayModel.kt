package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song

/**
 * Desktop 歌手详情的稳定页面投影。
 *
 * @property artist 当前选中的歌手；为空时显示不可用降级态。
 * @property songs 当前歌手的完整播放队列。
 * @property title hero 显示的歌手名称或不可用文案。
 * @property playAllLabel 播放全部按钮文案。
 * @property isPlaybackEnabled 是否允许创建新的歌手播放队列。
 * @property heroArtworkCandidates 按歌曲顺序排列的有效 hero 封面候选。
 * @property emptyMessage 曲目不可用时显示的说明。
 */
internal data class DesktopArtistDetailDisplayModel(
    val artist: Artist?,
    val songs: List<Song>,
    val title: String,
    val playAllLabel: String,
    val isPlaybackEnabled: Boolean,
    val heroArtworkCandidates: List<Song>,
    val emptyMessage: String?,
)

/** 使用详情页同一份歌手队列派生播放、空态和 hero 候选，避免视觉与动作数据分裂。 */
internal fun buildDesktopArtistDetailDisplayModel(
    artist: Artist?,
    songs: List<Song>,
): DesktopArtistDetailDisplayModel {
    val artistSongs: List<Song> =
        artist
            ?.let { selectedArtist: Artist ->
                songs.filter { song: Song -> song.artist == selectedArtist.name }
            }.orEmpty()
    return DesktopArtistDetailDisplayModel(
        artist = artist,
        songs = artistSongs,
        title = artist?.name ?: "歌手不可用",
        playAllLabel = "播放全部 (${artistSongs.size})",
        isPlaybackEnabled = artistSongs.isNotEmpty(),
        heroArtworkCandidates = artistSongs.filter(::isDesktopArtistHeroArtworkCandidate),
        emptyMessage = resolveDesktopArtistDetailEmptyMessage(artist = artist, songs = artistSongs),
    )
}

/** 歌曲有扫描 URI 或非默认资源封面时才能作为 hero 候选，默认图只在全部失败后使用。 */
private fun isDesktopArtistHeroArtworkCandidate(song: Song): Boolean = song.coverImageUri.isNullOrBlank().not() || song.coverArt != CoverArt.HeroLocalMusic

/** 区分缺失歌手与正常歌手没有曲目的空态，避免按钮禁用后没有原因。 */
private fun resolveDesktopArtistDetailEmptyMessage(
    artist: Artist?,
    songs: List<Song>,
): String? =
    when {
        artist == null -> "没有找到歌手信息"
        songs.isEmpty() -> "暂无歌曲"
        else -> null
    }
