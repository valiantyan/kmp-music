package com.yanhao.kmpmusic.domain.model

/**
 * 本地自建歌单元信息，只保存当前应用内部的歌单事实。
 *
 * @property id App 内稳定歌单标识。
 * @property name 用户可见歌单名称，已按创建规则裁剪首尾空格。
 * @property createdAt 歌单创建时间。
 * @property updatedAt 歌单最近真实变更时间。
 */
data class LocalPlaylist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 歌单内保存的歌曲关系，歌曲实体仍由曲库仓库解析。
 *
 * @property playlistId 关系所属歌单标识。
 * @property songId 当前应用内歌曲标识。
 * @property addedAt 首次加入歌单时间。
 * @property sortOrder 首次加入时分配的稳定顺序。
 */
data class LocalPlaylistSong(
    val playlistId: String,
    val songId: String,
    val addedAt: Long,
    val sortOrder: Int,
)

/**
 * 歌单详情读取结果，同时保留全部关系和当前可用歌曲口径。
 *
 * @property playlist 歌单元信息。
 * @property relations 歌单内全部已保存关系，包含当前不可用歌曲。
 * @property availableSongs 当前曲库仍能解析出的可播放歌曲，按 [relations] 的最新添加顺序排列。
 */
data class LocalPlaylistDetail(
    val playlist: LocalPlaylist,
    val relations: List<LocalPlaylistSong>,
    val availableSongs: List<Song>,
)

/**
 * 创建歌单的领域错误，供 UI 层后续映射成用户可见文案。
 */
sealed class LocalPlaylistCreateResult {
    /**
     * 歌单创建成功。
     *
     * @property playlist 已保存的歌单元信息。
     */
    data class Success(
        val playlist: LocalPlaylist,
    ) : LocalPlaylistCreateResult()

    /** 裁剪后名称为空。 */
    data object BlankName : LocalPlaylistCreateResult()

    /** 与已有名称完全字符一致。 */
    data object DuplicateName : LocalPlaylistCreateResult()
}

/**
 * 新增歌曲到歌单的结果，区分真实新增和幂等命中。
 */
sealed class AddSongToLocalPlaylistResult {
    /**
     * 真实新增关系。
     *
     * @property relation 新保存的歌曲关系。
     */
    data class Added(
        val relation: LocalPlaylistSong,
    ) : AddSongToLocalPlaylistResult()

    /**
     * 目标关系已经存在，调用者仍可按成功处理。
     *
     * @property relation 已存在的歌曲关系。
     */
    data class AlreadyExists(
        val relation: LocalPlaylistSong,
    ) : AddSongToLocalPlaylistResult()

    /** 目标歌单不存在。 */
    data object PlaylistNotFound : AddSongToLocalPlaylistResult()

    /** 歌曲不是当前应用曲库中的可用歌曲。 */
    data object SongUnavailable : AddSongToLocalPlaylistResult()
}

/**
 * 批量删除本地自建歌单的结果。
 *
 * @property deletedCount 实际删除的歌单数量；不存在的标识不会计入。
 */
data class LocalPlaylistDeleteResult(
    val deletedCount: Int,
)

/**
 * 新建歌单并加入当前歌曲的原子流程结果。
 */
sealed class CreateLocalPlaylistWithSongResult {
    /**
     * 原子流程完成。
     *
     * @property playlist 已创建的歌单。
     * @property relation 已加入的新歌曲关系。
     */
    data class Success(
        val playlist: LocalPlaylist,
        val relation: LocalPlaylistSong,
    ) : CreateLocalPlaylistWithSongResult()

    /** 裁剪后名称为空。 */
    data object BlankName : CreateLocalPlaylistWithSongResult()

    /** 与已有名称完全字符一致。 */
    data object DuplicateName : CreateLocalPlaylistWithSongResult()

    /** 歌曲不是当前应用曲库中的可用歌曲。 */
    data object SongUnavailable : CreateLocalPlaylistWithSongResult()
}
