package com.yanhao.kmpmusic.domain.usecase

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 本地扫描弹层旧状态，保留到首页 UI 切到 [LibrarySnapshot] 后移除。
 */
enum class ScanStatus {
    Idle,
    Scanning,
    Done,
}

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

    /**
     * 阶段兼容方法，避免 Task 4 提前改动首页扫描弹层交互。
     */
    operator fun invoke(currentStatus: ScanStatus): ScanStatus {
        return when (currentStatus) {
            ScanStatus.Idle -> ScanStatus.Scanning
            ScanStatus.Scanning -> ScanStatus.Done
            ScanStatus.Done -> ScanStatus.Idle
        }
    }
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
