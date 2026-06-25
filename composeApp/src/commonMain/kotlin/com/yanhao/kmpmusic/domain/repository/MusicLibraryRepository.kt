package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 音乐库读取与扫描合并接口，支持首页轻量预览和二级页按需全量加载。
 */
interface MusicLibraryRepository {
    /**
     * 获取当前曲库快照，旧调用方兼容入口。
     */
    fun getSnapshot(): LibrarySnapshot

    /**
     * 冷启动首页最多读取 6 条本地歌曲预览。
     */
    fun getHomePreview(limit: Int = 6): List<Song>

    /**
     * 读取全部可用本地歌曲，供本地二级页、搜索和详情使用。
     */
    fun getAllAvailableSongs(): List<Song>

    /**
     * 按歌曲 id 读取当前仍可用的歌曲，供冷启动恢复和收藏投影按需补齐实体。
     */
    fun getAvailableSongsByIds(songIds: List<String>): List<Song>

    /**
     * 读取当前可用曲库统计。
     */
    fun getLibraryStats(): LibraryStats

    /**
     * 合并扫描结果并返回新的曲库快照。
     */
    fun applyScanResult(
        request: LocalMusicScanRequest,
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot

    /**
     * 获取本地歌曲，保留旧用例兼容性。
     */
    fun getSongs(): List<Song> = getAllAvailableSongs()

    /**
     * 获取本地专辑，保留旧用例兼容性。
     */
    fun getAlbums(): List<Album> = getSnapshot().albums

    /**
     * 获取本地歌手，保留旧用例兼容性。
     */
    fun getArtists(): List<Artist> = getSnapshot().artists
}
