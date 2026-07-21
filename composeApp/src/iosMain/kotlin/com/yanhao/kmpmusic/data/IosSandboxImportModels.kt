package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverage
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import platform.Foundation.NSURL

/**
 * 构造 iOS 导入成功后的平台无关元数据。
 */
internal fun buildIosImportedAudioMetadata(
    fileSystem: IosImportFileSystem,
    committedPath: String,
    fileName: String,
    albumName: String,
    audioType: LocalAudioType,
): MusicFileMetadata =
    MusicFileMetadata(
        sourceId = committedPath,
        sourceKind = LocalMusicSourceKind.IosImportedFile,
        localUri = fileSystem.fileUrlString(path = committedPath),
        fileName = fileName,
        title = LocalAudioFileRules.titleFromFileName(fileName = fileName),
        artist = null,
        album = albumName,
        durationMs = null,
        mimeType = audioType.mimeType,
        sizeBytes = null,
        modifiedAt = null,
        coverArt = CoverArt.HeroLocalMusic,
    )

/**
 * 构造导入完成的扫描结果。
 */
internal fun buildIosImportScanResult(
    fileSystem: IosImportFileSystem,
    folderUrl: NSURL,
    discovered: List<MusicFileMetadata>,
    failed: List<LocalMusicProblem>,
    completedAt: Long,
): LocalMusicScanResult =
    LocalMusicScanResult(
        discovered = discovered,
        failed = failed,
        sourceSummaries =
            listOf(
                LocalMusicSourceSummary(
                    sourceKind = LocalMusicSourceKind.IosImportedFile,
                    displayName = fileSystem.lastPathComponent(url = folderUrl) ?: "iOS 导入",
                    songCount = discovered.size,
                    problemCount = failed.size,
                    lastScannedAt = completedAt,
                ),
            ),
        completedCoverage = listOf(LocalMusicScanCoverage.PositiveOnly),
        completedAt = completedAt,
    )

/**
 * 构造单个导入失败问题。
 */
internal fun createIosImportProblem(
    sourcePath: String,
    fileName: String,
    type: LocalMusicScanErrorType,
    message: String,
): LocalMusicProblem =
    LocalMusicProblem(
        sourceKind = LocalMusicSourceKind.IosImportedFile,
        sourceId = sourcePath,
        fileName = fileName,
        error =
            LocalMusicScanError(
                type = type,
                message = message,
                sourceKind = LocalMusicSourceKind.IosImportedFile,
                sourceId = sourcePath,
            ),
    )

/**
 * 构造选择器返回不可用 URL 时的统一错误。
 */
internal fun createUnavailableIosFolderException(
    fileSystem: IosImportFileSystem,
    folderUrl: NSURL,
): LocalMusicScanException =
    LocalMusicScanException(
        error =
            LocalMusicScanError(
                type = LocalMusicScanErrorType.FolderUnavailable,
                message = "选择的 iOS 音乐文件夹不可用",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
                sourceId = fileSystem.absoluteString(url = folderUrl),
            ),
    )

/**
 * 构造文件夹路径不可遍历时的统一错误。
 */
internal fun createUnavailableIosFolderPathException(folderPath: String): LocalMusicScanException =
    LocalMusicScanException(
        error =
            LocalMusicScanError(
                type = LocalMusicScanErrorType.FolderUnavailable,
                message = "无法读取 iOS 音乐文件夹",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
                sourceId = folderPath,
            ),
    )

/**
 * 构造沙盒导入目录不可用时的统一错误。
 */
internal fun createIosSandboxUnavailableException(): LocalMusicScanException =
    LocalMusicScanException(
        error =
            LocalMusicScanError(
                type = LocalMusicScanErrorType.FileUnreadable,
                message = "无法准备 App 沙盒导入目录，请重试导入",
                sourceKind = LocalMusicSourceKind.IosImportedFile,
            ),
    )
