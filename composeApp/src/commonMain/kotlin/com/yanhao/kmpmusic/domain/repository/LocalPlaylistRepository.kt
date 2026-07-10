package com.yanhao.kmpmusic.domain.repository

import com.yanhao.kmpmusic.domain.model.AddSongToLocalPlaylistResult
import com.yanhao.kmpmusic.domain.model.CreateLocalPlaylistWithSongResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.domain.model.LocalPlaylistCreateResult
import com.yanhao.kmpmusic.domain.model.LocalPlaylistDetail

/**
 * 本地自建歌单仓库契约，只保存歌单元信息和当前应用内歌曲标识关系。
 */
interface LocalPlaylistRepository {
    /**
     * 创建歌单元信息，用户可见入口仍由后续“新建并加入当前歌曲”流程承载。
     */
    fun createPlaylist(name: String): LocalPlaylistCreateResult

    /**
     * 新建歌单并加入当前歌曲，作为后续弹窗流程的原子保存边界。
     */
    fun createPlaylistWithSong(
        name: String,
        songId: String,
    ): CreateLocalPlaylistWithSongResult

    /**
     * 将当前应用曲库中的歌曲标识保存到指定歌单。
     */
    fun addSongToPlaylist(
        playlistId: String,
        songId: String,
    ): AddSongToLocalPlaylistResult

    /**
     * 按最近更新时间倒序读取歌单元信息。
     */
    fun getPlaylists(): List<LocalPlaylist>

    /**
     * 按名称搜索歌单，搜索规则不影响创建判重规则。
     */
    fun searchPlaylists(query: String): List<LocalPlaylist>

    /**
     * 生成可用默认歌单名称。
     */
    fun getNextDefaultPlaylistName(): String

    /**
     * 读取歌单详情，保留全部关系并过滤当前不可用歌曲。
     */
    fun getPlaylistDetail(playlistId: String): LocalPlaylistDetail?
}
