package com.yanhao.kmpmusic.domain.model

/**
 * 本地专辑模型，首页、搜索和详情页共享。
 *
 * @property id 专辑稳定标识。
 * @property title 专辑标题。
 * @property artist 专辑歌手。
 * @property songCount 专辑曲目数。
 * @property coverArt 封面资源标识。
 * @property coverImageUri 专辑首曲扫描封面 URI，缺失时使用 [coverArt]。
 * @property mood 专辑氛围标签。
 * @property year 发行年份。
 */
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val songCount: Int,
    val coverArt: CoverArt,
    val coverImageUri: String? = null,
    val mood: String,
    val year: String,
)
