package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.screen.buildAlbumDetailSongs

/**
 * Desktop 专辑详情所需的稳定页面投影。
 *
 * @property album 当前选择的专辑；为空时渲染不可用占位。
 * @property songs 按专辑曲序排列的完整播放队列。
 * @property title 头部标题。
 * @property artist 头部歌手文案。
 * @property playAllLabel 主播放按钮文案。
 * @property isPlaybackEnabled 是否允许建立新的专辑播放队列。
 */
internal data class DesktopAlbumDetailDisplayModel(
    val album: Album?,
    val songs: List<Song>,
    val title: String,
    val artist: String,
    val playAllLabel: String,
    val isPlaybackEnabled: Boolean,
)

/** 使用移动端已验证的曲序规则建立 Desktop 详情投影，保证列表和播放队列一致。 */
internal fun buildDesktopAlbumDetailDisplayModel(
    album: Album?,
    songs: List<Song>,
): DesktopAlbumDetailDisplayModel {
    val albumSongs: List<Song> =
        album
            ?.let { selectedAlbum: Album ->
                buildAlbumDetailSongs(
                    album = selectedAlbum,
                    songs = songs,
                )
            }.orEmpty()
    return DesktopAlbumDetailDisplayModel(
        album = album,
        songs = albumSongs,
        title = album?.title ?: "专辑不可用",
        artist = album?.artist ?: "没有找到专辑信息",
        playAllLabel = "播放全部 | ${albumSongs.size}首",
        isPlaybackEnabled = albumSongs.isNotEmpty(),
    )
}
