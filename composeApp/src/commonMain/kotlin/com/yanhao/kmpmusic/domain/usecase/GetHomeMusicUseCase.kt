package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 首页音乐数据聚合结果。
 */
data class HomeMusic(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

/**
 * 获取首页音乐库数据的接口。
 */
interface GetHomeMusicUseCase {
    /**
     * 读取首页所需歌曲、专辑和歌手数据。
     */
    operator fun invoke(): HomeMusic
}

/**
 * 获取首页音乐库数据的实现。
 */
class GetHomeMusicUseCaseImpl(
    private val musicLibraryRepository: MusicLibraryRepository,
) : GetHomeMusicUseCase {
    /** 汇总首页所需数据，避免 UI 直接碰仓库。 */
    override operator fun invoke(): HomeMusic {
        return HomeMusic(
            songs = musicLibraryRepository.getSongs(),
            albums = musicLibraryRepository.getAlbums(),
            artists = musicLibraryRepository.getArtists(),
        )
    }
}
