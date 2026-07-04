package com.yanhao.kmpmusic.domain.model

/**
 * 归一化专辑显示名，用于本地元数据轻微差异下仍能聚合到同一专辑。
 *
 * @param value 原始专辑名。
 * @return 去掉首尾空白并忽略英文大小写后的匹配键。
 */
fun normalizeAlbumTitle(value: String): String {
    return value.trim().lowercase()
}

/**
 * 判断两个专辑名是否代表同一张专辑。
 *
 * @param firstTitle 第一个专辑显示名。
 * @param secondTitle 第二个专辑显示名。
 * @return 轻微空白和英文大小写差异被忽略后的相等结果。
 */
fun hasSameAlbumTitle(firstTitle: String, secondTitle: String): Boolean {
    return normalizeAlbumTitle(value = firstTitle) == normalizeAlbumTitle(value = secondTitle)
}

/**
 * 判断歌曲是否归属于指定专辑。
 *
 * @param song 待判断歌曲。
 * @param album 当前专辑。
 * @return 歌曲专辑名与当前专辑名归一化后是否一致。
 */
fun isSongInAlbum(song: Song, album: Album): Boolean {
    return hasSameAlbumTitle(
        firstTitle = song.album,
        secondTitle = album.title,
    )
}
