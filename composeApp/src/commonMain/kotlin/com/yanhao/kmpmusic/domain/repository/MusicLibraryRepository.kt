package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 音乐库读取与扫描合并接口，UI 只读取统一的 [LibrarySnapshot]。
 */
interface MusicLibraryRepository {
    /**
     * 获取当前曲库快照。
     */
    fun getSnapshot(): LibrarySnapshot

    /**
     * 合并扫描结果并返回新的曲库快照。
     */
    fun applyScanResult(
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot

    /**
     * 获取本地歌曲，保留旧用例兼容性。
     */
    fun getSongs(): List<Song> = getSnapshot().songs

    /**
     * 获取本地专辑，保留旧用例兼容性。
     */
    fun getAlbums(): List<Album> = getSnapshot().albums

    /**
     * 获取本地歌手，保留旧用例兼容性。
     */
    fun getArtists(): List<Artist> = getSnapshot().artists
}
