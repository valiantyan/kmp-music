package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState

// 首页扫描入口统一消费扫描状态，避免歌曲、专辑、歌手空态文案漂移。
internal fun localMusicScanActionLabel(scanState: LocalMusicScanState): String {
    return when (scanState) {
        LocalMusicScanState.Idle -> "扫描本地音乐"
        LocalMusicScanState.WaitingForPermission -> "继续授权"
        is LocalMusicScanState.Importing -> "取消扫描"
        is LocalMusicScanState.Scanning -> "取消扫描"
        is LocalMusicScanState.Done -> "重新扫描"
        is LocalMusicScanState.Cancelled -> "重新扫描"
        is LocalMusicScanState.Error -> localMusicScanErrorActionLabel(scanState = scanState)
    }
}

// 权限类错误需要区分普通重试和系统设置入口，避免重复触发无效弹窗。
private fun localMusicScanErrorActionLabel(scanState: LocalMusicScanState.Error): String {
    return when (scanState.error.type) {
        LocalMusicScanErrorType.PermissionDenied -> "继续授权"
        LocalMusicScanErrorType.PermissionPermanentlyDenied -> "打开权限设置"
        else -> "重试扫描"
    }
}
