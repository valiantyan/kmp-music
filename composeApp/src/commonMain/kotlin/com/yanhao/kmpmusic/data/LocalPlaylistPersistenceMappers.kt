package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.domain.model.LocalPlaylistSong
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistEntity
import com.yanhao.kmpmusic.domain.persistence.LocalPlaylistSongEntity

/** 映射数据库歌单实体到领域模型。 */
internal fun LocalPlaylistEntity.toDomain(): LocalPlaylist {
    return LocalPlaylist(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

/** 映射数据库关系实体到领域模型。 */
internal fun LocalPlaylistSongEntity.toDomain(): LocalPlaylistSong {
    return LocalPlaylistSong(
        playlistId = playlistId,
        songId = songId,
        addedAt = addedAt,
        sortOrder = sortOrder,
    )
}
