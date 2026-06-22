package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 本地音乐扫描接口，协调平台 scanner 和曲库快照合并。
 */
interface ScanLocalMusicUseCase {
    /**
     * 执行扫描并把结果写入曲库仓库。
     */
    suspend operator fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
    ): LibrarySnapshot
}

/**
 * 本地音乐扫描实现，保证 scanner 不直接驱动 Composable。
 */
class ScanLocalMusicUseCaseImpl(
    private val localMusicScanner: LocalMusicScanner,
    private val musicLibraryRepository: MusicLibraryRepository,
) : ScanLocalMusicUseCase {
    /** 扫描并合并为 UI 可读的 [LibrarySnapshot]。 */
    override suspend operator fun invoke(
        request: LocalMusicScanRequest,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        val result: LocalMusicScanResult = localMusicScanner.scan(request = request)
        return musicLibraryRepository.applyScanResult(
            scanResult = result,
            likedSongIds = likedSongIds,
        )
    }
}
