package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.graphics.Color
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.PlaybackStatus
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.isSongByArtist

/**
 * 歌手详情页渲染内容，隔离播放入口下展示全部歌手歌曲的页面规则。
 *
 * @property artistSongs 当前歌手名下全部歌曲。
 * @property playAllText 主行动按钮文案。
 * @property playAllCountText 主行动按钮右侧的歌曲数量文案。
 * @property songRows 歌曲列表逐行样式和交互状态。
 */
internal data class ArtistDetailContent(
    val artistSongs: List<Song>,
    val playAllText: String,
    val playAllCountText: String,
    val songRows: List<ArtistDetailSongRowState>,
)

/**
 * 歌手详情页歌曲行状态。
 *
 * @property song 行内歌曲。
 * @property indexLabel 两位序号文案。
 * @property titleColor 歌名颜色。
 * @property metaColor 歌手和时长颜色。
 * @property indexColor 序号颜色。
 * @property containerColor 行背景色。
 * @property showsPlaybackAnimation 是否显示播放动画；歌手详情页固定关闭。
 */
internal data class ArtistDetailSongRowState(
    val song: Song,
    val indexLabel: String,
    val titleColor: Color,
    val metaColor: Color,
    val indexColor: Color,
    val containerColor: Color,
    val showsPlaybackAnimation: Boolean,
)

/**
 * 歌手详情页歌曲行点击动作。
 */
internal enum class ArtistDetailSongClickAction {
    PlaySong,
    ToggleCurrentPlayback,
}

// Figma 歌手详情页正文主文字色。
internal val artistDetailTextColor: Color = Color(0xFF191C1D)

// Figma 歌手详情页正文次级文字色。
internal val artistDetailMetaColor: Color = Color(0xFF3D4947)

// Figma 歌手详情页弱化序号色。
internal val artistDetailMutedIndexColor: Color = Color(0x803D4947)

// Figma 歌手详情页当前歌曲卡片色。
internal val artistDetailActiveRowColor: Color = Color.White.copy(alpha = 0.52f)

// 普通歌曲行保持透明，让沉浸背景透出。
internal val artistDetailTransparentRowColor: Color = Color.Transparent

/**
 * 构建歌手详情页内容，确保播放入口下仍展示当前歌手全部歌曲。
 */
internal fun buildArtistDetailContent(
    artist: Artist,
    songs: List<Song>,
    currentSongId: String?,
    currentPlaybackStatus: PlaybackStatus,
): ArtistDetailContent {
    val artistSongs: List<Song> =
        songs.filter { song: Song ->
            isSongByArtist(
                song = song,
                artist = artist,
            )
        }
    val songRows: List<ArtistDetailSongRowState> =
        artistSongs.mapIndexed { index: Int, song: Song ->
            buildArtistDetailSongRowState(
                index = index,
                song = song,
                isCurrentSong = song.id == currentSongId,
                currentPlaybackStatus = currentPlaybackStatus,
            )
        }
    return ArtistDetailContent(
        artistSongs = artistSongs,
        playAllText = "播放全部",
        playAllCountText = "${artistSongs.size} 首歌曲",
        songRows = songRows,
    )
}

/**
 * 判断行主体点击应切歌还是切换当前播放状态。
 */
internal fun resolveArtistDetailSongClickAction(
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
): ArtistDetailSongClickAction {
    if (!isCurrentSong) {
        return ArtistDetailSongClickAction.PlaySong
    }
    return when {
        shouldShowArtistDetailPauseControl(currentPlaybackStatus = currentPlaybackStatus) -> ArtistDetailSongClickAction.ToggleCurrentPlayback
        currentPlaybackStatus == PlaybackStatus.Paused -> ArtistDetailSongClickAction.ToggleCurrentPlayback
        else -> ArtistDetailSongClickAction.PlaySong
    }
}

// 与全局播放控制保持一致：正在启动、缓冲和播放中都可通过当前歌曲行暂停。
private fun shouldShowArtistDetailPauseControl(currentPlaybackStatus: PlaybackStatus): Boolean =
    currentPlaybackStatus == PlaybackStatus.Loading ||
        currentPlaybackStatus == PlaybackStatus.Buffering ||
        currentPlaybackStatus == PlaybackStatus.Playing

// 歌手详情页行样式只用颜色表达播放态，不显示等化器动画。
private fun buildArtistDetailSongRowState(
    index: Int,
    song: Song,
    isCurrentSong: Boolean,
    currentPlaybackStatus: PlaybackStatus,
): ArtistDetailSongRowState {
    val isCurrentSongPlaying: Boolean = isCurrentSong && currentPlaybackStatus == PlaybackStatus.Playing
    val textColor: Color = if (isCurrentSongPlaying) MusicColors.PlayingRed else artistDetailTextColor
    val metaColor: Color = if (isCurrentSongPlaying) MusicColors.PlayingRed else artistDetailMetaColor
    val indexColor: Color = if (isCurrentSongPlaying) MusicColors.PlayingRed else artistDetailMutedIndexColor
    val containerColor: Color = if (isCurrentSong) artistDetailActiveRowColor else artistDetailTransparentRowColor
    return ArtistDetailSongRowState(
        song = song,
        indexLabel = (index + 1).toString().padStart(length = 2, padChar = '0'),
        titleColor = textColor,
        metaColor = metaColor,
        indexColor = indexColor,
        containerColor = containerColor,
        showsPlaybackAnimation = false,
    )
}
