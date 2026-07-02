package com.yanhao.kmpmusic.domain.model

/**
 * 归一化歌手显示名，用于本地元数据轻微差异下仍能聚合到同一歌手。
 *
 * @param value 原始歌手名。
 * @return 去掉首尾空白、折叠连续空白并忽略英文大小写后的匹配键。
 */
fun normalizeArtistName(value: String): String {
    return value
        .trim()
        .replace(regex = ARTIST_NAME_WHITESPACE_REGEX, replacement = " ")
        .lowercase()
}

/**
 * 判断两个歌手名是否代表同一位歌手。
 *
 * @param firstName 第一个歌手显示名。
 * @param secondName 第二个歌手显示名。
 * @return 轻微空白和英文大小写差异被忽略后的相等结果。
 */
fun hasSameArtistName(firstName: String, secondName: String): Boolean {
    return normalizeArtistName(value = firstName) == normalizeArtistName(value = secondName)
}

/**
 * 判断歌曲是否归属于指定歌手。
 *
 * @param song 待判断歌曲。
 * @param artist 当前歌手。
 * @return 歌曲歌手名与当前歌手名归一化后是否一致。
 */
fun isSongByArtist(song: Song, artist: Artist): Boolean {
    return hasSameArtistName(
        firstName = song.artist,
        secondName = artist.name,
    )
}

private val ARTIST_NAME_WHITESPACE_REGEX: Regex = Regex(pattern = "\\s+")
