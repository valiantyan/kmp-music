package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.repository.FavoritesRepository

/**
 * 切换歌曲收藏状态接口。
 */
interface ToggleFavoriteUseCase {
    /**
     * 切换歌曲收藏状态并按最近收藏时间从新到旧返回歌曲标识。
     */
    operator fun invoke(songId: String): List<String>
}

/**
 * 切换歌曲收藏状态实现。
 */
class ToggleFavoriteUseCaseImpl(
    private val favoritesRepository: FavoritesRepository,
) : ToggleFavoriteUseCase {
    /** 委托仓库更新收藏状态，保持 UI 不感知存储细节。 */
    override operator fun invoke(songId: String): List<String> = favoritesRepository.toggleSong(songId = songId)
}
