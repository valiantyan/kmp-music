package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import platform.Foundation.NSURL

/**
 * iOS 平台导入扫描器，在授权窗口内把 Files 音频复制到 App 沙盒后再发布曲库结果。
 */
class IosFolderMusicScanner internal constructor(
    // 文件夹选择入口，测试通过注入固定 URL 避免触发 UIKit。
    private val chooseFolder: suspend () -> NSURL,
    // 沙盒导入器，负责复制生命周期和本地 URI 收口。
    private val importer: IosSandboxAudioImporter,
    // 当前时间提供者，测试固定时间戳保证断言稳定。
    private val nowMillis: () -> Long,
) : LocalMusicScanner {
    /**
     * 真实 iOS 入口使用系统选择器和默认沙盒导入器。
     */
    constructor() : this(
        chooseFolder = { IosFolderPicker().chooseMusicFolder() },
        importer = IosSandboxAudioImporter(),
        nowMillis = { currentTimeMillis() },
    )

    /**
     * 测试入口直接注入文件系统边界，仍复用真实导入规则。
     */
    internal constructor(
        chooseFolder: suspend () -> NSURL,
        fileSystem: IosImportFileSystem,
        nowMillis: () -> Long,
    ) : this(
        chooseFolder = chooseFolder,
        importer = IosSandboxAudioImporter(fileSystem = fileSystem),
        nowMillis = nowMillis,
    )

    /** 弹出 iOS 文件夹选择器并扫描用户授权目录中的音频文件。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scan(
            request = request,
            preferences = LocalMusicDiscoveryPreferences(),
        )
    }

    /** 弹出 iOS 文件夹选择器并按偏好导入授权目录中的音频文件。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        validateRequest(request = request)
        val folderUrl: NSURL = chooseFolder()
        return importer.importFolder(
            folderUrl = folderUrl,
            completedAt = nowMillis(),
            preferences = preferences,
        )
    }

    // 只接受通用刷新或 iOS 文件来源请求，避免平台 scanner 被错误复用。
    private fun validateRequest(request: LocalMusicScanRequest) {
        if (request is LocalMusicScanRequest.Source && request.sourceKind != LocalMusicSourceKind.IosImportedFile) {
            throw LocalMusicScanException(
                error = LocalMusicScanError(
                    type = LocalMusicScanErrorType.FolderUnavailable,
                    message = "当前 iOS 端只能导入用户选择的音频文件",
                    sourceKind = LocalMusicSourceKind.IosImportedFile,
                ),
            )
        }
    }
}
