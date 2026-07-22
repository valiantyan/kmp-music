package com.yanhao.kmpmusic.feature.app

import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.PlaybackState
import com.yanhao.kmpmusic.domain.model.QueueState
import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.domain.repository.SearchHistoryRepository
import com.yanhao.kmpmusic.domain.repository.UserPreferencesRepository
import com.yanhao.kmpmusic.feature.app.library.LibraryStateSynchronizer

/**
 * 构建冷启动 UI 状态，避免 [MusicAppController] 同时承担仓库读取和初始投影职责。
 */
internal class MusicAppInitialStateBuilder(
    private val musicLibraryRepository: MusicLibraryRepository,
    private val playbackRepository: PlaybackRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val favoriteSongsBuilder: (likedSongIds: List<String>, preferredSongs: List<Song>) -> List<Song>,
    private val recentSongsBuilder: (state: MusicAppUiState, extraSongs: List<Song>) -> List<Song>,
) {
    /**
     * 按既有仓库顺序构建初始状态，避免冷启动额外读取完整曲库。
     */
    fun build(
        homePreview: List<Song>,
        initialLikedSongIds: List<String>,
    ): MusicAppUiState {
        val stats: LibraryStats = musicLibraryRepository.getLibraryStats()
        val playbackState: PlaybackState = playbackRepository.getPlaybackState()
        val queueState: QueueState = playbackRepository.getQueueState()
        val initialLikedSongIdSet: Set<String> = initialLikedSongIds.toSet()
        val previewWithLikes: List<Song> =
            homePreview.map { song: Song ->
                song.copy(isLiked = initialLikedSongIdSet.contains(element = song.id) || song.isLiked)
            }
        val initialScanState: LocalMusicScanState = buildInitialScanState(stats = stats)
        val baseState: MusicAppUiState =
            buildBaseState(
                previewWithLikes = previewWithLikes,
                initialLikedSongIds = initialLikedSongIdSet,
                playbackState = playbackState,
                queueState = queueState,
                stats = stats,
                initialScanState = initialScanState,
            )
        return baseState.copy(
            favoriteSongs = favoriteSongsBuilder(initialLikedSongIds, previewWithLikes),
            recentSongs = recentSongsBuilder(baseState, previewWithLikes),
            themeMode = userPreferencesRepository.getThemeMode(),
            localMusicDiscoveryPreferences = userPreferencesRepository.getLocalMusicDiscoveryPreferences(),
            localLibrarySearchHistory =
                searchHistoryRepository.getSearchHistory(
                    context = SearchContext.LocalLibrary,
                ),
            favoritesSearchHistory =
                searchHistoryRepository.getSearchHistory(
                    context = SearchContext.Favorites,
                ),
        )
    }

    // 持久层已有歌曲时，冷启动首页表达已有曲库，但不因此读取全量歌曲。
    private fun buildInitialScanState(stats: LibraryStats): LocalMusicScanState = LibraryStateSynchronizer.buildInitialScanStateFromStats(stats = stats)

    // 先构造最小状态，供收藏和最近播放投影复用同一份基础事实。
    private fun buildBaseState(
        previewWithLikes: List<Song>,
        initialLikedSongIds: Set<String>,
        playbackState: PlaybackState,
        queueState: QueueState,
        stats: LibraryStats,
        initialScanState: LocalMusicScanState,
    ): MusicAppUiState =
        MusicAppUiState(
            homeLocalSongPreview = previewWithLikes,
            localSongs = emptyList(),
            localAlbums = emptyList(),
            localArtists = emptyList(),
            favoriteSongs = emptyList(),
            queueSongsSnapshot = emptyList(),
            likedSongIds = initialLikedSongIds,
            currentSongId = playbackState.currentSongId,
            playbackStatus = playbackState.status,
            playbackPositionMs = playbackState.positionMs,
            playbackDurationMs = playbackState.durationMs,
            playbackMode = queueState.playbackMode,
            playbackError = playbackState.error,
            queueSongIds = queueState.songIds,
            libraryStats = stats,
            scanState = initialScanState,
        )
}
