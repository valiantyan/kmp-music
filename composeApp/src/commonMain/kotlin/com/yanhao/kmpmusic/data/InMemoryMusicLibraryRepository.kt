package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository
import com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultRequest
import com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultUseCase
import com.yanhao.kmpmusic.domain.usecase.MergeLocalMusicScanResultUseCaseImpl

/**
 * 内存曲库仓库，承接 fake scanner 和后续平台 scanner 的扫描结果。
 */
class InMemoryMusicLibraryRepository(
    private val mergeUseCase: MergeLocalMusicScanResultUseCase = MergeLocalMusicScanResultUseCaseImpl(),
) : MusicLibraryRepository {
    // 当前曲库快照，第一阶段不落盘。
    private var snapshot: LibrarySnapshot = LibrarySnapshot.Empty

    /** 获取当前曲库快照。 */
    override fun getSnapshot(): LibrarySnapshot = snapshot

    /** 合并扫描结果并更新内存快照。 */
    override fun applyScanResult(
        scanResult: LocalMusicScanResult,
        likedSongIds: Set<String>,
    ): LibrarySnapshot {
        snapshot = mergeUseCase(
            request = MergeLocalMusicScanResultRequest(
                previousSnapshot = snapshot,
                scanResult = scanResult,
                likedSongIds = likedSongIds,
            ),
        )
        return snapshot
    }
}
