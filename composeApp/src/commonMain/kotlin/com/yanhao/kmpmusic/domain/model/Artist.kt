package com.yanhao.kmpmusic.domain.model

/**
 * 歌手模型，保持与歌手详情和收藏歌手列表一致。
 *
 * @property id 歌手稳定标识。
 * @property name 歌手名称。
 * @property songCount 本地曲目数。
 * @property coverArt 歌手视觉资源标识。
 * @property tag 歌手分类标签。
 */
data class Artist(
    val id: String,
    val name: String,
    val songCount: Int,
    val coverArt: CoverArt,
    val tag: String,
)
