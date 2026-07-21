package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import platform.Foundation.NSURL

/**
 * iOS 沙盒导入器，保证外部 Files URL 不直接进入播放队列。
 */
internal class IosSandboxAudioImporter(
    // 文件系统边界，真实运行使用 [NSFileManager]，测试使用 fake。
    private val fileSystem: IosImportFileSystem = NSFileManagerIosImportFileSystemImpl(),
) {
    // 沙盒文件提交器，隔离临时文件和最终文件的状态转换。
    private val committer: IosSandboxAudioCommitter = IosSandboxAudioCommitter(fileSystem = fileSystem)

    /** 在 security scope 存活窗口内导入目录中的音频。 */
    fun importFolder(
        folderUrl: NSURL,
        completedAt: Long,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        val hasAccess: Boolean = folderUrl.startAccessingSecurityScopedResource()
        try {
            return importFolderWithAccess(
                folderUrl = folderUrl,
                completedAt = completedAt,
                preferences = preferences,
            )
        } finally {
            if (hasAccess) {
                folderUrl.stopAccessingSecurityScopedResource()
            }
        }
    }

    /** 复制授权目录中的音频，并把失败保留为可展示问题。 */
    private fun importFolderWithAccess(
        folderUrl: NSURL,
        completedAt: Long,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        val folderPath: String =
            fileSystem.path(url = folderUrl)
                ?: throw createUnavailableIosFolderException(fileSystem = fileSystem, folderUrl = folderUrl)
        val subpaths: List<String> =
            fileSystem.listSubpaths(path = folderPath)
                ?: throw createUnavailableIosFolderPathException(folderPath = folderPath)
        val importDirectoryPath: String = resolveImportDirectoryPath()
        val discovered: MutableList<MusicFileMetadata> = mutableListOf()
        val failed: MutableList<LocalMusicProblem> = mutableListOf()
        subpaths.forEach { relativePath: String ->
            importRelativePath(
                folderPath = folderPath,
                relativePath = relativePath,
                importDirectoryPath = importDirectoryPath,
                preferences = preferences,
                discovered = discovered,
                failed = failed,
            )
        }
        return buildIosImportScanResult(
            fileSystem = fileSystem,
            folderUrl = folderUrl,
            discovered = discovered,
            failed = failed,
            completedAt = completedAt,
        )
    }

    /** 解析并创建沙盒导入目录。 */
    private fun resolveImportDirectoryPath(): String {
        val importDirectoryPath: String =
            fileSystem.sandboxImportDirectoryPath()
                ?: throw createIosSandboxUnavailableException()
        if (!fileSystem.ensureDirectory(path = importDirectoryPath)) {
            throw createIosSandboxUnavailableException()
        }
        return importDirectoryPath
    }

    /** 导入单个相对路径，非音频文件直接忽略。 */
    private fun importRelativePath(
        folderPath: String,
        relativePath: String,
        importDirectoryPath: String,
        preferences: LocalMusicDiscoveryPreferences,
        discovered: MutableList<MusicFileMetadata>,
        failed: MutableList<LocalMusicProblem>,
    ) {
        val fileName: String = relativePath.substringAfterLast(delimiter = "/")
        val audioType: LocalAudioType = LocalAudioFileRules.matchAudioType(fileName = fileName) ?: return
        if (!LocalAudioFileRules.shouldIncludeByDuration(durationMs = null, preferences = preferences)) {
            return
        }
        val sourcePath: String = "$folderPath/$relativePath"
        val commitResult: IosSandboxAudioCommitResult =
            committer.commit(
                candidate =
                    IosSandboxAudioImportCandidate(
                        sourcePath = sourcePath,
                        fileName = fileName,
                        importDirectoryPath = importDirectoryPath,
                    ),
            )
        if (commitResult is IosSandboxAudioCommitResult.Failure) {
            failed += commitResult.problem
            return
        }
        val committedPath: String = (commitResult as IosSandboxAudioCommitResult.Success).committedPath
        val albumName: String =
            relativePath
                .substringBeforeLast(
                    delimiter = "/",
                    missingDelimiterValue = "iOS 导入",
                ).substringAfterLast(delimiter = "/")
        discovered +=
            buildIosImportedAudioMetadata(
                fileSystem = fileSystem,
                committedPath = committedPath,
                fileName = fileName,
                albumName = albumName,
                audioType = audioType,
            )
    }
}
