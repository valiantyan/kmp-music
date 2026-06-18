package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 音乐库读取接口，后续真实本地扫描会在 data 层替换当前 seed 实现。
 */
interface MusicLibraryRepository {
    /**
     * 获取本地歌曲。
     */
    fun getSongs(): List<Song>

    /**
     * 获取本地专辑。
     */
    fun getAlbums(): List<Album>

    /**
     * 获取本地歌手。
     */
    fun getArtists(): List<Artist>
}
