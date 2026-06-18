package com.yanhao.kmpmusic.domain.usecase

/**
 * 本地扫描状态。
 */
enum class ScanStatus {
    Idle,
    Scanning,
    Done,
}

/**
 * 本地音乐扫描接口，后续由平台实现替换。
 */
interface ScanLocalMusicUseCase {
    /**
     * 触发本地扫描并返回阶段一模拟状态。
     */
    operator fun invoke(currentStatus: ScanStatus): ScanStatus
}

/**
 * 阶段一模拟扫描实现。
 */
class ScanLocalMusicUseCaseImpl : ScanLocalMusicUseCase {
    /** 按原型交互在扫描中和完成之间推进状态。 */
    override operator fun invoke(currentStatus: ScanStatus): ScanStatus {
        return when (currentStatus) {
            ScanStatus.Idle -> ScanStatus.Scanning
            ScanStatus.Scanning -> ScanStatus.Done
            ScanStatus.Done -> ScanStatus.Idle
        }
    }
}
