package com.yanhao.kmpmusic.feature.app.library

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicLastScanSummary
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.FavoritesRepository
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.repository.PlaybackRepository
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

/**
 * 把曲库仓库事实同步成不可变的 App UI 状态，避免 facade 直接承载列表推导细节。
 */
class LibraryStateSynchronizer(
    private val musicLibraryRepository: MusicLibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackRepository: PlaybackRepository,
) {
    companion object {
        /** 持久层已有歌曲时，冷启动只表达“已有曲库”状态，不触发全量歌曲读取。 */
        fun buildInitialScanStateFromStats(stats: LibraryStats): LocalMusicScanState {
            if (stats.songCount <= 0) {
                return LocalMusicScanState.Idle
            }
            return LocalMusicScanState.Done(
                summary = LocalMusicLastScanSummary(
                    addedCount = stats.songCount,
                    updatedCount = 0,
                    removedCount = 0,
                    problemCount = 0,
                    completedAt = 0L,
                ),
            )
        }
    }

    /** 永久拒绝权限后，二次扫描前必须先要求用户确认是否跳转系统设置。 */
    fun shouldConfirmPermissionSettingsBeforeScan(state: MusicAppUiState): Boolean {
        val scanState: LocalMusicScanState = state.scanState
        return scanState is LocalMusicScanState.Error &&
            scanState.error.type == LocalMusicScanErrorType.PermissionPermanentlyDenied
    }

    /** 为 facade 暴露冷启动扫描态构造入口，统一复用同一条规则。 */
    fun buildInitialScanState(stats: LibraryStats): LocalMusicScanState {
        return buildInitialScanStateFromStats(stats = stats)
    }

    /** 扫描后同步首页预览、完整曲库、收藏和最近播放这些共享列表。 */
    fun syncLibrarySnapshot(
        state: MusicAppUiState,
        snapshot: LibrarySnapshot,
    ): MusicAppUiState {
        val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
        val previewWithLikes: List<Song> = musicLibraryRepository.getHomePreview(limit = 6).map { song: Song ->
            song.copy(isLiked = likedSongIds.contains(element = song.id) || song.isLiked)
        }
        val shouldRefreshFullLibrary: Boolean = state.localSongs.isNotEmpty() ||
            state.navigationState.secondaryScreen is SecondaryScreen.LocalMusic
        val fullSongsWithLikes: List<Song> = if (shouldRefreshFullLibrary) {
            musicLibraryRepository.getAllAvailableSongs().map { song: Song ->
                song.copy(isLiked = likedSongIds.contains(element = song.id) || song.isLiked)
            }
        } else {
            state.localSongs
        }
        return state.copy(
            homeLocalSongPreview = previewWithLikes,
            localSongs = fullSongsWithLikes,
            localAlbums = if (shouldRefreshFullLibrary) {
                MusicLibraryProjector.buildAlbums(songs = fullSongsWithLikes)
            } else {
                state.localAlbums
            },
            localArtists = if (shouldRefreshFullLibrary) {
                MusicLibraryProjector.buildArtists(songs = fullSongsWithLikes)
            } else {
                state.localArtists
            },
            libraryStats = musicLibraryRepository.getLibraryStats(),
            localMusicSources = snapshot.sources,
            localMusicProblems = snapshot.problems,
            scanState = snapshot.scanState,
            likedSongIds = likedSongIds + previewWithLikes
                .filter { song: Song -> song.isLiked }
                .map { song: Song -> song.id },
            recentSongs = buildRecentSongs(
                state = state,
                extraSongs = fullSongsWithLikes + previewWithLikes,
            ),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = previewWithLikes + fullSongsWithLikes + state.queueSongsSnapshot + state.favoriteSongs,
            ),
        )
    }

    /** 按需加载完整曲库，避免首页冷启动就读取全部本地歌曲。 */
    fun loadLocalMusicLibrary(state: MusicAppUiState): MusicAppUiState {
        if (state.localSongs.isNotEmpty()) {
            return state
        }
        val likedSongIds: Set<String> = favoritesRepository.getLikedSongIds()
        val songsWithLikes: List<Song> = musicLibraryRepository.getAllAvailableSongs().map { song: Song ->
            song.copy(isLiked = likedSongIds.contains(element = song.id) || song.isLiked)
        }
        return state.copy(
            localSongs = songsWithLikes,
            localAlbums = MusicLibraryProjector.buildAlbums(songs = songsWithLikes),
            localArtists = MusicLibraryProjector.buildArtists(songs = songsWithLikes),
            favoriteSongs = buildFavoriteSongs(
                likedSongIds = likedSongIds,
                preferredSongs = state.homeLocalSongPreview + songsWithLikes + state.queueSongsSnapshot + state.favoriteSongs,
            ),
            likedSongIds = likedSongIds + songsWithLikes
                .filter { song: Song -> song.isLiked }
                .map { song: Song -> song.id },
            recentSongs = buildRecentSongs(
                state = state,
                extraSongs = songsWithLikes,
            ),
        )
    }

    /** 最近播放只应来自真实播放历史，并优先复用当前已知歌曲实体。 */
    fun buildRecentSongs(
        state: MusicAppUiState,
        extraSongs: List<Song> = emptyList(),
    ): List<Song> {
        val songs: List<Song> = extraSongs +
            state.queueSongsSnapshot +
            state.localSongs +
            state.homeLocalSongPreview +
            state.favoriteSongs
        val songsById: Map<String, Song> = songs
            .distinctBy { song: Song -> song.id }
            .associateBy { song: Song -> song.id }
        return playbackRepository.getPlaybackHistory().songIds
            .mapNotNull { songId: String -> songsById[songId] }
    }

    /** 收藏列表需要按 id 补齐实体并强制回写 liked 状态。 */
    fun buildFavoriteSongs(
        likedSongIds: Set<String>,
        preferredSongs: List<Song>,
    ): List<Song> {
        return resolveAvailableSongsByIds(
            songIds = likedSongIds.toList(),
            preferredSongs = preferredSongs,
        ).map { song: Song ->
            song.copy(isLiked = true)
        }
    }

    /** 先复用当前已知歌曲对象，再补查仓库，避免同一首歌出现多份不同实例。 */
    fun resolveAvailableSongsByIds(
        songIds: List<String>,
        preferredSongs: List<Song>,
    ): List<Song> {
        if (songIds.isEmpty()) {
            return emptyList()
        }
        val preferredById: Map<String, Song> = preferredSongs.associateBy { song: Song -> song.id }
        val fetchedSongs: List<Song> = musicLibraryRepository.getAvailableSongsByIds(songIds = songIds)
        val fetchedSongIds: Set<String> = fetchedSongs.map { song: Song -> song.id }.toSet()
        return (
            fetchedSongs.map { song: Song -> preferredById[song.id] ?: song } +
                preferredSongs.filter { song: Song ->
                    songIds.contains(element = song.id) && !fetchedSongIds.contains(element = song.id)
                }
            )
            .distinctBy { song: Song -> song.id }
    }
}
