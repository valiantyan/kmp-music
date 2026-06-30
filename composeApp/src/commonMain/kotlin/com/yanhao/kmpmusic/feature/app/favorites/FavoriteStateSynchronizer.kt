package com.yanhao.kmpmusic.feature.app.favorites

import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.usecase.ToggleFavoriteUseCase
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 把收藏切换结果同步到所有可见歌曲集合，保证 facade 不再承载列表投影细节。
 */
class FavoriteStateSynchronizer(
    // 收藏切换入口仍走用例，确保仓库事实只有一份。
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    // 收藏列表实体仍由上游解析，避免这里重复感知仓库或查询策略。
    private val favoriteSongsResolver: (likedSongIds: Set<String>, preferredSongs: List<Song>) -> List<Song>,
    // 最近播放列表继续沿用既有真实历史规则，只把可见歌曲候选交给外部构建。
    private val recentSongsBuilder: (state: MusicAppUiState, songs: List<Song>) -> List<Song>,
) {
    /**
     * 切换单曲收藏后，统一回写所有共享歌曲来源，避免 UI 出现同 songId 多份状态不一致。
     */
    fun toggleFavorite(state: MusicAppUiState, songId: String): MusicAppUiState {
        val likedSongIds: Set<String> = toggleFavoriteUseCase(songId = songId)
        val homePreview: List<Song> = state.homeLocalSongPreview.map { song: Song -> song.withFavorite(likedSongIds = likedSongIds) }
        val localSongs: List<Song> = state.localSongs.map { song: Song -> song.withFavorite(likedSongIds = likedSongIds) }
        val queueSnapshot: List<Song> = state.queueSongsSnapshot.map { song: Song -> song.withFavorite(likedSongIds = likedSongIds) }
        val favoriteSongs: List<Song> = favoriteSongsResolver(
            likedSongIds,
            homePreview + localSongs + queueSnapshot + state.favoriteSongs,
        )
        val stateWithUpdatedCollections: MusicAppUiState = state.copy(
            likedSongIds = likedSongIds,
            homeLocalSongPreview = homePreview,
            localSongs = localSongs,
            favoriteSongs = favoriteSongs,
            queueSongsSnapshot = queueSnapshot,
        )
        return stateWithUpdatedCollections.copy(
            recentSongs = recentSongsBuilder(
                stateWithUpdatedCollections,
                localSongs.ifEmpty { homePreview },
            ),
        )
    }

    // 用同一条规则覆盖可见歌曲源，避免每个集合单独写 liked 判断。
    private fun Song.withFavorite(likedSongIds: Set<String>): Song {
        return copy(isLiked = likedSongIds.contains(element = id))
    }
}
