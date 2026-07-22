package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.Song

/**
 * Desktop 收藏页只保留 Figma 所需的歌曲数量、播放入口和空态投影。
 *
 * @property songs 当前收藏歌曲，顺序同时作为顺序播放队列。
 * @property songCountLabel 标题下方的动态歌曲数量。
 * @property playAllLabel 主按钮动态数量文案。
 * @property isPlaybackEnabled 是否存在可播放歌曲。
 * @property emptyMessage 空收藏时的列表提示。
 */
internal data class DesktopFavoritesDisplayModel(
    val songs: List<Song>,
    val songCountLabel: String,
    val playAllLabel: String,
    val isPlaybackEnabled: Boolean,
    val emptyMessage: String?,
)

/** 根据真实收藏歌曲建立稳定显示模型，避免页面层复制数量和空态分支。 */
internal fun buildDesktopFavoritesDisplayModel(songs: List<Song>): DesktopFavoritesDisplayModel {
    val favoriteSongs: List<Song> = songs.filter { song: Song -> song.isLiked }
    val songCount: Int = favoriteSongs.size
    return DesktopFavoritesDisplayModel(
        songs = favoriteSongs,
        songCountLabel = "$songCount 首歌曲",
        playAllLabel = "播放全部 ($songCount)",
        isPlaybackEnabled = favoriteSongs.isNotEmpty(),
        emptyMessage = if (favoriteSongs.isEmpty()) "暂无收藏歌曲" else null,
    )
}
