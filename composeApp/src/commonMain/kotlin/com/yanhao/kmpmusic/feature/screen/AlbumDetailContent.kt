package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.isSongInAlbum

/**
 * 专辑详情页渲染内容，隔离专辑歌曲列表和播放队列的页面规则。
 *
 * @property albumSongs 当前专辑下按曲序排列的全部歌曲。
 * @property playAllText 主行动按钮文案。
 * @property playAllCountText 主行动按钮右侧的歌曲数量文案。
 */
internal data class AlbumDetailContent(
    val albumSongs: List<Song>,
    val playAllText: String,
    val playAllCountText: String,
)

/**
 * 专辑详情页歌曲行状态。
 *
 * @property song 行内歌曲。
 * @property indexLabel 两位序号文案。
 * @property titleColor 歌名颜色。
 * @property metaColor 歌手和专辑颜色。
 * @property indexColor 序号颜色。
 * @property durationColor 时长颜色。
 * @property showsPlaybackGlyph 是否显示当前播放辅助标识。
 */
internal data class AlbumDetailSongRowState(
    val song: Song,
    val indexLabel: String,
    val titleColor: Color,
    val metaColor: Color,
    val indexColor: Color,
    val durationColor: Color,
    val showsPlaybackGlyph: Boolean,
)

/**
 * 专辑详情页歌曲行点击动作。
 */
internal enum class AlbumDetailSongClickAction {
    PlaySong,
    ToggleCurrentPlayback,
}

// Figma 专辑详情页正文主文字色。
internal val albumDetailTextColor: Color = Color(0xFF191C1D)

// Figma 专辑详情页正文次级文字色。
internal val albumDetailMetaColor: Color = Color(0xFF3D4947)

// Figma 专辑详情页弱化序号和操作色。
internal val albumDetailMutedColor: Color = Color(0x993D4947)

// Figma 专辑详情页主按钮色。
internal val albumDetailActionColor: Color = Color(0xFF26A69A)

// Figma 专辑详情页背景色。
internal val albumDetailBackgroundColor: Color = Color.White

/**
 * 构建专辑详情页内容，保证播放入口和单曲点击使用同一份专辑播放队列。
 */
internal fun buildAlbumDetailContent(
    album: Album,
    songs: List<Song>,
    demoSongCount: Int = 0,
): AlbumDetailContent {
    val albumSongs: List<Song> = buildAlbumDetailSongs(
        album = album,
        songs = songs,
        demoSongCount = demoSongCount,
    )
    return buildAlbumDetailContent(albumSongs = albumSongs)
}

/**
 * 构建专辑详情页稳定歌曲队列，让播放态变化时不重复做专辑过滤和 demo 生成。
 */
internal fun buildAlbumDetailSongs(
    album: Album,
    songs: List<Song>,
    demoSongCount: Int = 0,
): List<Song> {
    val realAlbumSongs: List<Song> = songs
        .filter { song: Song -> isSongInAlbum(song = song, album = album) }
        .sortedBy { song: Song -> albumTrackSortKey(song = song) }
    return appendAlbumDetailDemoSongs(
        album = album,
        albumSongs = realAlbumSongs,
        demoSongCount = demoSongCount,
    )
}

/**
 * 根据稳定专辑队列构建内容文案，不在这里预构建 500 条行状态。
 */
internal fun buildAlbumDetailContent(albumSongs: List<Song>): AlbumDetailContent {
    return AlbumDetailContent(
        albumSongs = albumSongs,
        playAllText = "播放全部",
        playAllCountText = "${albumSongs.size}首",
    )
}

/**
 * 判断行主体点击应切歌还是切换当前播放状态。
 */
internal fun resolveAlbumDetailSongClickAction(
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
): AlbumDetailSongClickAction {
    if (!isCurrentSong) {
        return AlbumDetailSongClickAction.PlaySong
    }
    return when {
        shouldShowAlbumDetailPauseControl(currentPlaybackStatus = currentPlaybackStatus) -> AlbumDetailSongClickAction.ToggleCurrentPlayback
        currentPlaybackStatus == PlaybackStatus.Paused -> AlbumDetailSongClickAction.ToggleCurrentPlayback
        else -> AlbumDetailSongClickAction.PlaySong
    }
}

// 未知或非法曲序放到末尾，避免它们挤到专辑第一首之前。
private fun albumTrackSortKey(song: Song): Int {
    if (song.trackNumber > 0) {
        return song.trackNumber
    }
    return Int.MAX_VALUE
}

// 与全局播放控制保持一致：正在启动、缓冲和播放中都可通过当前歌曲行暂停。
private fun shouldShowAlbumDetailPauseControl(currentPlaybackStatus: PlaybackStatus): Boolean {
    return currentPlaybackStatus == PlaybackStatus.Loading ||
        currentPlaybackStatus == PlaybackStatus.Buffering ||
        currentPlaybackStatus == PlaybackStatus.Playing
}

// 专辑详情页遵循全局规则：当前歌曲在列表中使用红色文本，并显示辅助标识。
internal fun buildAlbumDetailSongRowState(
    index: Int,
    song: Song,
    isCurrentSong: Boolean,
): AlbumDetailSongRowState {
    val activeColor: Color = if (isCurrentSong) MusicColors.PlayingRed else albumDetailTextColor
    val secondaryColor: Color = if (isCurrentSong) MusicColors.PlayingRed else albumDetailMetaColor
    val indexColor: Color = if (isCurrentSong) MusicColors.PlayingRed else albumDetailMutedColor
    return AlbumDetailSongRowState(
        song = song,
        indexLabel = (index + 1).toString().padStart(length = 2, padChar = '0'),
        titleColor = activeColor,
        metaColor = secondaryColor,
        indexColor = indexColor,
        durationColor = secondaryColor,
        showsPlaybackGlyph = isCurrentSong,
    )
}
