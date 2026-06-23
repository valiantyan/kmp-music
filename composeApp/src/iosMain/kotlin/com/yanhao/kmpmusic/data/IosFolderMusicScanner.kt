package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.posix.time

/**
 * iOS 平台文件夹扫描器，由用户选择文件夹后递归发现可播放音频。
 */
class IosFolderMusicScanner : LocalMusicScanner {
    // 系统文件夹选择器封装，隔离 UIKit delegate 生命周期细节。
    private val folderPicker: IosFolderPicker = IosFolderPicker()

    /** 弹出 iOS 文件夹选择器并扫描用户授权目录中的音频文件。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        validateRequest(request = request)
        val folderUrl: NSURL = folderPicker.chooseMusicFolder()
        return scanFolder(folderUrl = folderUrl)
    }

    // 只接受通用刷新或 iOS 文件来源请求，避免平台 scanner 被错误复用。
    private fun validateRequest(request: LocalMusicScanRequest) {
        if (request is LocalMusicScanRequest.Source && request.sourceKind != LocalMusicSourceKind.IosImportedFile) {
            throw LocalMusicScanException(
                error = LocalMusicScanError(
                    type = LocalMusicScanErrorType.FolderUnavailable,
                    message = "当前 iOS 端只能扫描用户选择的音乐文件夹",
                    sourceKind = LocalMusicSourceKind.IosImportedFile,
                ),
            )
        }
    }

    // 递归扫描 security-scoped 文件夹，扫描结束后释放授权。
    private fun scanFolder(folderUrl: NSURL): LocalMusicScanResult {
        val hasAccess: Boolean = folderUrl.startAccessingSecurityScopedResource()
        try {
            val folderPath: String = folderUrl.path ?: throw unavailableFolderException(folderUrl = folderUrl)
            return scanFolderPath(folderPath = folderPath, folderName = folderUrl.lastPathComponent ?: "iOS 文件夹")
        } finally {
            if (hasAccess) {
                folderUrl.stopAccessingSecurityScopedResource()
            }
        }
    }

    // 遍历文件夹子路径，忽略非音频文件并返回真实扫描结果。
    private fun scanFolderPath(folderPath: String, folderName: String): LocalMusicScanResult {
        val subpaths: List<*> = NSFileManager.defaultManager.subpathsAtPath(path = folderPath)
            ?: throw unavailableFolderPathException(folderPath = folderPath)
        val completedAt: Long = currentTimeMillis()
        val discovered: List<MusicFileMetadata> = subpaths.mapNotNull { subpath ->
            val relativePath: String = subpath as? String ?: return@mapNotNull null
            createMetadata(folderPath = folderPath, relativePath = relativePath)
        }
        return LocalMusicScanResult(
            discovered = discovered,
            sourceSummaries = listOf(
                LocalMusicSourceSummary(
                    sourceKind = LocalMusicSourceKind.IosImportedFile,
                    displayName = folderName,
                    songCount = discovered.size,
                    problemCount = 0,
                    lastScannedAt = completedAt,
                ),
            ),
            completedAt = completedAt,
        )
    }

    // 将单个 iOS 文件路径映射为平台无关音频元数据。
    private fun createMetadata(folderPath: String, relativePath: String): MusicFileMetadata? {
        val fileName: String = relativePath.substringAfterLast(delimiter = "/")
        val audioType: LocalAudioType = LocalAudioFileRules.matchAudioType(fileName = fileName) ?: return null
        val fullPath: String = "$folderPath/$relativePath"
        if (!NSFileManager.defaultManager.isReadableFileAtPath(path = fullPath)) {
            return null
        }
        val sourceId: String = fullPath
        val localUri: String = NSURL.fileURLWithPath(path = fullPath).absoluteString ?: fullPath
        return MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.IosImportedFile,
            localUri = localUri,
            fileName = fileName,
            title = LocalAudioFileRules.titleFromFileName(fileName = fileName),
            artist = null,
            album = relativePath.substringBeforeLast(
                delimiter = "/",
                missingDelimiterValue = "iOS 文件夹",
            ).substringAfterLast(delimiter = "/"),
            durationMs = null,
            mimeType = audioType.mimeType,
            sizeBytes = null,
            modifiedAt = null,
            coverArt = LocalAudioFileRules.coverForSourceId(sourceId = sourceId),
        )
    }

    // 构造选择器返回不可用 URL 时的统一错误。
    private fun unavailableFolderException(folderUrl: NSURL): LocalMusicScanException {
        return LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.FolderUnavailable,
                message = "选择的 iOS 音乐文件夹不可用",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
                sourceId = folderUrl.absoluteString,
            ),
        )
    }

    // 构造文件夹路径不可遍历时的统一错误。
    private fun unavailableFolderPathException(folderPath: String): LocalMusicScanException {
        return LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.FolderUnavailable,
                message = "无法读取 iOS 音乐文件夹",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
                sourceId = folderPath,
            ),
        )
    }

    // 获取毫秒时间戳，保持 iOS scanner 不依赖 JVM API。
    @OptIn(ExperimentalForeignApi::class)
    private fun currentTimeMillis(): Long {
        return time(null) * 1_000L
    }
}
