package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LibrarySnapshot
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.Song
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

    /** 返回首页本地歌曲预览，保证旧内存数据源也遵守新读取边界。 */
    override fun getHomePreview(limit: Int): List<Song> {
        return snapshot.songs.take(n = limit)
    }

    /** 返回当前全部可用歌曲，保持搜索和详情的旧读取行为。 */
    override fun getAllAvailableSongs(): List<Song> {
        return snapshot.songs
    }

    /** 返回当前曲库统计，避免调用方自行推导。 */
    override fun getLibraryStats(): LibraryStats {
        return snapshot.stats
    }

    /** 合并扫描结果并更新内存快照。 */
    override fun applyScanResult(
        request: LocalMusicScanRequest,
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
