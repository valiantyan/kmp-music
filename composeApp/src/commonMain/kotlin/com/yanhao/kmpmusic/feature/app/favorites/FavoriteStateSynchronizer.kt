package com.yanhao.kmpmusic.feature.app.favorites

import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.feature.app.LocalPlaylistDetailDisplayModel
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 把收藏切换结果同步到所有可见歌曲集合，保证 facade 不再承载列表投影细节。
 */
class FavoriteStateSynchronizer(
    // 收藏切换入口仍走用例，确保仓库事实只有一份。
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    // 收藏列表实体仍由上游解析，避免这里重复感知仓库或查询策略。
    private val favoriteSongsResolver: (likedSongIds: List<String>, preferredSongs: List<Song>) -> List<Song>,
    // 最近播放列表继续沿用既有真实历史规则，只把可见歌曲候选交给外部构建。
    private val recentSongsBuilder: (state: MusicAppUiState, songs: List<Song>) -> List<Song>,
) {
    /**
     * 切换单曲收藏后，统一回写所有共享歌曲来源，避免 UI 出现同 songId 多份状态不一致。
     */
    fun toggleFavorite(
        state: MusicAppUiState,
        songId: String,
    ): MusicAppUiState {
        val likedSongIds: List<String> = toggleFavoriteUseCase(songId = songId)
        val isLiked: Boolean = likedSongIds.contains(element = songId)
        val homePreview: List<Song> =
            state.homeLocalSongPreview.updateFavoriteFlag(
                songId = songId,
                isLiked = isLiked,
            )
        val localSongs: List<Song> =
            state.localSongs.updateFavoriteFlag(
                songId = songId,
                isLiked = isLiked,
            )
        val queueSnapshot: List<Song> =
            state.queueSongsSnapshot.updateFavoriteFlag(
                songId = songId,
                isLiked = isLiked,
            )
        val selectedLocalPlaylistDetail: LocalPlaylistDetailDisplayModel? = state.selectedLocalPlaylistDetail
        val updatedLocalPlaylistDetail: LocalPlaylistDetailDisplayModel? =
            selectedLocalPlaylistDetail?.copy(
                songs =
                    selectedLocalPlaylistDetail.songs.updateFavoriteFlag(
                        songId = songId,
                        isLiked = isLiked,
                    ),
            )
        val preferredSongs: List<Song> =
            homePreview +
                localSongs +
                queueSnapshot +
                updatedLocalPlaylistDetail.orEmptySongs() +
                state.favoriteSongs
        val favoriteSongs: List<Song> =
            resolveFavoriteSongs(
                likedSongIds = likedSongIds,
                songId = songId,
                isLiked = isLiked,
                currentFavoriteSongs = state.favoriteSongs,
                preferredSongs = preferredSongs,
            )
        val stateWithUpdatedCollections: MusicAppUiState =
            state.copy(
                likedSongIds = likedSongIds.toSet(),
                homeLocalSongPreview = homePreview,
                localSongs = localSongs,
                favoriteSongs = favoriteSongs,
                queueSongsSnapshot = queueSnapshot,
                selectedLocalPlaylistDetail = updatedLocalPlaylistDetail,
            )
        return stateWithUpdatedCollections.copy(
            recentSongs =
                recentSongsBuilder(
                    stateWithUpdatedCollections,
                    localSongs.ifEmpty { homePreview },
                ),
        )
    }

    // 已知歌曲足以覆盖收藏集合时，直接用内存投影，避免 500 次增删触发 500 次仓库回查。
    private fun resolveFavoriteSongs(
        likedSongIds: List<String>,
        songId: String,
        isLiked: Boolean,
        currentFavoriteSongs: List<Song>,
        preferredSongs: List<Song>,
    ): List<Song> {
        if (likedSongIds.isEmpty()) {
            return emptyList()
        }
        if (!isLiked) {
            return currentFavoriteSongs.filterNot { song: Song -> song.id == songId }
        }
        val knownFavoriteSongs: List<Song> =
            buildKnownFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = preferredSongs,
            )
        if (knownFavoriteSongs.size == likedSongIds.size) {
            return knownFavoriteSongs
        }
        return favoriteSongsResolver(likedSongIds, preferredSongs)
    }

    // 从当前 UI 已知歌曲里派生收藏列表，保持最近收藏顺序并强制收藏态为 true。
    private fun buildKnownFavoriteSongs(
        likedSongIds: List<String>,
        preferredSongs: List<Song>,
    ): List<Song> {
        val preferredSongsById: Map<String, Song> =
            preferredSongs
                .distinctBy { song: Song -> song.id }
                .associateBy { song: Song -> song.id }
        return likedSongIds.mapNotNull { likedSongId: String ->
            preferredSongsById[likedSongId]?.copy(isLiked = true)
        }
    }

    // 只复制被切换的一首歌，减少 500 条列表连续操作时的对象分配。
    private fun List<Song>.updateFavoriteFlag(
        songId: String,
        isLiked: Boolean,
    ): List<Song> {
        val songIndex: Int = indexOfFirst { song: Song -> song.id == songId }
        if (songIndex < 0 || this[songIndex].isLiked == isLiked) {
            return this
        }
        val updatedSongs: MutableList<Song> = toMutableList()
        updatedSongs[songIndex] = this[songIndex].copy(isLiked = isLiked)
        return updatedSongs
    }

    // 歌单详情未打开时不额外扩展收藏候选。
    private fun LocalPlaylistDetailDisplayModel?.orEmptySongs(): List<Song> = this?.songs.orEmpty()
}
