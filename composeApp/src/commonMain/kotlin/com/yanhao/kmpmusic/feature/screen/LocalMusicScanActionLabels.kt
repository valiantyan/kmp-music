package com.yanhao.kmpmusic.feature.screen

import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind

/**
 * 本地音频发现入口所处平台，用平台参数显式选择文案，避免 common UI 猜测运行环境。
 */
enum class LocalMusicDiscoveryPlatform {
    Android,
    Desktop,
    Ios,
}

// 扫描入口统一消费扫描状态和平台语义，避免不同页面文案漂移。
internal fun localMusicScanActionLabel(
    scanState: LocalMusicScanState,
    platform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
): String =
    when (scanState) {
        LocalMusicScanState.Idle -> {
            platform.initialScanActionLabel()
        }

        LocalMusicScanState.WaitingForPermission -> {
            "继续授权"
        }

        is LocalMusicScanState.Importing -> {
            "取消扫描"
        }

        is LocalMusicScanState.Scanning -> {
            "取消扫描"
        }

        is LocalMusicScanState.Done -> {
            "重新扫描"
        }

        is LocalMusicScanState.Cancelled -> {
            "重新扫描"
        }

        is LocalMusicScanState.Error -> {
            localMusicScanErrorActionLabel(
                scanState = scanState,
                platform = platform,
            )
        }
    }

// 来源类型展示名按平台模型收敛，避免把内部 sourceKind 名称直接暴露给用户。
internal fun localMusicSourceKindLabel(
    sourceKind: LocalMusicSourceKind,
    platform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
): String =
    when (sourceKind) {
        LocalMusicSourceKind.AndroidMediaStore -> {
            "Android 媒体库"
        }

        LocalMusicSourceKind.DesktopFolder -> {
            if (platform == LocalMusicDiscoveryPlatform.Desktop) {
                "扫描目录"
            } else {
                sourceKind.displayName
            }
        }

        LocalMusicSourceKind.IosImportedFile -> {
            if (platform == LocalMusicDiscoveryPlatform.Ios) {
                "已添加音频"
            } else {
                sourceKind.displayName
            }
        }

        LocalMusicSourceKind.IosMediaLibrary,
        LocalMusicSourceKind.FakeScanner,
        -> {
            sourceKind.displayName
        }
    }

// 空曲库入口根据平台真实授权模型表达第一步动作。
private fun LocalMusicDiscoveryPlatform.initialScanActionLabel(): String =
    when (this) {
        LocalMusicDiscoveryPlatform.Android -> "开始扫描"
        LocalMusicDiscoveryPlatform.Desktop -> "添加文件夹"
        LocalMusicDiscoveryPlatform.Ios -> "导入音频"
    }

// 权限类错误需要区分普通重试和系统设置入口，避免重复触发无效弹窗。
private fun localMusicScanErrorActionLabel(
    scanState: LocalMusicScanState.Error,
    platform: LocalMusicDiscoveryPlatform,
): String =
    when (scanState.error.type) {
        LocalMusicScanErrorType.PermissionDenied -> {
            "继续授权"
        }

        LocalMusicScanErrorType.PermissionPermanentlyDenied -> {
            "打开权限设置"
        }

        else -> {
            if (platform == LocalMusicDiscoveryPlatform.Ios) {
                "扫描曲库"
            } else {
                "重新扫描"
            }
        }
    }
