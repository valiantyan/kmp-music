package com.yanhao.kmpmusic.domain.model

/**
 * 本地音乐歌曲模型，作为播放、搜索、收藏和队列的统一数据源。
 *
 * @property id 歌曲稳定标识。
 * @property title 歌曲标题。
 * @property artist 歌手名称。
 * @property album 所属专辑名称。
 * @property duration 展示用时长。
 * @property coverArt 原型封面资源标识。
 * @property isLiked 当前收藏状态。
 * @property lastPlayed 最近播放文案。
 * @property quality 本地音质标签。
 * @property lyric 播放页展示的短句。
 * @property trackNumber 专辑内曲序。
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val coverArt: CoverArt,
    val isLiked: Boolean,
    val lastPlayed: String,
    val quality: String,
    val lyric: String,
    val trackNumber: Int,
)
